package common.net;

import common.protocol.CryptoService;
import common.protocol.Packet;
import common.protocol.PacketDecomposer;

import java.util.concurrent.BlockingQueue;

public class Decryptor implements Runnable {
    private final BlockingQueue<byte[]> input;
    private final BlockingQueue<Packet> output;
    private final CryptoService crypto;

    public Decryptor(BlockingQueue<byte[]> input, BlockingQueue<Packet> output, CryptoService crypto) {
        this.input = input;
        this.output = output;
        this.crypto = crypto;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                byte[] data = input.take();
                try {
                    Packet p = PacketDecomposer.decompose(data, crypto);
                    output.put(p);
                } catch (RuntimeException e) {
                    System.err.println("[Decryptor] Bad packet: " + e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
