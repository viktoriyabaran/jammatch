package client.ui.screens;

import client.ui.Router;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import javafx.geometry.Pos;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class CreateRoomScreen extends VBox {

    public CreateRoomScreen(Router router) {
        super(12);
        var name = new TextField();
        name.setPromptText("Room name");
        name.setMaxWidth(240);
        var rounds = new Spinner<Integer>(1, 20, 5);
        var duration = new Spinner<Integer>(10, 180, 30);
        getChildren().addAll(
                new JLabel("Create room", JLabel.Type.HEADLINE),
                name,
                new JLabel("ROUNDS", JLabel.Type.FIELD), rounds,
                new JLabel("ROUND DURATION (S)", JLabel.Type.FIELD), duration,
                new JButton("Create", JButton.Variant.PRIMARY,
                        () -> router.show(new LobbyScreen(router))),
                new JButton("Back", JButton.Variant.SECONDARY,
                        () -> router.show(new MainMenuScreen(router))));
        setAlignment(Pos.CENTER);
        getStyleClass().addAll("screen", "bg-center");
    }
}
