package client.ui;

import client.net.GameApi;
import client.net.GameState;
import client.net.ServerConnection;
import client.net.Session;
import javafx.scene.Parent;

public class AppContext {

    private final Router router;
    private final ServerConnection conn;
    private final Session session;
    private final GameApi api;
    private GameState game;
    private AudioService audio;

    public AppContext(Router router, ServerConnection conn, Session session) {
        this.router = router;
        this.conn = conn;
        this.session = session;
        this.api = new GameApi(conn, session);
    }

    public ServerConnection conn() {
        return conn;
    }

    public Session session() {
        return session;
    }

    public GameApi api() {
        return api;
    }

    public GameState game() {
        return game;
    }

    public void setGame(GameState game) {
        this.game = game;
    }

    public AudioService audio() {
        if (audio == null) {
            audio = new AudioService();
            router.addOverlay(audio.view());
        }
        return audio;
    }

    public void disposeAudio() {
        if (audio != null) {
            audio.dispose();
            audio = null;
        }
    }

    public void show(Parent screen) {
        router.show(screen);
    }
}