package client.net;

import com.google.gson.Gson;
import common.contracts.CommandType;
import common.messages.RoomMessages.JoinRoom;
import common.messages.RoomMessages.KickPlayer;
import common.messages.RoomMessages.LobbyUpdate;
import common.messages.RoomMessages.RoomClosed;
import common.messages.RoomMessages.RoomConfig;
import common.messages.SessionMessages.ClientLogin;
import common.messages.SessionMessages.LookupNickname;
import common.messages.SessionMessages.SavedPlaylist;
import common.messages.SessionMessages.SavedPlaylists;
import common.messages.SessionMessages.SubmitPlaylist;
import common.messages.SessionMessages.ValidatePlaylist;

import java.util.List;
import java.util.function.Consumer;

public class GameApi {

    private final ServerConnection conn;
    private final Session session;
    private final Gson gson = new Gson();

    public GameApi(ServerConnection conn, Session session) {
        this.conn = conn;
        this.session = session;
    }

    public void login(String nickname, Runnable onSuccess, ServerConnection.MessageHandler onError) {
        conn.send(CommandType.CLIENT_LOGIN, 0, new ClientLogin(nickname, session.clientToken()), body -> {
            if (ServerConnection.isError(body)) {
                onError.handle(ServerConnection.errorMessage(body));
            } else {
                session.login(Integer.parseInt(body), nickname);
                onSuccess.run();
            }
        });
    }

    public void lookupNickname(Consumer<String> handler) {
        conn.send(CommandType.LOOKUP_NICKNAME, 0, new LookupNickname(session.clientToken()), body -> {
            handler.accept(ServerConnection.isError(body) ? "" : body);
        });
    }

    public void listSavedPlaylists(Consumer<List<SavedPlaylist>> handler) {
        conn.send(CommandType.LIST_SAVED_PLAYLISTS, session.userId(), null, body -> {
            if (ServerConnection.isError(body)) {
                handler.accept(List.of());
                return;
            }
            SavedPlaylists result = gson.fromJson(body, SavedPlaylists.class);
            handler.accept(result.items() != null ? result.items() : List.of());
        });
    }

    public void validatePlaylist(String playlistUrl, Consumer<SavedPlaylist> onSuccess,
            ServerConnection.MessageHandler onError) {
        conn.send(CommandType.VALIDATE_PLAYLIST, session.userId(), new ValidatePlaylist(playlistUrl), body -> {
            if (ServerConnection.isError(body)) {
                onError.handle(ServerConnection.errorMessage(body));
            } else {
                onSuccess.accept(gson.fromJson(body, SavedPlaylist.class));
            }
        });
    }

    public void submitPlaylist(String playlistUrl, Runnable onSuccess, ServerConnection.MessageHandler onError) {
        conn.send(CommandType.SUBMIT_PLAYLIST, session.userId(), new SubmitPlaylist(playlistUrl), body -> {
            if (ServerConnection.isError(body)) {
                onError.handle(ServerConnection.errorMessage(body));
            } else {
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

    public void onLobbyUpdate(Consumer<LobbyUpdate> handler) {
        conn.onSticky(CommandType.LOBBY_UPDATE, body -> handler.accept(gson.fromJson(body, LobbyUpdate.class)));
    }

    public void onRoomClosed(ServerConnection.MessageHandler handler) {
        conn.on(CommandType.ROOM_CLOSED, body -> handler.handle(gson.fromJson(body, RoomClosed.class).reason()));
    }

    public void leaveRoom(Runnable onDone) {
        conn.clearSticky(CommandType.LOBBY_UPDATE);
        conn.send(CommandType.LEAVE_ROOM, session.userId(), null, body -> onDone.run());
    }

    public void kickPlayer(int targetUserId) {
        conn.send(CommandType.KICK_PLAYER, session.userId(), new KickPlayer(targetUserId), body -> {
        });
    }

    public void startGame() {
        conn.send(CommandType.START_GAME, session.userId(), null, body -> {
        });
    }
}