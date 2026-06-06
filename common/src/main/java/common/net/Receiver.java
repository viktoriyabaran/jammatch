package common.net;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class Receiver implements Runnable {
    private final MessageReceiver source;
    private final BlockingQueue<byte[]> output;

    public Receiver(MessageReceiver source, BlockingQueue<byte[]> output) {
        this.source = source;
        this.output = output;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    byte[] data = source.receive();
                    output.put(data);
                } catch (RuntimeException e) {
                    System.err.println("[Receiver] Runtime error: " + e.getMessage());
                } catch (IOException e) {
                    System.err.println("[Receiver] Error receiving: " + e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
