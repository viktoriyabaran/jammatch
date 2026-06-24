package client.ui.screens;

import client.ui.AppContext;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import client.ui.components.JStepper;
import client.ui.components.JSystemMessage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class CreateRoomScreen extends BorderPane {

    private static final double COLUMN_WIDTH = 380;

    public CreateRoomScreen(AppContext ctx) {
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

        var section = new JLabel("CREATE", JLabel.Type.SECTION);

        var roomNameLabel = new JLabel("ROOM NAME", JLabel.Type.FIELD);
        var roomName = new TextField();
        roomName.getStyleClass().add("input");
        roomName.setMinWidth(COLUMN_WIDTH);
        roomName.setMaxWidth(COLUMN_WIDTH);
        roomName.textProperty().addListener((obs, was, now) -> systemMessage.clear());
        var field = new VBox(14, roomNameLabel, roomName);

        var rounds = new JStepper("ROUNDS", 1, 20, 1, 5, "");
        var duration = new JStepper("DURATION", 10, 120, 5, 30, "s");
        HBox.setHgrow(rounds, Priority.ALWAYS);
        HBox.setHgrow(duration, Priority.ALWAYS);
        var stepperRow = new HBox(rounds, duration);
        stepperRow.getStyleClass().add("stepper-row");
        stepperRow.setMinWidth(COLUMN_WIDTH);
        stepperRow.setMaxWidth(COLUMN_WIDTH);

        var checkMark = new Label("✓");
        checkMark.getStyleClass().add("toggle-check");
        checkMark.setVisible(false);
        var checkBox = new StackPane(checkMark);
        checkBox.getStyleClass().add("toggle-box");
        var toggleLabel = new Label("PLAY AUDIO ON HOST ONLY");
        toggleLabel.getStyleClass().add("toggle-label");
        var hostAudioToggle = new HBox(12, checkBox, toggleLabel);
        hostAudioToggle.getStyleClass().add("toggle");
        hostAudioToggle.setAlignment(Pos.CENTER_LEFT);
        hostAudioToggle.setCursor(Cursor.HAND);
        hostAudioToggle.setMinWidth(COLUMN_WIDTH);
        hostAudioToggle.setMaxWidth(COLUMN_WIDTH);
        boolean[] hostAudioOnly = {false};
        hostAudioToggle.setOnMouseClicked(e -> {
            hostAudioOnly[0] = !hostAudioOnly[0];
            checkMark.setVisible(hostAudioOnly[0]);
            hostAudioToggle.getStyleClass().remove("toggle--on");
            if (hostAudioOnly[0]) {
                hostAudioToggle.getStyleClass().add("toggle--on");
            }
        });

        var createRoomButton = new JButton("CREATE ROOM", JButton.Variant.PRIMARY, () -> {
            String roomNameTrimmed = roomName.getText().trim();
            int roundNumber = rounds.value();
            int roundDuration = duration.value();
            if (roomNameTrimmed.isEmpty()) {
                systemMessage.error("Enter a room name");
                return;
            }
            ctx.api().createRoom(roomNameTrimmed, roundNumber, roundDuration, hostAudioOnly[0],
                    () -> ctx.show(new AddPlaylistScreen(ctx)),
                    systemMessage::error);
        });
        createRoomButton.setMinWidth(COLUMN_WIDTH);
        createRoomButton.setMaxWidth(COLUMN_WIDTH);
        systemMessage.markOnError(roomName, createRoomButton);

        var column = new VBox(section, field, stepperRow, hostAudioToggle, createRoomButton);
        column.setAlignment(Pos.CENTER_LEFT);
        column.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        VBox.setMargin(field, new Insets(24, 0, 0, 0));
        VBox.setMargin(stepperRow, new Insets(16, 0, 0, 0));
        VBox.setMargin(hostAudioToggle, new Insets(18, 0, 0, 0));
        VBox.setMargin(createRoomButton, new Insets(24, 0, 0, 0));

        setTop(top);
        setCenter(column);
        BorderPane.setAlignment(column, Pos.CENTER);
        setPadding(new Insets(36, 80, 36, 80));
        getStyleClass().add("bg-center");
    }
}
