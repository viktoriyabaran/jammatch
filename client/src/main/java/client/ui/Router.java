package client.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Router {

    private final Stage stage;
    private final StackPane root = new StackPane();
    private Scene scene;

    public Router(Stage stage) {
        this.stage = stage;
    }

    public void show(Parent screen) {
        if (root.getChildren().isEmpty()) {
            root.getChildren().add(screen);
        } else {
            root.getChildren().set(0, screen);
        }
        if (scene == null) {
            scene = new Scene(root, 960, 640);
            scene.getStylesheets().addAll(
                    getClass().getResource("/css/theme.css").toExternalForm(),
                    getClass().getResource("/css/typography.css").toExternalForm(),
                    getClass().getResource("/css/app.css").toExternalForm());
            stage.setScene(scene);
        }
    }

    public void addOverlay(Node node) {
        if (!root.getChildren().contains(node)) {
            root.getChildren().add(node);
        }
    }
}
