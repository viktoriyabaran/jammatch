package client.ui;

import client.net.ServerConnection;
import client.net.Session;
import client.ui.screens.LoginScreen;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        Fonts.load();
        ServerConnection conn = new ServerConnection();
        try {
            conn.connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var session = new Session();
        var router = new Router(stage);
        var ctx = new AppContext(router, conn, session);

        ctx.show(new LoginScreen(ctx));
        stage.setTitle("JamMatch");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}