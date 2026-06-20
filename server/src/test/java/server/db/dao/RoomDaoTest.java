package server.db.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.db.ConnectionPool;
import server.db.DbInitializer;

import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RoomDaoTest {
    private ConnectionPool pool;
    private RoomDao roomDao;
    private UserDao userDao;

    @BeforeEach
    void setUp() {
        File dbFile = new File("jammatch.db");
        if (dbFile.exists()) {
            dbFile.delete();
        }

        pool = new ConnectionPool();
        DbInitializer initializer = new DbInitializer(pool);
        initializer.initialize();

        roomDao = new RoomDao(pool);
        userDao = new UserDao(pool);
    }

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.closePool();
        }
    }

    @Test
    void createAndRetrieveRoom() throws Exception {
        int hostId = userDao.insertUser("room_host");
        assertTrue(hostId > 0, "Host should be created successfully");

        String testCode = "JAM123";
        int roomId = roomDao.createRoom(testCode, "My Cool Room", hostId, 10, 5, 30);
        assertTrue(roomId > 0, "Room ID should be generated and greater than 0");

        Optional<Integer> foundId = roomDao.getRoomIdByCode(testCode);
        assertTrue(foundId.isPresent(), "Room should be found in the database");
        assertEquals(roomId, foundId.get(), "The found room ID should match the created one");
    }

    @Test
    void retrieveNonExistentRoom() throws Exception {
        Optional<Integer> foundId = roomDao.getRoomIdByCode("WRONG!");
        assertFalse(foundId.isPresent(), "Should return empty Optional for invalid code");
    }
}