package common.net;

import java.util.concurrent.BlockingQueue;

public class Sender implements Runnable {
    private final MessageSender sink;
    private final BlockingQueue<byte[]> input;

    public Sender(MessageSender sink, BlockingQueue<byte[]> input) {
        this.sink = sink;
        this.input = input;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                byte[] data = input.take();
                sink.send(data);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
