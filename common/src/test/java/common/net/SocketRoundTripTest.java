package common.net;

import common.contracts.CommandType;
import common.protocol.CryptoService;
import common.protocol.Message;
import common.protocol.Packet;
import common.protocol.PacketComposer;
import common.protocol.PacketDecomposer;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketRoundTripTest {

    private static final byte[] AES_KEY = "JamMatchTestKey1".getBytes(StandardCharsets.UTF_8);

    @Test
    void singlePacketRoundTrip() throws Exception {
        CryptoService crypto = new CryptoService(AES_KEY);
        AtomicReference<Throwable> serverError = new AtomicReference<>();

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();

            Thread server = new Thread(() -> {
                try (Socket accepted = serverSocket.accept()) {
                    SocketMessageReceiver rx = new SocketMessageReceiver(accepted);
                    SocketMessageSender tx = new SocketMessageSender(accepted);

                    Packet request = PacketDecomposer.decompose(rx.receive(), crypto);

                    Message replyMsg = new Message(
                            request.getbMsg().getcType(),
                            request.getbMsg().getbUserId(),
                            "{\"status\":\"OK\"}".getBytes(StandardCharsets.UTF_8));
                    Packet reply = new Packet(request.getbSrc(), request.getbPktId(), replyMsg);
                    tx.send(PacketComposer.compose(reply, crypto));
                } catch (Throwable t) {
                    serverError.set(t);
                }
            });
            server.setDaemon(true);
            server.start();

            try (Socket clientSocket = new Socket("localhost", port)) {
                SocketMessageSender tx = new SocketMessageSender(clientSocket);
                SocketMessageReceiver rx = new SocketMessageReceiver(clientSocket);

                Message loginMsg = new Message(
                        CommandType.CLIENT_LOGIN.code(), 1,
                        "{\"nickname\":\"vika\"}".getBytes(StandardCharsets.UTF_8));
                tx.send(PacketComposer.compose(new Packet((byte) 1, loginMsg), crypto));

                Packet reply = PacketDecomposer.decompose(rx.receive(), crypto);

                assertEquals(CommandType.CLIENT_LOGIN.code(), reply.getbMsg().getcType());
                String body = new String(reply.getbMsg().getMessage(), StandardCharsets.UTF_8);
                assertTrue(body.contains("OK"), "reply body should contain OK, was: " + body);
            }

            server.join(2000);
        }

        if (serverError.get() != null) {
            throw new AssertionError("server thread failed", serverError.get());
        }
    }
}
