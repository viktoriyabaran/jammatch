package client.net;

import common.contracts.CommandType;
import common.messages.RoomMessages.JoinRoom;
import common.messages.RoomMessages.RoomConfig;
import common.messages.SessionMessages.ClientLogin;

public class GameApi {

    private final ServerConnection conn;
    private final Session session;

    public GameApi(ServerConnection conn, Session session) {
        this.conn = conn;
        this.session = session;
    }

    public void login(String nickname, Runnable onSuccess, ServerConnection.MessageHandler onError) {
        conn.send(CommandType.CLIENT_LOGIN, 0, new ClientLogin(nickname), body -> {
            if (ServerConnection.isError(body)) {
                onError.handle(ServerConnection.errorMessage(body));
            } else {
                session.login(Integer.parseInt(body), nickname);
                onSuccess.run();
            }
        });
    }

    public void createRoom(String roomName, int rounds, int roundDurationSeconds, Runnable onSuccess, ServerConnection.MessageHandler onError) {
        conn.send(CommandType.CREATE_ROOM, session.userId(), new RoomConfig(roomName, 8, rounds, roundDurationSeconds), body -> {
            if (ServerConnection.isError(body)) {
                onError.handle(ServerConnection.errorMessage(body));
            } else {
                onSuccess.run();
            }
        });
    }

    public void joinRoom(String roomCode, Runnable onSuccess, ServerConnection.MessageHandler onError) {
        conn.send(CommandType.JOIN_ROOM, session.userId(), new JoinRoom(roomCode), body -> {
            if (ServerConnection.isError(body)) {
                onError.handle(ServerConnection.errorMessage(body));
            } else {
                onSuccess.run();
            }
        });
    }
}