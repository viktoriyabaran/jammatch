package client.ui.screens;

import client.ui.Router;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;

public class MainMenuScreen extends VBox {

    public MainMenuScreen(Router router) {
        super(16);
        getChildren().addAll(
                new JLabel("Main menu", JLabel.Type.HEADLINE),
                new JButton("Create room", JButton.Variant.PRIMARY,
                        () -> router.show(new CreateRoomScreen(router))),
                new JButton("Join room", JButton.Variant.SECONDARY,
                        () -> router.show(new JoinRoomScreen(router))));
        setAlignment(Pos.CENTER);
        getStyleClass().addAll("screen", "bg-corner");
    }
}
