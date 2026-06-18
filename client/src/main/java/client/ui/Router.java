package client.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Router {

    private final Stage stage;
    private Scene scene;

    public Router(Stage stage) {
        this.stage = stage;
    }

    public void show(Parent screen) {
        if (scene == null) {
            scene = new Scene(screen, 960, 640);
            scene.getStylesheets().addAll(
                    getClass().getResource("/css/theme.css").toExternalForm(),
                    getClass().getResource("/css/typography.css").toExternalForm(),
                    getClass().getResource("/css/app.css").toExternalForm());
            stage.setScene(scene);
        } else {
            scene.setRoot(screen);
        }
    }
}
