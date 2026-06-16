package client.net;

import common.protocol.*;
import common.contracts.CommandType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class GameClient {
    private static final String HOST = "localhost";
    private static final int PORT = 2503;
    private static final CryptoService crypto = new CryptoService("StoreServerTest1".getBytes(StandardCharsets.UTF_8));

    private static int myUserId = 0;

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PORT);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream())) {

            System.out.println("\tConnected to Game Server");
            System.out.println("Available commands:");
            System.out.println("1. login <nickname>");
            System.out.println("2. create");
            System.out.println("3. join <roomCode>");
            System.out.println("4. kick <userId>");
            System.out.println("5. leave\n");

            Thread listener = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        int len = in.readInt();
                        byte[] responseBytes = new byte[len];
                        in.readFully(responseBytes);
                        Packet response = PacketDecomposer.decompose(responseBytes, crypto);

                        String responseBody = new String(response.getbMsg().getMessage(), StandardCharsets.UTF_8);
                        CommandType cmd = CommandType.fromCode(response.getbMsg().getcType());

                        System.out.println("\n[SERVER -> " + cmd + "]: " + responseBody);
                        System.out.print("> ");

                        if (cmd == CommandType.CLIENT_LOGIN && !responseBody.contains("ERROR")) {
                            myUserId = Integer.parseInt(responseBody);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("\n[Disconnected from server]");
                }
            });
            listener.start();

            Scanner scanner = new Scanner(System.in);
            int pktId = 1;

            System.out.print("> ");
            while (true) {
                String input = scanner.nextLine().trim();
                if (input.isEmpty())
                    continue;

                String[] parts = input.split(" ");
                String cmd = parts[0].toLowerCase();
                Message msg = null;

                try {
                    if (cmd.equals("login") && parts.length > 1) {
                        String payload = "{\"nickname\":\"" + parts[1] + "\"}";
                        msg = new Message(CommandType.CLIENT_LOGIN.code(), 0, payload.getBytes(StandardCharsets.UTF_8));
                    } else if (cmd.equals("create")) {
                        String payload = "{\"roomName\":\"Demo Room\", \"maxPlayers\":5, \"rounds\":3, \"roundDurationSeconds\":30}";
                        msg = new Message(CommandType.CREATE_ROOM.code(), myUserId,
                                payload.getBytes(StandardCharsets.UTF_8));
                    } else if (cmd.equals("join") && parts.length > 1) {
                        String payload = "{\"roomCode\":\"" + parts[1] + "\"}";
                        msg = new Message(CommandType.JOIN_ROOM.code(), myUserId,
                                payload.getBytes(StandardCharsets.UTF_8));
                    } else if (cmd.equals("kick") && parts.length > 1) {
                        String payload = "{\"targetUserId\":" + parts[1] + "}";
                        msg = new Message(CommandType.KICK_PLAYER.code(), myUserId,
                                payload.getBytes(StandardCharsets.UTF_8));
                    } else if (cmd.equals("leave")) {
                        msg = new Message(CommandType.LEAVE_ROOM.code(), myUserId,
                                "{}".getBytes(StandardCharsets.UTF_8));
                    } else {
                        System.out.println("Unknown command or incorrect format.");
                        System.out.print("> ");
                        continue;
                    }

                    Packet packet = new Packet((byte) 1, pktId++, msg);
                    byte[] wireData = PacketComposer.compose(packet, crypto);
                    out.writeInt(wireData.length);
                    out.write(wireData);
                    out.flush();

                } catch (Exception e) {
                    System.out.println("Sending error: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Server not running or connection error: " + e.getMessage());
        }
    }
}