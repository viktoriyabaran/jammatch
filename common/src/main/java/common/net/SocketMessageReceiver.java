package common.net;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketMessageReceiver implements MessageReceiver {
    private final DataInputStream input;

    public SocketMessageReceiver(Socket socket) throws IOException {
        this.input = new DataInputStream(socket.getInputStream());
    }

    @Override
    public byte[] receive() throws InterruptedException {
        try {
            int length = input.readInt();
            byte[] data = new byte[length];
            input.readFully(data);
            return data;
        } catch (IOException e) {
            throw new InterruptedException("Connection closed: " + e.getMessage());
        }
    }
}
