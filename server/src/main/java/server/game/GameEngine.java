package server.game;

import com.google.gson.Gson;
import common.contracts.CommandType;
import common.messages.GameMessages.*;
import common.messages.RoomMessages.PlayerInfo;
import common.protocol.Message;
import common.protocol.Packet;
import server.db.dao.*;
import server.net.SessionManager;
import server.room.GameRoom;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class GameEngine {
    private final GameRoom room;
    private final SessionManager sessionManager;
    private final RoomDao roomDao;
    private final GameDao gameDao;
    private final GameParticipantDao participantDao;
    private final GameSongDao songDao;
    private final RoundDao roundDao;

    private int gameId;
    private int currentRound = 0;

    private final Map<Integer, Integer> playerScores = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> currentRoundVotes = new ConcurrentHashMap<>();

    // CONCURRENCY: Guarantees that only the first correct vote will receive a speed
    // bonus
    private final AtomicReference<Integer> firstCorrectVoter = new AtomicReference<>(null);

    // CONCURRENCY: Prevents a round from ending twice (if the timer and the last
    // vote go off at the same time)
    private final AtomicBoolean roundActive = new AtomicBoolean(false);

    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> roundTimerTask;

    private int currentCorrectUserId = -1;
    private String currentVideoId = null;
    private final Gson gson = new Gson();

    public GameEngine(GameRoom room, SessionManager sessionManager, RoomDao roomDao,
            GameDao gameDao, GameParticipantDao participantDao, GameSongDao songDao, RoundDao roundDao) {
        this.room = room;
        this.sessionManager = sessionManager;
        this.roomDao = roomDao;
        this.gameDao = gameDao;
        this.participantDao = participantDao;
        this.songDao = songDao;
        this.roundDao = roundDao;
    }

    public void startGame() {
        try {
            int roomId = roomDao.getRoomIdByCode(room.getRoomCode()).orElseThrow();
            this.gameId = gameDao.createGame(roomId);

            Map<Integer, String> playlists = room.getPlaylists();
            for (PlayerInfo p : room.getPlayers()) {
                participantDao.addParticipant(gameId, p.id(), playlists.get(p.id()));
                playerScores.put(p.id(), 0);

                List<String> userSongs = room.getResolvedSongs(p.id());
                if (userSongs != null && !userSongs.isEmpty()) {
                    songDao.addSongs(gameId, p.id(), userSongs);
                }
            }

            room.setGameStarted(true);
            System.out.println("[GameEngine] Game started in room " + room.getRoomCode());

            timer.schedule(this::startNextRound, 3, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.err.println("[GameEngine] Failed to start game: " + e.getMessage());
        }
    }

    private void startNextRound() {
        if (currentRound >= room.getSettings().rounds()) {
            endGame();
            return;
        }

        currentRound++;
        room.setCurrentRound(currentRound);
        currentRoundVotes.clear();
        firstCorrectVoter.set(null);

        try {
            Optional<RoundSong> songOpt = songDao.pickRandomUnplayed(gameId);
            if (songOpt.isEmpty()) {
                endGame();
                return;
            }

            RoundSong song = songOpt.get();
            currentCorrectUserId = song.correctUserId();
            currentVideoId = song.videoId();

            List<Integer> options = new ArrayList<>();
            options.add(currentCorrectUserId);

            List<PlayerInfo> allPlayers = room.getPlayers();
            Collections.shuffle(allPlayers);
            for (PlayerInfo p : allPlayers) {
                if (options.size() >= 4)
                    break;
                if (p.id() != currentCorrectUserId) {
                    options.add(p.id());
                }
            }
            Collections.shuffle(options);

            int duration = room.getSettings().roundDurationSeconds();

            RoundStart roundStartMsg = new RoundStart(
                    currentRound, room.getSettings().rounds(), currentVideoId, duration, options);

            broadcast(CommandType.ROUND_START, gson.toJson(roundStartMsg));

            roundActive.set(true);

            roundTimerTask = timer.schedule(this::endRound, duration, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.err.println("[GameEngine] Round start error: " + e.getMessage());
        }
    }

    public void submitVote(int userId, int votedUserId) {
        if (!roundActive.get())
            return;

        currentRoundVotes.put(userId, votedUserId);

        if (votedUserId == currentCorrectUserId) {
            firstCorrectVoter.compareAndSet(null, userId);
        }

        if (currentRoundVotes.size() >= room.getPlayers().size()) {
            if (roundTimerTask != null)
                roundTimerTask.cancel(false);
            endRound();
        }
    }

    private void endRound() {
        // CONCURRENCY: Whoever calls first (timer or last vote) will go next
        if (!roundActive.compareAndSet(true, false)) {
            return;
        }

        List<RoundResult> results = new ArrayList<>();
        Integer firstCorrectId = firstCorrectVoter.get();

        for (PlayerInfo p : room.getPlayers()) {
            int pid = p.id();
            int votedFor = currentRoundVotes.getOrDefault(pid, -1);
            boolean correct = (votedFor == currentCorrectUserId);

            int points = 0;
            if (correct) {
                points = (firstCorrectId != null && firstCorrectId == pid) ? 20 : 10;
            }

            int newScore = playerScores.getOrDefault(pid, 0) + points;
            playerScores.put(pid, newScore);

            results.add(new RoundResult(pid, votedFor, correct, points, newScore));

            if (points > 0) {
                try {
                    participantDao.addScore(gameId, pid, points);
                } catch (Exception e) {
                    System.err.println("[GameEngine] Failed to add score to DB: " + e.getMessage());
                }
            }
        }

        try {
            roundDao.insertRound(gameId, currentVideoId, currentCorrectUserId);
        } catch (Exception e) {
            System.err.println("[GameEngine] Failed to save round to DB: " + e.getMessage());
        }

        RoundEnd roundEndMsg = new RoundEnd(currentRound, currentCorrectUserId, results);
        broadcast(CommandType.ROUND_END, gson.toJson(roundEndMsg));

        timer.schedule(this::startNextRound, 5, TimeUnit.SECONDS);
    }

    private void endGame() {
        room.setGameStarted(false);

        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        List<PlayerInfo> players = room.getPlayers();

        players.sort((p1, p2) -> Integer.compare(playerScores.getOrDefault(p2.id(), 0),
                playerScores.getOrDefault(p1.id(), 0)));

        int rank = 1;
        for (PlayerInfo p : players) {
            leaderboard.add(new LeaderboardEntry(p.id(), p.nickname(), playerScores.getOrDefault(p.id(), 0), rank++));
        }

        GameOver gameOverMsg = new GameOver(leaderboard);
        broadcast(CommandType.GAME_OVER, gson.toJson(gameOverMsg));

        try {
            gameDao.updateGameStatus(gameId, "FINISHED");
        } catch (Exception e) {
            System.err.println("[GameEngine] Failed to close game in DB: " + e.getMessage());
        }

        timer.shutdown();
    }

    private void broadcast(CommandType type, String payloadStr) {
        byte[] payload = payloadStr.getBytes(StandardCharsets.UTF_8);
        for (PlayerInfo p : room.getPlayers()) {
            Message msg = new Message(type.code(), p.id(), payload);
            Packet pkt = new Packet((byte) 0, 0, msg);
            sessionManager.sendToUser(p.id(), pkt);
        }
    }
}