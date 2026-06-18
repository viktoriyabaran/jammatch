package client.ui.screens;

import client.ui.Router;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import javafx.geometry.Pos;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

public class LobbyScreen extends VBox {

    public LobbyScreen(Router router) {
        super(16);
        var players = new ListView<String>();
        players.getItems().add("You (host)");
        players.setMaxHeight(160);
        getChildren().addAll(
                new JLabel("Lobby", JLabel.Type.HEADLINE),
                players,
                new JButton("Start", JButton.Variant.PRIMARY, () -> {}),
                new JButton("Leave", JButton.Variant.SECONDARY,
                        () -> router.show(new MainMenuScreen(router))));
        setAlignment(Pos.CENTER);
        getStyleClass().addAll("screen", "bg-corner");
    }
}
