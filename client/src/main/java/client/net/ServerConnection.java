package client.net;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import common.contracts.CommandType;
import common.protocol.CryptoService;
import common.protocol.Message;
import common.protocol.Packet;
import common.protocol.PacketComposer;
import common.protocol.PacketDecomposer;
import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerConnection {

    public interface MessageHandler {
        void handle(String body);
    }

    private static final String HOST = "localhost";
    private static final int PORT = 2503;
    private static final CryptoService crypto =
            new CryptoService("StoreServerTest1".getBytes(StandardCharsets.UTF_8));

    private final Gson gson = new Gson();

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private volatile boolean running;

    private volatile Pending pending;
    private final Map<CommandType, MessageHandler> handlers = new ConcurrentHashMap<>();
    private final Map<CommandType, String> sticky = new ConcurrentHashMap<>();

    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
        running = true;

        Thread reader = new Thread(this::receiveLoop, "server-reader");
        reader.setDaemon(true);
        reader.start();

        System.out.println("[Client] Connected to " + HOST + ":" + PORT);
    }

    public void send(CommandType type, int userId, Object payload, MessageHandler onReply) {
        try {
            String json = (payload == null) ? "{}" : gson.toJson(payload);
            Message msg = new Message(type.code(), userId, json.getBytes(StandardCharsets.UTF_8));
            Packet packet = new Packet((byte) 1, msg);

            pending = new Pending(packet.getbPktId(), onReply);

            byte[] wireData = PacketComposer.compose(packet, crypto);
            out.writeInt(wireData.length);
            out.write(wireData);
            out.flush();

            System.out.println("[Client] Sent " + type + " (pkt " + packet.getbPktId() + ")");
        } catch (IOException e) {
            System.out.println("[Client] Send failed: " + e.getMessage());
        }
    }

    public void on(CommandType type, MessageHandler handler) {
        handlers.put(type, handler);
    }

    public void onSticky(CommandType type, MessageHandler handler) {
        handlers.put(type, handler);
        String last = sticky.get(type);
        if (last != null) {
            deliver(handler, last);
        }
    }

    public void clearSticky(CommandType type) {
        sticky.remove(type);
    }

    public void close() {
        running = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private void receiveLoop() {
        try {
            while (running) {
                int len = in.readInt();
                byte[] responseBytes = new byte[len];
                in.readFully(responseBytes);
                Packet response = PacketDecomposer.decompose(responseBytes, crypto);
                route(response);
            }
        } catch (IOException e) {
            System.out.println("[Client] Disconnected: " + e.getMessage());
        }
    }

    private void route(Packet response) {
        String responseBody = new String(response.getbMsg().getMessage(), StandardCharsets.UTF_8);
        long pktId = response.getbPktId();

        if (pktId != 0) {
            System.out.println("[Client] Got reply (pkt " + pktId + "): " + responseBody);
            Pending waiting = pending;
            if (waiting != null && waiting.pktId == pktId) {
                pending = null;
                deliver(waiting.handler, responseBody);
            }
            return;
        }

        CommandType type;
        try {
            type = CommandType.fromCode(response.getbMsg().getcType());
        } catch (IllegalArgumentException e) {
            return;
        }
        System.out.println("[Client] Got event " + type + ": " + responseBody);
        sticky.put(type, responseBody);
        deliver(handlers.get(type), responseBody);
    }

    private void deliver(MessageHandler handler, String body) {
        if (handler != null) {
            Platform.runLater(() -> handler.handle(body));
        }
    }

    public static boolean isError(String body) {
        try {
            JsonElement element = JsonParser.parseString(body);
            return element.isJsonObject()
                    && element.getAsJsonObject().has("status")
                    && "ERROR".equals(element.getAsJsonObject().get("status").getAsString());
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static String errorMessage(String body) {
        try {
            return JsonParser.parseString(body).getAsJsonObject().get("message").getAsString();
        } catch (RuntimeException e) {
            return "Unknown error";
        }
    }

    private static final class Pending {
        final long pktId;
        final MessageHandler handler;

        Pending(long pktId, MessageHandler handler) {
            this.pktId = pktId;
            this.handler = handler;
        }
    }
}