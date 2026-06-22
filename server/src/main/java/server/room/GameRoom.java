package server.room;

import common.messages.RoomMessages.RoomSettings;
import common.messages.RoomMessages.PlayerInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class GameRoom {
    private final String roomCode;
    private final String roomName;
    private final int hostId;
    private final RoomSettings settings;
    private final List<PlayerInfo> players = new ArrayList<>();
    private final Map<Integer, String> playlists = new HashMap<>();
    private final Map<Integer, List<String>> resolvedSongs = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private boolean gameStarted = false;
    private int currentRound = 0;

    public GameRoom(String roomCode, String roomName, int hostId, RoomSettings settings) {
        this.roomCode = roomCode;
        this.roomName = roomName;
        this.hostId = hostId;
        this.settings = settings;
    }

    public boolean addPlayer(int id, String nickname, boolean hasPlaylist) {
        // CONCURRENCY: Запобігаємо race condition при одночасному підключенні, щоб не
        // перевищити ліміт maxPlayers
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
            players.add(new PlayerInfo(id, nickname, hasPlaylist, 0));
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean removePlayer(int id) {
        // CONCURRENCY: Захист доступу до стану кімнати
        lock.lock();
        try {
            playlists.remove(id);
            resolvedSongs.remove(id);
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
        // CONCURRENCY: Захист доступу до стану кімнати
        lock.lock();
        try {
            return new ArrayList<>(players);
        } finally {
            lock.unlock();
        }
    }

    public boolean isGameStarted() {
        // CONCURRENCY: Захист доступу до стану кімнати
        lock.lock();
        try {
            return gameStarted;
        } finally {
            lock.unlock();
        }
    }

    public void setGameStarted(boolean gameStarted) {
        // CONCURRENCY: Захист доступу до стану кімнати
        lock.lock();
        try {
            this.gameStarted = gameStarted;
        } finally {
            lock.unlock();
        }
    }

    public int getCurrentRound() {
        // CONCURRENCY: Захист доступу до стану кімнати
        lock.lock();
        try {
            return currentRound;
        } finally {
            lock.unlock();
        }
    }

    public void setCurrentRound(int round) {
        // CONCURRENCY: Захист доступу до стану кімнати
        lock.lock();
        try {
            this.currentRound = round;
        } finally {
            lock.unlock();
        }
    }

    public void setPlaylist(int userId, String playlistUrl) {
        lock.lock();
        try {
            playlists.put(userId, playlistUrl);
        } finally {
            lock.unlock();
        }
    }

    public Map<Integer, String> getPlaylists() {
        lock.lock();
        try {
            return new HashMap<>(playlists);
        } finally {
            lock.unlock();
        }
    }

    public void setResolvedSongs(int userId, List<String> videoIds) {
        lock.lock();
        try {
            resolvedSongs.put(userId, videoIds);
        } finally {
            lock.unlock();
        }
    }

    public List<String> getResolvedSongs(int userId) {
        lock.lock();
        try {
            return resolvedSongs.getOrDefault(userId, List.of());
        } finally {
            lock.unlock();
        }
    }

    public void markPlaylistReady(int userId) {
        lock.lock();
        try {
            for (int i = 0; i < players.size(); i++) {
                PlayerInfo p = players.get(i);
                if (p.id() == userId) {
                    int songCount = resolvedSongs.getOrDefault(userId, List.of()).size();
                    players.set(i, new PlayerInfo(p.id(), p.nickname(), true, songCount));
                    return;
                }
            }
        } finally {
            lock.unlock();
        }
    }
}