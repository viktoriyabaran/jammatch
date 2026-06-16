package server.room;

import common.messages.RoomMessages.RoomSettings;
import common.messages.RoomMessages.PlayerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class GameRoom {
    private final String roomCode;
    private final String roomName;
    private final int hostId;
    private final RoomSettings settings;
    private final List<PlayerInfo> players = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private boolean gameStarted = false;

    public GameRoom(String roomCode, String roomName, int hostId, RoomSettings settings) {
        this.roomCode = roomCode;
        this.roomName = roomName;
        this.hostId = hostId;
        this.settings = settings;
    }

    public boolean addPlayer(int id, String nickname, boolean hasPlaylist) {
        lock.lock();
        try {
            if (gameStarted || players.size() >= settings.maxPlayers()) {
                return false;
            }
            for (PlayerInfo p : players) {
                if (p.id() == id || p.nickname().equalsIgnoreCase(nickname)) {
                    return false;
                }
            }
            players.add(new PlayerInfo(id, nickname, hasPlaylist));
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean removePlayer(int id) {
        lock.lock();
        try {
            return players.removeIf(p -> p.id() == id);
        } finally {
            lock.unlock();
        }
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getRoomName() {
        return roomName;
    }

    public int getHostId() {
        return hostId;
    }

    public RoomSettings getSettings() {
        return settings;
    }

    public List<PlayerInfo> getPlayers() {
        lock.lock();
        try {
            return new ArrayList<>(players);
        } finally {
            lock.unlock();
        }
    }

    public boolean isGameStarted() {
        lock.lock();
        try {
            return gameStarted;
        } finally {
            lock.unlock();
        }
    }

    public void setGameStarted(boolean gameStarted) {
        lock.lock();
        try {
            this.gameStarted = gameStarted;
        } finally {
            lock.unlock();
        }
    }
}