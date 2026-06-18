package client.ui;

import client.ui.screens.LoginScreen;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        Fonts.load();
        var router = new Router(stage);
        router.show(new LoginScreen(router));
        stage.setTitle("JamMatch");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}