package client.ui.screens;

import client.ui.Router;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import client.ui.components.Stepper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextFormatter;

public class JoinRoomScreen extends BorderPane {

    private static final double COLUMN_WIDTH = 380;

    public JoinRoomScreen(Router router) {
        var back = new JLabel("‹  BACK", JLabel.Type.NAV);
        back.setCursor(Cursor.HAND);
        back.setOnMouseClicked(e -> router.show(new MainMenuScreen(router)));

        var section = new JLabel("JOIN ROOM", JLabel.Type.SECTION);

        var roomCodeLabel = new JLabel("ROOM CODE", JLabel.Type.FIELD);
        var roomCode = new TextField();
        roomCode.getStyleClass().addAll("input", "code", "input--default");
        roomCode.setMinWidth(COLUMN_WIDTH);
        roomCode.setMaxWidth(COLUMN_WIDTH);
        roomCode.setTextFormatter(new TextFormatter<>(change -> {
            change.setText(change.getText().toUpperCase());
            return change;
        }));
        var field = new VBox(14, roomCodeLabel, roomCode);

        var joinRoomButton = new JButton("JOIN", JButton.Variant.PRIMARY,
                () -> router.show(new JoinRoomScreen(router))); // TODO: show "Add playlist" screen
        joinRoomButton.setMinWidth(COLUMN_WIDTH);
        joinRoomButton.setMaxWidth(COLUMN_WIDTH);

        var column = new VBox(section, field, joinRoomButton);
        column.setAlignment(Pos.CENTER_LEFT);
        column.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        VBox.setMargin(field, new Insets(24, 0, 0, 0));
        VBox.setMargin(joinRoomButton, new Insets(16, 0, 0, 0));

        setTop(back);
        setCenter(column);
        BorderPane.setAlignment(column, Pos.CENTER);
        setPadding(new Insets(36, 80, 36, 80));
        getStyleClass().add("bg-corner");
    }
}
