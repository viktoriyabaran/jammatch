package server.db.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.db.ConnectionPool;
import server.db.DbInitializer;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class UserDaoTest {
    private ConnectionPool pool;
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
        userDao = new UserDao(pool);
    }

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.closePool();
        }
    }

    @Test
    void insertAndRetrieveUser() throws Exception {
        int id = userDao.insertUser("test_player");
        assertTrue(id > 0, "ID should be generated and greater than 0");

        Optional<String> nickname = userDao.getUserNickname(id);
        assertTrue(nickname.isPresent(), "User should exist in the db");
        assertEquals("test_player", nickname.get());
    }

    @Test
    void testConcurrentInserts() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    userDao.insertUser("player_" + index);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        for (int i = 1; i <= threadCount; i++) {
            Optional<String> name = userDao.getUserNickname(i);
            assertTrue(name.isPresent(), "Player " + i + " was lost due to concurrency issues");
            assertTrue(name.get().startsWith("player_"));
        }
    }
}