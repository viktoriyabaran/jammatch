package server.net;

import com.google.gson.Gson;
import common.protocol.*;
import common.contracts.*;
import common.messages.SessionMessages.ClientLogin;
import common.messages.RoomMessages.RoomConfig;
import common.messages.RoomMessages.RoomSettings;
import common.messages.RoomMessages.CreateRoomResult;
import common.messages.RoomMessages.JoinRoom;
import server.db.dao.RoomDao;
import server.db.dao.UserDao;
import server.room.GameRoom;
import server.room.RoomManager;

import java.nio.charset.StandardCharsets;
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
    private final Gson gson = new Gson();

    public Processor(BlockingQueue<Packet> input, BlockingQueue<Packet> output, UserDao userDao, RoomDao roomDao,
            RoomManager roomManager) {
        this.input = input;
        this.output = output;
        this.userDao = userDao;
        this.roomDao = roomDao;
        this.roomManager = roomManager;
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
                    int userId = userDao.insertUser(loginData.nickname());
                    return buildSuccess(request, String.valueOf(userId));

                case CREATE_ROOM:
                    RoomConfig config = gson.fromJson(payloadStr, RoomConfig.class);
                    RoomSettings settings = new RoomSettings(config.maxPlayers(), config.rounds(),
                            config.roundDurationSeconds());
                    int hostId = msg.getbUserId();

                    GameRoom newRoom = roomManager.createRoom(config.roomName(), hostId, settings);
                    roomDao.createRoom(newRoom.getRoomCode(), config.roomName(), hostId, config.maxPlayers(),
                            config.rounds(), config.roundDurationSeconds());

                    CreateRoomResult result = new CreateRoomResult(newRoom.getRoomCode());
                    return buildSuccess(request, gson.toJson(result));

                case JOIN_ROOM:
                    JoinRoom joinData = gson.fromJson(payloadStr, JoinRoom.class);
                    int joinUserId = msg.getbUserId();
                    java.util.Optional<String> nicknameOpt = userDao.getUserNickname(joinUserId);

                    if (nicknameOpt.isEmpty()) {
                        return buildError(request, "User not found");

                    java.util.Optional<GameRoom> roomOpt = roomManager.getRoom(joinData.roomCode());
                    if (roomOpt.isEmpty()) {
                        return buildError(request, "Room not found");
                    }

                    if (!roomOpt.get().addPlayer(joinUserId, nicknameOpt.get(), false)) {
                        return buildError(request, "Cannot join room");
                    }

                    return buildSuccess(request, "{\"status\":\"OK\"}");

                case LEAVE_ROOM:
                    int leaveUserId = msg.getbUserId();
                    roomManager.removePlayerFromAllRooms(leaveUserId);
                    return buildSuccess(request, "{\"status\":\"OK\"}");

                default:
                    return buildSuccess(request, "{\"status\":\"OK\"}");
            }
        } catch (Exception e) {
            System.err.println("[Processor] Error handling " + command + ": " + e.getMessage());
            return buildError(request, e.getMessage());
        }
    }

    private Packet buildSuccess(Packet request, String body) {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        Message responseMsg = new Message(
                request.getbMsg().getcType(),
                request.getbMsg().getbUserId(),
                payload);
        return new Packet(request.getbSrc(), request.getbPktId(), responseMsg);
    }

    private Packet buildError(Packet request, String errorMessage) {
        String body = "{\"status\":\"ERROR\",\"message\":\"" + escape(errorMessage) + "\"}";
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        Message responseMsg = new Message(
                request.getbMsg().getcType(),
                request.getbMsg().getbUserId(),
                payload);
        return new Packet(request.getbSrc(), request.getbPktId(), responseMsg);
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