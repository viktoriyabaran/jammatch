package server.room;

import common.messages.RoomMessages.RoomSettings;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameRoomTest {

    @Test
    void testConcurrentJoins() throws InterruptedException {
        RoomSettings settings = new RoomSettings(5, 3, 30);
        GameRoom room = new GameRoom("TST123", "Race Room", 1, settings);

        // Хост займає перше місце
        room.addPlayer(1, "Host", false);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 2; i <= 11; i++) {
            final int playerId = i;
            executor.submit(() -> {
                try {
                    latch.await();
                    if (room.addPlayer(playerId, "Player_" + playerId, false)) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        latch.countDown();
        done.await();

        assertEquals(5, room.getPlayers().size(), "В кімнаті не може бути більше гравців, ніж ліміт");
        assertEquals(4, successCount.get(), "Тільки 4 додаткових гравців мали успішно зайти");
    }
}