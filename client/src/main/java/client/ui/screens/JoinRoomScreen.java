package client.ui.screens;

import client.ui.AppContext;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import client.ui.components.JSystemMessage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextFormatter;
import javafx.scene.text.TextAlignment;

public class JoinRoomScreen extends BorderPane {

    private static final double COLUMN_WIDTH = 380;

    public JoinRoomScreen(AppContext ctx) {
        var back = new JLabel("‹  BACK", JLabel.Type.NAV);
        back.setCursor(Cursor.HAND);
        back.setOnMouseClicked(e -> ctx.show(new MainMenuScreen(ctx)));

        var systemMessage = new JSystemMessage();
        systemMessage.setTextAlignment(TextAlignment.RIGHT);
        systemMessage.setMaxWidth(460);
        var top = new BorderPane();
        top.setLeft(back);
        top.setRight(systemMessage);
        BorderPane.setAlignment(back, Pos.TOP_LEFT);
        BorderPane.setAlignment(systemMessage, Pos.TOP_RIGHT);

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
        roomCode.textProperty().addListener((obs, was, now) -> systemMessage.clear());
        var field = new VBox(14, roomCodeLabel, roomCode);

        var joinRoomButton = getJoinRoomButton(ctx, roomCode, systemMessage);

        var column = new VBox(section, field, joinRoomButton);
        column.setAlignment(Pos.CENTER_LEFT);
        column.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        VBox.setMargin(field, new Insets(24, 0, 0, 0));
        VBox.setMargin(joinRoomButton, new Insets(16, 0, 0, 0));

        setTop(top);
        setCenter(column);
        BorderPane.setAlignment(column, Pos.CENTER);
        setPadding(new Insets(36, 80, 36, 80));
        getStyleClass().add("bg-center");
    }

    private static JButton getJoinRoomButton(AppContext ctx, TextField roomCode, JSystemMessage systemMessage) {
        var joinRoomButton = new JButton("JOIN", JButton.Variant.PRIMARY, () -> {
            String code = roomCode.getText().trim();
            if (!code.matches("[A-Z0-9]{6}")) {
                systemMessage.error("Enter a valid 6-character room code");
                return;
            }
            ctx.api().joinRoom(code,
                    () -> ctx.show(new AddPlaylistScreen(ctx)),
                    systemMessage::error);
        });
        joinRoomButton.setMinWidth(COLUMN_WIDTH);
        joinRoomButton.setMaxWidth(COLUMN_WIDTH);
        systemMessage.markOnError(roomCode, joinRoomButton);
        return joinRoomButton;
    }
}
