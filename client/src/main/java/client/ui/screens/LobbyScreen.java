package client.ui.screens;

import client.ui.Router;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class LobbyScreen extends BorderPane {

    private record Player(String name, boolean isHost, boolean you) {}

    private static final List<Player> PLAYERS = List.of(
            new Player("vi", true, true),
            new Player("sofie", false, false),
            new Player("mark", false, false),
            new Player("andrew", false, false),
            new Player("mia", false, false));

    public LobbyScreen(Router router) {
        setTop(buildHeader(router));
        setCenter(buildPlayers());
        setBottom(buildActions(router));
        setPadding(new Insets(36, 80, 36, 80));
        getStyleClass().add("bg-corner");
    }

    private HBox buildHeader(Router router) {
        var sectionName = new JLabel("LOBBY", JLabel.Type.SECTION);
        var roomName = new JLabel("your room", JLabel.Type.DISPLAY);
        var roomCode = new JLabel("X7K9P2", JLabel.Type.CODE);
        var roomInfo = new VBox(6, sectionName, roomName, roomCode);

        var editSettings = new JLabel("EDIT SETTINGS  ›", JLabel.Type.SECTION);
        editSettings.setCursor(Cursor.HAND);
        editSettings.setOnMouseClicked(e -> router.show(new CreateRoomScreen(router)));
        var roundCount = new JLabel("5 ROUNDS", JLabel.Type.SUBTITLE);
        var roundDuration = new JLabel("30 SECONDS / ROUND", JLabel.Type.SUBTITLE);
        var gameInfo = new VBox(6, editSettings, roundCount, roundDuration);
        gameInfo.setAlignment(Pos.TOP_RIGHT);

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var header = new HBox(roomInfo, spacer, gameInfo);
        header.setAlignment(Pos.TOP_LEFT);
        return header;
    }

    private VBox buildPlayers() {
        var label = new JLabel("PLAYERS · " + PLAYERS.size() + " / 8", JLabel.Type.FIELD);

        var grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.setMaxWidth(Double.MAX_VALUE);
        var left = new ColumnConstraints();
        left.setPercentWidth(50);
        var right = new ColumnConstraints();
        right.setPercentWidth(50);
        grid.getColumnConstraints().addAll(left, right);

        for (int i = 0; i < PLAYERS.size(); i++) {
            grid.add(playerRow(PLAYERS.get(i), i), i % 2, i / 2);
        }

        var section = new VBox(16, label, grid);
        BorderPane.setAlignment(section, Pos.TOP_LEFT);
        BorderPane.setMargin(section, new Insets(28, 0, 0, 0));
        return section;
    }

    private HBox playerRow(Player player, int index) {
        var dot = new Region();
        dot.getStyleClass().addAll("row__dot", "badge-" + (index % 7 + 1));

        var name = new Label(player.name());
        name.getStyleClass().add("row__title");

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var row = new HBox(dot, name, spacer);
        row.getStyleClass().add("row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setCursor(Cursor.DEFAULT);
        row.setMaxWidth(Double.MAX_VALUE);

        if (player.you()) {
            var tag = new Label(player.isHost() ? "HOST · YOU" : "YOU");
            tag.getStyleClass().add("tag");
            row.getChildren().add(tag);
        } else {
            var kick = new Label("KICK ×");
            kick.getStyleClass().add("row__kick");
            kick.setOnMouseClicked(e -> { /* TODO: kick player */ });
            row.getChildren().add(kick);
        }
        return row;
    }

    private HBox buildActions(Router router) {
        var leaveRoom = new JButton("‹  LEAVE ROOM", JButton.Variant.GHOST, false,
                () -> router.show(new MainMenuScreen(router)));
        var startGame = new JButton("START GAME", JButton.Variant.PRIMARY,
                () -> { /* TODO: start the game */ });
        startGame.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(startGame, Priority.ALWAYS);

        var actions = new HBox(24, leaveRoom, startGame);
        actions.setAlignment(Pos.CENTER_LEFT);
        BorderPane.setMargin(actions, new Insets(24, 0, 0, 0));
        return actions;
    }
}
