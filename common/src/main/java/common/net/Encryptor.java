package common.net;

import common.protocol.CryptoService;
import common.protocol.Packet;
import common.protocol.PacketComposer;

import java.util.concurrent.BlockingQueue;

public class Encryptor implements Runnable {
    private final BlockingQueue<Packet> input;
    private final BlockingQueue<byte[]> output;
    private final CryptoService crypto;

    public Encryptor(BlockingQueue<Packet> input, BlockingQueue<byte[]> output, CryptoService crypto) {
        this.input = input;
        this.output = output;
        this.crypto = crypto;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Packet data = input.take();
                try {
                    byte[] m = PacketComposer.compose(data, crypto);
                    output.put(m);
                } catch (RuntimeException e) {
                    System.err.println("[Encryptor] Bad packet: " + e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
