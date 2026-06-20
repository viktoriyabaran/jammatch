package client.ui.screens;

import client.ui.Router;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MainMenuScreen extends BorderPane {

    private static final double COLUMN_WIDTH = 380;

    public MainMenuScreen(Router router) {
        var headline = new JLabel("Welcome, love", JLabel.Type.HEADLINE);
        var notYou = new JLabel("NOT YOU?", JLabel.Type.NAV);
        var changeNickname = new JLabel("CHANGE NICKNAME  ›", JLabel.Type.SECTION);
        changeNickname.setCursor(Cursor.HAND);
        changeNickname.setOnMouseClicked(e -> router.show(new LoginScreen(router)));

        var subline = new HBox(12, notYou, changeNickname);
        subline.setAlignment(Pos.CENTER_LEFT);
        var header = new VBox(8, headline, subline);

        var createRoomButton = new JButton("CREATE ROOM", JButton.Variant.PRIMARY,
                () -> router.show(new CreateRoomScreen(router)));
        createRoomButton.setMinWidth(COLUMN_WIDTH);
        createRoomButton.setMaxWidth(COLUMN_WIDTH);

        var joinRoomButton = new JButton("JOIN ROOM", JButton.Variant.PRIMARY,
                () -> router.show(new JoinRoomScreen(router)));
        joinRoomButton.setMinWidth(COLUMN_WIDTH);
        joinRoomButton.setMaxWidth(COLUMN_WIDTH);

        var about = new JLabel("ABOUT / HOW TO PLAY  ›", JLabel.Type.NAV);
        about.setCursor(Cursor.HAND);
        about.setOnMouseClicked(e -> { /* TODO: router.show(new AboutScreen(router)); */ });

        var column = new VBox(header, createRoomButton, joinRoomButton, about);
        column.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(createRoomButton, new Insets(48, 0, 0, 0));
        VBox.setMargin(joinRoomButton,  new Insets(16, 0, 0, 0));
        VBox.setMargin(about,           new Insets(40, 0, 0, 0));

        setCenter(column);
        BorderPane.setAlignment(column, Pos.CENTER_LEFT);
        setPadding(new Insets(0, 80, 36, 80));
        getStyleClass().add("bg-corner");
    }
}
