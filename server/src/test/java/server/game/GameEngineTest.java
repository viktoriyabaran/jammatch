package server.game;

import common.messages.RoomMessages.RoomSettings;
import org.junit.jupiter.api.Test;
import server.room.GameRoom;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GameEngineTest {

    @Test
    void testConcurrentVotingRaceCondition() throws Exception {
        RoomSettings settings = new RoomSettings(100, 3, 30, false);
        GameRoom room = new GameRoom("TEST99", "Race Room", 1, settings);
        for (int i = 1; i <= 100; i++) {
            room.addPlayer(i, "Player" + i, true);
        }

        GameEngine engine = new GameEngine(room, null, null, null, null, null, null, null);

        setPrivateField(engine, "roundActive", new AtomicBoolean(true));
        setPrivateField(engine, "currentCorrectUserId", 42);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int i = 1; i <= threadCount; i++) {
            final int voterId = i;
            executor.submit(() -> {
                try {
                    latch.await();
                    engine.submitVote(voterId, 42);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        latch.countDown();
        done.await();

        AtomicReference<Integer> firstCorrectVoter = getPrivateField(engine, "firstCorrectVoter");

        assertNotNull(firstCorrectVoter.get(), "Хтось мав стати першим");
        System.out.println("The winner of the race condition is Player ID: " + firstCorrectVoter.get());

    }

    private void setPrivateField(Object object, String fieldName, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(Object object, String fieldName) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(object);
    }
}