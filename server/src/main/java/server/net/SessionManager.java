package server.net;

import common.protocol.Packet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;

public class SessionManager {
    private final ConcurrentHashMap<Integer, BlockingQueue<Packet>> sessions = new ConcurrentHashMap<>();

    public void register(int userId, BlockingQueue<Packet> outputQueue) {
        sessions.put(userId, outputQueue);
    }

    public void remove(int userId) {
        sessions.remove(userId);
    }

    public void sendToUser(int userId, Packet packet) {
        BlockingQueue<Packet> queue = sessions.get(userId);
        if (queue != null) {
            try {
                queue.put(packet);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}