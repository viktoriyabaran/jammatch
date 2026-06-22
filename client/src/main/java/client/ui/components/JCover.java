package client.ui.components;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class JCover extends StackPane {

    private static final double SIZE = 44;

    public JCover(String url) {
        getStyleClass().add("row__cover");
        if (url != null && !url.isBlank()) {
            var image = new Image(url, SIZE, SIZE, false, true, true);
            var view = new ImageView(image);
            view.setFitWidth(SIZE);
            view.setFitHeight(SIZE);
            getChildren().add(view);
        }
    }
}
