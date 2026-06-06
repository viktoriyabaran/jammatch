package client.net;

import common.protocol.*;
import common.contracts.CommandType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class GameClient {
    private static final String HOST = "localhost";
    private static final int PORT = 2503;
    private static final int SOCKET_TIMEOUT_MS = 2000;
    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MS = 1000;
    private static final CryptoService crypto =
            new CryptoService("StoreServerTest1".getBytes(StandardCharsets.UTF_8));

    public static void main(String[] args) throws InterruptedException {
        int requestCounter = 0;
        int retryCounter = 0;

        while (!Thread.currentThread().isInterrupted()) {
            try (Socket socket = new Socket(HOST, PORT);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {

                socket.setSoTimeout(SOCKET_TIMEOUT_MS);
                retryCounter = 0;

                while (!Thread.currentThread().isInterrupted()) {
                    sendAndReceive(out, in, requestCounter++);
                    Thread.sleep(2000);
                }

            } catch (IOException e) {
                retryCounter++;
                System.out.println("[Client] Connection issue (" + retryCounter + "/" + MAX_RETRIES + "): " + e.getMessage());

                if (retryCounter > MAX_RETRIES) {
                    System.err.println("[Client] Giving up after " + MAX_RETRIES + " retries");
                    break;
                }

                long backoffMs = BASE_BACKOFF_MS << (retryCounter - 1);
                System.out.println("[Client] Connection lost (retry " + retryCounter + "/" + MAX_RETRIES + ") in " + backoffMs + "ms");
                Thread.sleep(backoffMs);
            }
        }
    }

    private static void sendAndReceive(DataOutputStream out, DataInputStream in, int i) throws IOException {
        Message msg = new Message(
                CommandType.CLIENT_LOGIN.code(),
                1000 + i,
                ("request №" + i).getBytes(StandardCharsets.UTF_8)
        );
        Packet packet = new Packet((byte) 1, msg);
        byte[] wireData = PacketComposer.compose(packet, crypto);

        out.writeInt(wireData.length);
        out.write(wireData);
        out.flush();

        int len = in.readInt();
        byte[] responseBytes = new byte[len];
        in.readFully(responseBytes);
        Packet response = PacketDecomposer.decompose(responseBytes, crypto);

        String responseBody = new String(response.getbMsg().getMessage(), StandardCharsets.UTF_8);
        System.out.println("[Client] Got response: " + responseBody);
    }
}
