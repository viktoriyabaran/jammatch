package server.net;

import com.google.gson.Gson;
import common.protocol.*;
import common.contracts.*;
import common.messages.SessionMessages.ClientLogin;
import common.messages.SessionMessages.LookupNickname;
import common.messages.SessionMessages.SavedPlaylist;
import common.messages.SessionMessages.SavedPlaylists;
import common.messages.SessionMessages.SubmitPlaylist;
import common.messages.SessionMessages.ValidatePlaylist;
import common.messages.RoomMessages.RoomConfig;
import common.messages.RoomMessages.RoomSettings;
import common.messages.RoomMessages.CreateRoomResult;
import common.messages.RoomMessages.JoinRoom;
import common.messages.RoomMessages.LobbyUpdate;
import common.messages.RoomMessages.PlayerInfo;
import server.db.dao.RoomDao;
import server.db.dao.RoundDao;
import server.db.dao.SavedPlaylistDao;
import server.db.dao.UserDao;
import server.db.dao.GameDao;
import server.db.dao.GameParticipantDao;
import server.db.dao.GameSongDao;
import server.external.PlaylistResolver;
import server.external.model.PlaylistPreview;
import server.room.GameRoom;
import server.room.RoomManager;
import server.game.GameEngine;
import server.game.GameManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Processor implements Runnable {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private static final ExecutorService RESOLUTION_POOL = Executors.newFixedThreadPool(4);
    private final int id = NEXT_ID.getAndIncrement();
    private volatile int userId = 0;
    private final BlockingQueue<Packet> input;
    private final BlockingQueue<Packet> output;

    private final UserDao userDao;
    private final RoomDao roomDao;
    private final RoomManager roomManager;
    private final SessionManager sessionManager;
    private final SavedPlaylistDao savedPlaylistDao;
    private final PlaylistResolver playlistResolver;

    private final GameDao gameDao;
    private final GameParticipantDao participantDao;
    private final GameSongDao songDao;
    private final GameManager gameManager;
    private final RoundDao roundDao;

    private final Gson gson = new Gson();

    public Processor(BlockingQueue<Packet> input, BlockingQueue<Packet> output, UserDao userDao, RoomDao roomDao,
            RoomManager roomManager, SessionManager sessionManager, SavedPlaylistDao savedPlaylistDao,
            PlaylistResolver playlistResolver, GameDao gameDao, GameParticipantDao participantDao,
            GameSongDao songDao, GameManager gameManager, RoundDao roundDao) {
        this.input = input;
        this.output = output;
        this.userDao = userDao;
        this.roomDao = roomDao;
        this.roomManager = roomManager;
        this.sessionManager = sessionManager;
        this.savedPlaylistDao = savedPlaylistDao;
        this.playlistResolver = playlistResolver;

        this.gameDao = gameDao;
        this.participantDao = participantDao;
        this.songDao = songDao;
        this.gameManager = gameManager;
        this.roundDao = roundDao;
    }

    private Packet process(Packet request) {
        Message msg = request.getbMsg();
        CommandType command;

        try {
            command = CommandType.fromCode(msg.getcType());
        } catch (IllegalArgumentException e) {
            return buildError(request, "Unknown command code");
        }

        try {
            String payloadStr = new String(msg.getMessage(), StandardCharsets.UTF_8);
            System.out.println("[Processor " + id + "] Received " + command + ": " + payloadStr);

            switch (command) {
                case CLIENT_LOGIN:
                    ClientLogin loginData = gson.fromJson(payloadStr, ClientLogin.class);
                    int loggedInUserId = userDao.findOrCreateByToken(loginData.clientToken(), loginData.nickname());

                    this.userId = loggedInUserId;
                    sessionManager.register(loggedInUserId, output);
                    return buildSuccess(request, String.valueOf(loggedInUserId));

                case LOOKUP_NICKNAME:
                    LookupNickname lookupData = gson.fromJson(payloadStr, LookupNickname.class);
                    String knownNickname = userDao.findNicknameByToken(lookupData.clientToken()).orElse("");
                    return buildSuccess(request, knownNickname);

                case CREATE_ROOM:
                    RoomConfig config = gson.fromJson(payloadStr, RoomConfig.class);
                    RoomSettings settings = new RoomSettings(config.maxPlayers(), config.rounds(),
                            config.roundDurationSeconds());
                    int hostId = msg.getbUserId();

                    GameRoom newRoom = roomManager.createRoom(config.roomName(), hostId, settings);
                    roomDao.createRoom(newRoom.getRoomCode(), config.roomName(), hostId, config.maxPlayers(),
                            config.rounds(), config.roundDurationSeconds());

                    Optional<String> hostName = userDao.getUserNickname(hostId);
                    newRoom.addPlayer(hostId, hostName.orElse("Host"), false);

                    CreateRoomResult result = new CreateRoomResult(newRoom.getRoomCode());
                    Packet response = buildSuccess(request, gson.toJson(result));

                    broadcastLobbyUpdate(newRoom);
                    return response;

                case JOIN_ROOM:
                    JoinRoom joinData = gson.fromJson(payloadStr, JoinRoom.class);
                    int joinUserId = msg.getbUserId();
                    Optional<String> nicknameOpt = userDao.getUserNickname(joinUserId);

                    if (nicknameOpt.isEmpty()) {
                        return buildError(request, "User not found");
                    }
                    Optional<GameRoom> roomOpt = roomManager.getRoom(joinData.roomCode());
                    if (roomOpt.isEmpty()) {
                        return buildError(request, "Room not found");
                    }
                    GameRoom roomToJoin = roomOpt.get();
                    if (!roomToJoin.addPlayer(joinUserId, nicknameOpt.get(), false)) {
                        return buildError(request, "Cannot join room (full or already started)");
                    }

                    Packet joinResponse = buildSuccess(request, "{\"status\":\"OK\"}");
                    broadcastLobbyUpdate(roomToJoin);
                    return joinResponse;

                case START_GAME:
                    int startRequesterId = msg.getbUserId();
                    Optional<GameRoom> startRoomOpt = roomManager.getRoomByUserId(startRequesterId);

                    if (startRoomOpt.isEmpty() || startRoomOpt.get().getHostId() != startRequesterId) {
                        return buildError(request, "Only host can start the game");
                    }

                    GameRoom rToStart = startRoomOpt.get();
                    if (rToStart.isGameStarted()) {
                        return buildError(request, "Game already started");
                    }

                    GameEngine engine = new GameEngine(rToStart, gameManager, sessionManager, roomDao, gameDao, participantDao, songDao, roundDao);
                    gameManager.addGame(rToStart.getRoomCode(), engine);
                    return buildSuccess(request, "{\"status\":\"OK\"}");

                case SUBMIT_VOTE:
                    common.messages.GameMessages.SubmitVote voteData = gson.fromJson(payloadStr,
                            common.messages.GameMessages.SubmitVote.class);
                    int voterId = msg.getbUserId();

                    roomManager.getRoomByUserId(voterId).ifPresent(r -> {
                        gameManager.getGame(r.getRoomCode()).ifPresent(activeGame -> {
                            activeGame.submitVote(voterId, voteData.votedUserId());
                        });
                    });
                    return buildSuccess(request, "{\"status\":\"OK\"}");

                case LEAVE_ROOM:
                    int leaveUserId = msg.getbUserId();
                    roomManager.getRoomByUserId(leaveUserId)
                            .ifPresent(room -> leaveRoom(room, leaveUserId, "Host left the room"));
                    return buildSuccess(request, "{\"status\":\"OK\"}");

                case KICK_PLAYER:
                    common.messages.RoomMessages.KickPlayer kickData = gson.fromJson(payloadStr,
                            common.messages.RoomMessages.KickPlayer.class);
                    int requesterId = msg.getbUserId();
                    java.util.Optional<GameRoom> hostRoom = roomManager.getRoomByUserId(requesterId);

                    if (hostRoom.isEmpty() || hostRoom.get().getHostId() != requesterId) {
                        return buildError(request, "Only host can kick players");
                    }

                    if (kickData.targetUserId() == requesterId) {
                        return buildError(request, "Host cannot kick themselves");
                    }

                    GameRoom r = hostRoom.get();
                    if (r.removePlayer(kickData.targetUserId())) {
                        roomManager.removePlayerFromAllRooms(kickData.targetUserId());
                        sendRoomClosed(kickData.targetUserId(), "You were removed by the host");
                        broadcastLobbyUpdate(r); // Оновлюємо лоббі для інших
                    }
                    return buildSuccess(request, "{\"status\":\"OK\"}");

                case SUBMIT_PLAYLIST:
                    SubmitPlaylist submitData = gson.fromJson(payloadStr, SubmitPlaylist.class);
                    int submitUserId = msg.getbUserId();
                    String submitUrl = submitData.playlistUrl();
                    savedPlaylistDao.save(submitUserId, submitUrl, null);
                    roomManager.getRoomByUserId(submitUserId).ifPresent(room -> {
                        room.setPlaylist(submitUserId, submitUrl);
                        RESOLUTION_POOL.submit(() -> resolvePlaylistAsync(room, submitUserId, submitUrl));
                    });
                    return buildSuccess(request, "{\"status\":\"OK\"}");

                case VALIDATE_PLAYLIST:
                    ValidatePlaylist validateData = gson.fromJson(payloadStr, ValidatePlaylist.class);
                    int validateUserId = msg.getbUserId();
                    String validateUrl = validateData.playlistUrl();

                    PlaylistPreview preview;
                    try {
                        preview = playlistResolver.preview(validateUrl);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return buildError(request, friendlyPlaylistError(e));
                    }
                    if (preview.trackCount() == 0) {
                        return buildError(request, "This playlist has no playable songs");
                    }

                    String previewName = preview.name() != null ? preview.name() : "Untitled playlist";
                    savedPlaylistDao.upsert(validateUserId, validateUrl, previewName, preview.trackCount(),
                            preview.coverUrl());
                    SavedPlaylist validated = new SavedPlaylist(validateUrl, previewName, preview.trackCount(),
                            preview.coverUrl());
                    return buildSuccess(request, gson.toJson(validated));

                case PLAYER_READY:
                    int readyUserId = msg.getbUserId();

                    roomManager.getRoomByUserId(readyUserId).ifPresent(readyRoom -> {
                        gameManager.getGame(readyRoom.getRoomCode()).ifPresent(activeGame -> {
                            activeGame.markPlayerReady(readyUserId);
                        });
                    });

                    return buildSuccess(request, "{\"status\":\"OK\"}");

                case LIST_SAVED_PLAYLISTS:
                    java.util.List<SavedPlaylist> playlists = savedPlaylistDao.listForUser(msg.getbUserId()).stream()
                            .map(p -> new SavedPlaylist(p.url(), p.name(), p.trackCount(), p.coverUrl()))
                            .toList();
                    return buildSuccess(request, gson.toJson(new SavedPlaylists(playlists)));

                default:
                    return buildSuccess(request, "{\"status\":\"OK\"}");
            }
        } catch (Exception e) {
            System.err.println("[Processor] Error handling " + command + ": " + e.getMessage());
            return buildError(request, e.getMessage());
        }
    }

    private void resolvePlaylistAsync(GameRoom room, int userId, String playlistUrl) {
        try {
            List<String> videoIds = playlistResolver.resolve(playlistUrl);
            room.setResolvedSongs(userId, videoIds);
            room.markPlaylistReady(userId);
            broadcastLobbyUpdate(room);
        } catch (Exception e) {
            System.err.println("[Processor] playlist resolve failed for user " + userId + ": " + e.getMessage());
        }
    }

    private String friendlyPlaylistError(IOException e) {
        String detail = e.getMessage() == null ? "" : e.getMessage();
        if (detail.contains("HTTP 404")) {
            return "Playlist is private or does not exist";
        }
        if (detail.contains("HTTP 403")) {
            return "Playlist is private or unavailable";
        }
        return "Couldn't reach YouTube, please try again";
    }

    public void onDisconnect() {
        if (userId <= 0) {
            return;
        }
        try {
            for (GameRoom room : roomManager.getRoomsByUserId(userId)) {
                leaveRoom(room, userId, "Host disconnected");
            }
            sessionManager.remove(userId);
        } catch (Exception e) {
            System.err.println("[Processor] Disconnect cleanup failed for user " + userId + ": " + e.getMessage());
        }
    }

    private void leaveRoom(GameRoom room, int userId, String hostLeftReason) {
        try {
            if (room.getHostId() == userId) {
                String code = room.getRoomCode();
                List<PlayerInfo> playersToNotify = room.getPlayers();

                gameManager.getGame(code).ifPresent(GameEngine::stop);
                gameManager.removeGame(code);
                roomManager.removeRoom(code);
                roomDao.closeRoom(code);

                for (PlayerInfo p : playersToNotify) {
                    if (p.id() != userId) {
                        sendRoomClosed(p.id(), hostLeftReason);
                    }
                }
            } else {
                room.removePlayer(userId);
                broadcastLobbyUpdate(room);
            }
        } catch (Exception e) {
            System.err.println("[Processor] leaveRoom failed for user " + userId + ": " + e.getMessage());
        }
    }

    private void sendRoomClosed(int userId, String reason) {
        common.messages.RoomMessages.RoomClosed closed = new common.messages.RoomMessages.RoomClosed(reason);
        byte[] payload = gson.toJson(closed).getBytes(StandardCharsets.UTF_8);
        Message eventMsg = new Message(CommandType.ROOM_CLOSED.code(), userId, payload);
        sessionManager.sendToUser(userId, new Packet((byte) 0, 0, eventMsg));
    }

    private void broadcastLobbyUpdate(GameRoom room) {
        LobbyUpdate update = new LobbyUpdate(
                room.getRoomCode(), room.getRoomName(), room.getSettings(),
                room.getHostId(), room.getPlayers());
        byte[] payload = gson.toJson(update).getBytes(StandardCharsets.UTF_8);

        for (PlayerInfo player : room.getPlayers()) {
            Message msg = new Message(CommandType.LOBBY_UPDATE.code(), player.id(), payload);
            Packet eventPacket = new Packet((byte) 0, 0, msg);
            sessionManager.sendToUser(player.id(), eventPacket);
        }
    }

    private Packet buildSuccess(Packet request, String body) {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        return new Packet(request.getbSrc(), request.getbPktId(),
                new Message(request.getbMsg().getcType(), request.getbMsg().getbUserId(), payload));
    }

    private Packet buildError(Packet request, String errorMessage) {
        String body = "{\"status\":\"ERROR\",\"message\":\"" + escape(errorMessage) + "\"}";
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        return new Packet(request.getbSrc(), request.getbPktId(),
                new Message(request.getbMsg().getcType(), request.getbMsg().getbUserId(), payload));
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Packet request = input.take();
                try {
                    Packet response = process(request);
                    output.put(response);
                } catch (RuntimeException e) {
                    System.err.println(e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}