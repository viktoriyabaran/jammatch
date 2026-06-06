package server.net;

import common.protocol.*;
import common.contracts.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Processor implements Runnable {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private final int id = NEXT_ID.getAndIncrement();
    private final BlockingQueue<Packet> input;
    private final BlockingQueue<Packet> output;

    public Processor(BlockingQueue<Packet> input, BlockingQueue<Packet> output) {
        this.input = input;
        this.output = output;
    }

    private Packet process(Packet request) {
        Message msg = request.getbMsg();
        CommandType command;
        try {
            System.out.println("[Processor " + id + "] Processing message");
            command = CommandType.fromCode(msg.getcType());
        } catch (IllegalArgumentException e) {
            System.err.println("[Processor " + id + "] Unknown command code: " + msg.getcType());
            return buildResponse(request, ResponseCode.INTERNAL_ERROR,
                    "Unknown command code: " + msg.getcType());
        }

        System.out.println("[Processor] " + command + " from user " + msg.getbUserId());
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
                payload
        );
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
                    System.err.println("[Processor] Error processing packet: " + e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}