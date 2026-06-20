package client.ui;

import client.net.GameApi;
import client.net.ServerConnection;
import client.net.Session;
import javafx.scene.Parent;

public class AppContext {

    private final Router router;
    private final ServerConnection conn;
    private final Session session;
    private final GameApi api;

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

    public void show(Parent screen) {
        router.show(screen);
    }
}