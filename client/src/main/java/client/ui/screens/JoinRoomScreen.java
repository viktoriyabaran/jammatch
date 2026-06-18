package client.ui.screens;

import client.ui.Router;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class JoinRoomScreen extends VBox {

    public JoinRoomScreen(Router router) {
        super(16);
        var code = new TextField();
        code.setPromptText("Room code");
        code.setMaxWidth(240);
        getChildren().addAll(
                new JLabel("Join room", JLabel.Type.HEADLINE),
                code,
                new JButton("Submit", JButton.Variant.PRIMARY,
                        () -> router.show(new LobbyScreen(router))),
                new JButton("Back", JButton.Variant.SECONDARY,
                        () -> router.show(new MainMenuScreen(router))));
        setAlignment(Pos.CENTER);
        getStyleClass().addAll("screen", "bg-center");
    }
}
