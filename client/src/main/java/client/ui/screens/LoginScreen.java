package client.ui.screens;

import client.ui.Router;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginScreen extends VBox {

    public LoginScreen(Router router) {
        super(16);
        var nickname = new TextField();
        nickname.setPromptText("Nickname");
        nickname.setMaxWidth(240);
        var enter = new JButton("Enter", JButton.Variant.PRIMARY,
                () -> router.show(new MainMenuScreen(router)));
        getChildren().addAll(new JLabel("JamMatch", JLabel.Type.WORDMARK), nickname, enter);
        setAlignment(Pos.CENTER);
        getStyleClass().addAll("screen", "bg-corner");
    }
}
