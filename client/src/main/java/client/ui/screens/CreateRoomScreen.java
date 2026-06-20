package client.ui.screens;

import client.ui.Router;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import client.ui.components.JStepper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class CreateRoomScreen extends BorderPane {

    private static final double COLUMN_WIDTH = 380;

    public CreateRoomScreen(Router router) {
        var back = new JLabel("‹  BACK", JLabel.Type.NAV);
        back.setCursor(Cursor.HAND);
        back.setOnMouseClicked(e -> router.show(new MainMenuScreen(router)));

        var section = new JLabel("CREATE", JLabel.Type.SECTION);

        var roomNameLabel = new JLabel("ROOM NAME", JLabel.Type.FIELD);
        var roomName = new TextField();
        roomName.getStyleClass().add("input");
        roomName.setMinWidth(COLUMN_WIDTH);
        roomName.setMaxWidth(COLUMN_WIDTH);
        var field = new VBox(14, roomNameLabel, roomName);

        var rounds = new JStepper("ROUNDS", 1, 20, 1, 5, "");
        var duration = new JStepper("DURATION", 10, 120, 5, 30, "s");
        HBox.setHgrow(rounds, Priority.ALWAYS);
        HBox.setHgrow(duration, Priority.ALWAYS);
        var stepperRow = new HBox(rounds, duration);
        stepperRow.getStyleClass().add("stepper-row");
        stepperRow.setMinWidth(COLUMN_WIDTH);
        stepperRow.setMaxWidth(COLUMN_WIDTH);

        var createRoomButton = new JButton("CREATE ROOM", JButton.Variant.PRIMARY,
                () -> router.show(new AddPlaylistScreen(router)));
        createRoomButton.setMinWidth(COLUMN_WIDTH);
        createRoomButton.setMaxWidth(COLUMN_WIDTH);

        var column = new VBox(section, field, stepperRow, createRoomButton);
        column.setAlignment(Pos.CENTER_LEFT);
        column.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        VBox.setMargin(field, new Insets(24, 0, 0, 0));
        VBox.setMargin(stepperRow, new Insets(16, 0, 0, 0));
        VBox.setMargin(createRoomButton, new Insets(24, 0, 0, 0));

        setTop(back);
        setCenter(column);
        BorderPane.setAlignment(column, Pos.CENTER);
        setPadding(new Insets(36, 80, 36, 80));
        getStyleClass().add("bg-corner");
    }
}
