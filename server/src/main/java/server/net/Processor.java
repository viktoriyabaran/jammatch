package server.net;

import com.google.gson.Gson;
import common.protocol.*;
import common.contracts.*;
import common.messages.SessionMessages.ClientLogin;
import common.messages.RoomMessages.RoomConfig;
import common.messages.RoomMessages.RoomSettings;
import common.messages.RoomMessages.CreateRoomResult;
import common.messages.RoomMessages.JoinRoom;
import common.messages.RoomMessages.LobbyUpdate;
import common.messages.RoomMessages.PlayerInfo;
import server.db.dao.RoomDao;
import server.db.dao.UserDao;
import server.room.GameRoom;
import server.room.RoomManager;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Processor implements Runnable {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private final int id = NEXT_ID.getAndIncrement();
    private final BlockingQueue<Packet> input;
    private final BlockingQueue<Packet> output;

    private final UserDao userDao;
    private final RoomDao roomDao;
    private final RoomManager roomManager;
    private final SessionManager sessionManager;
    private final Gson gson = new Gson();

    public Processor(BlockingQueue<Packet> input, BlockingQueue<Packet> output, UserDao userDao, RoomDao roomDao,
            RoomManager roomManager, SessionManager sessionManager) {
        this.input = input;
        this.output = output;
        this.userDao = userDao;
        this.roomDao = roomDao;
        this.roomManager = roomManager;
        this.sessionManager = sessionManager;
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
                    int userId = userDao.findOrCreateByToken(loginData.clientToken(), loginData.nickname());

                    sessionManager.register(userId, output);
                    return buildSuccess(request, String.valueOf(userId));

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

                case LEAVE_ROOM:
                    int leaveUserId = msg.getbUserId();
                    Optional<GameRoom> userRoom = roomManager.getRoomByUserId(leaveUserId);

                    if (userRoom.isPresent()) {
                        GameRoom room = userRoom.get();
                        if (room.getHostId() == leaveUserId) {
                            String code = room.getRoomCode();
                            java.util.List<PlayerInfo> playersToNotify = room.getPlayers();

                            roomManager.removeRoom(code);
                            roomDao.closeRoom(code);

                            common.messages.RoomMessages.RoomClosed closedMessage = new common.messages.RoomMessages.RoomClosed(
                                    "Host left the room");
                            byte[] closedPayload = gson.toJson(closedMessage).getBytes(StandardCharsets.UTF_8);

                            for (PlayerInfo p : playersToNotify) {
                                if (p.id() != leaveUserId) {
                                    Message eventMsg = new Message(CommandType.ROOM_CLOSED.code(), p.id(),
                                            closedPayload);
                                    sessionManager.sendToUser(p.id(), new Packet((byte) 0, 0, eventMsg));
                                }
                            }
                        } else {
                            room.removePlayer(leaveUserId);
                            broadcastLobbyUpdate(room);
                        }
                    }
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
                        broadcastLobbyUpdate(r); // Оновлюємо лоббі для інших
                    }
                    return buildSuccess(request, "{\"status\":\"OK\"}");

                default:
                    return buildSuccess(request, "{\"status\":\"OK\"}");
            }
        } catch (Exception e) {
            System.err.println("[Processor] Error handling " + command + ": " + e.getMessage());
            return buildError(request, e.getMessage());
        }
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