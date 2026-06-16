package server.room;

import common.messages.RoomMessages.RoomSettings;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class RoomManager {
    private final ConcurrentHashMap<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;

    public GameRoom createRoom(String roomName, int hostId, RoomSettings settings) {
        String roomCode;
        do {
            roomCode = generateCode();
        } while (rooms.containsKey(roomCode));

        GameRoom room = new GameRoom(roomCode, roomName, hostId, settings);
        rooms.put(roomCode, room);
        return room;
    }

    public Optional<GameRoom> getRoom(String roomCode) {
        return Optional.ofNullable(rooms.get(roomCode));
    }

    public boolean removeRoom(String roomCode) {
        return rooms.remove(roomCode) != null;
    }

    public void removePlayerFromAllRooms(int userId) {
        rooms.values().forEach(room -> room.removePlayer(userId));
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = ThreadLocalRandom.current().nextInt(CHARS.length());
            sb.append(CHARS.charAt(index));
        }
        return sb.toString();
    }
}