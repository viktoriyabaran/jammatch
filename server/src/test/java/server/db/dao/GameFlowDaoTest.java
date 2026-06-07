package server.db.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.db.ConnectionPool;
import server.db.DbInitializer;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class GameFlowDaoTest {
    private ConnectionPool pool;
    private UserDao userDao;
    private RoomDao roomDao;
    private GameDao gameDao;
    private GameParticipantDao participantDao;
    private RoundDao roundDao;
    private BannedArtistDao bannedArtistDao;

    @BeforeEach
    void setUp() {
        File dbFile = new File("jammatch.db");
        if (dbFile.exists()) {
            dbFile.delete();
        }

        pool = new ConnectionPool();
        new DbInitializer(pool).initialize();

        userDao = new UserDao(pool);
        roomDao = new RoomDao(pool);
        gameDao = new GameDao(pool);
        participantDao = new GameParticipantDao(pool);
        roundDao = new RoundDao(pool);
        bannedArtistDao = new BannedArtistDao(pool);
    }

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.closePool();
        }
    }

    @Test
    void testFullGameFlow() throws Exception {
        int hostId = userDao.insertUser("host_player");
        int roomId = roomDao.createRoom("PLAY99", "Test Room", hostId, 10, 3, 30);

        int gameId = gameDao.createGame(roomId);
        assertTrue(gameId > 0, "Game ID should be generated");

        int player2Id = userDao.insertUser("friend_1");
        participantDao.addParticipant(gameId, hostId, "youtube.com/playlist1");
        participantDao.addParticipant(gameId, player2Id, "youtube.com/playlist2");

        assertDoesNotThrow(() -> {
            participantDao.addScore(gameId, player2Id, 100);
        }, "Updating score should not throw an exception");

        assertDoesNotThrow(() -> {
            roundDao.insertRound(gameId, "dQw4w9WgXcQ", hostId);
        }, "Inserting a round should work if foreign keys are correct");

        assertDoesNotThrow(() -> {
            gameDao.updateGameStatus(gameId, "FINISHED");
        }, "Updating game status should not fail");
    }

    @Test
    void testBannedArtists() throws Exception {
        assertTrue(bannedArtistDao.isArtistBanned("Morgenshtern"), "Should block exact match");
        assertTrue(bannedArtistDao.isArtistBanned("instasamka"), "Should be case-insensitive");
        assertFalse(bannedArtistDao.isArtistBanned("The Beatles"), "Should allow normal artists");
    }
}