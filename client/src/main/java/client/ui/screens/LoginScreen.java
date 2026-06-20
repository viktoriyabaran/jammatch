package client.ui.screens;

import client.ui.Router;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LoginScreen extends BorderPane {

    private static final double COLUMN_WIDTH = 380;

    public LoginScreen(Router router) {
        var wordmark = new JLabel("SSSLY", JLabel.Type.WORDMARK);
        var subtitle = new JLabel("GUESS YOUR SPOTY JAMS", JLabel.Type.SUBTITLE);
        var header = new VBox(6, wordmark, subtitle);

        var nicknameLabel = new JLabel("NICKNAME", JLabel.Type.FIELD);
        var nickname = new TextField("");
        nickname.setPromptText("nickname");
        nickname.getStyleClass().addAll("input", "input--filled");
        nickname.setMinWidth(COLUMN_WIDTH);
        nickname.setMaxWidth(COLUMN_WIDTH);
        var field = new VBox(14, nicknameLabel, nickname);

        var continueButton = new JButton("Continue", JButton.Variant.PRIMARY,
                () -> router.show(new MainMenuScreen(router)));
        continueButton.setMinWidth(COLUMN_WIDTH);
        continueButton.setMaxWidth(COLUMN_WIDTH);

        var column = new VBox(header, field, continueButton);
        column.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(field, new Insets(56, 0, 0, 0));
        VBox.setMargin(continueButton, new Insets(16, 0, 0, 0));

        var dot = new JLabel("◉", JLabel.Type.SECTION);
        var contact = new JLabel("@sssly", JLabel.Type.SECTION);
        var footerLeft = new HBox(8, dot, contact);
        footerLeft.setAlignment(Pos.CENTER_LEFT);
        var version = new JLabel("v0.1.0", JLabel.Type.META);
        var footer = new BorderPane();
        footer.setLeft(footerLeft);
        footer.setRight(version);
        BorderPane.setAlignment(footerLeft, Pos.CENTER_LEFT);
        BorderPane.setAlignment(version, Pos.CENTER_RIGHT);

        setCenter(column);
        BorderPane.setAlignment(column, Pos.CENTER_LEFT);
        setBottom(footer);
        setPadding(new Insets(0, 80, 36, 80));
        getStyleClass().add("bg-corner");

        Platform.runLater(nickname::requestFocus);
    }
}
