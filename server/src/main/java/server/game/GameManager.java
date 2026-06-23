package server.game;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {
    private final ConcurrentHashMap<String, GameEngine> activeGames = new ConcurrentHashMap<>();

    public void addGame(String roomCode, GameEngine engine) {
        activeGames.put(roomCode, engine);
        engine.startGame();
    }

    public Optional<GameEngine> getGame(String roomCode) {
        return Optional.ofNullable(activeGames.get(roomCode));
    }

    public void removeGame(String roomCode) {
        activeGames.remove(roomCode);
    }
}