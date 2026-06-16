package server.net;

import common.protocol.*;
import common.contracts.*;
import server.db.dao.RoomDao;
import server.db.dao.UserDao;
import server.room.RoomManager;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Processor implements Runnable {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private final int id = NEXT_ID.getAndIncrement();
    private final BlockingQueue<Packet> input;
    private final BlockingQueue<Packet> output;

    private final UserDao userDao;
    private final RoomDao roomDao;
    private final RoomManager roomManager;

    public Processor(BlockingQueue<Packet> input, BlockingQueue<Packet> output, UserDao userDao, RoomDao roomDao,
            RoomManager roomManager) {
        this.input = input;
        this.output = output;
        this.userDao = userDao;
        this.roomDao = roomDao;
        this.roomManager = roomManager;
    }

    private Packet process(Packet request) {
        Message msg = request.getbMsg();
        CommandType command;
        try {
            command = CommandType.fromCode(msg.getcType());
        } catch (IllegalArgumentException e) {
            return buildResponse(request, ResponseCode.INTERNAL_ERROR, "Unknown command code");
        }

        return buildResponse(request, ResponseCode.OK, null);
    }

    private Packet buildResponse(Packet request, ResponseCode status, String errorMessage) {
        String body = status == ResponseCode.OK
                ? "{\"status\":\"OK\"}"
                : "{\"status\":\"ERROR\",\"message\":\"" + escape(errorMessage) + "\"}";

        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        Message responseMsg = new Message(
                request.getbMsg().getcType(),
                request.getbMsg().getbUserId(),
                payload);
        return new Packet(request.getbSrc(), request.getbPktId(), responseMsg);
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Packet request = input.take();
                try {
                    Packet response = process(request);
                    output.put(response);
                } catch (RuntimeException e) {
                    System.err.println(e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}