package client.ui.screens;

import client.ui.AppContext;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import common.messages.RoomMessages.LobbyUpdate;
import common.messages.RoomMessages.PlayerInfo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class LobbyScreen extends BorderPane {

    private final AppContext ctx;

    public LobbyScreen(AppContext ctx) {
        this.ctx = ctx;
        setPadding(new Insets(36, 80, 36, 80));
        getStyleClass().add("bg-corner");

        ctx.api().onRoomClosed(reason -> {
            new Alert(Alert.AlertType.INFORMATION, reason).showAndWait();
            ctx.show(new MainMenuScreen(ctx));
        });
        ctx.api().onLobbyUpdate(this::render);
    }

    private void render(LobbyUpdate update) {
        boolean host = update.hostId() == ctx.session().userId();
        setTop(buildHeader(update));
        setCenter(buildPlayers(update, host));
        setBottom(buildActions(host));
    }

    private HBox buildHeader(LobbyUpdate update) {
        var sectionName = new JLabel("LOBBY", JLabel.Type.SECTION);
        var roomName = new JLabel(update.roomName(), JLabel.Type.DISPLAY);

        var roomCode = new JLabel(update.roomCode(), JLabel.Type.CODE);
        roomCode.getStyleClass().add("copyable");
        roomCode.setCursor(Cursor.HAND);
        var copied = new JLabel("", JLabel.Type.META);
        copied.getStyleClass().add("system-message--success");
        roomCode.setOnMouseClicked(e -> {
            var content = new ClipboardContent();
            content.putString(update.roomCode());
            Clipboard.getSystemClipboard().setContent(content);
            copied.setText("COPIED ;)");
        });
        var codeRow = new HBox(12, roomCode, copied);
        codeRow.setAlignment(Pos.CENTER_LEFT);
        var roomInfo = new VBox(6, sectionName, roomName, codeRow);

        var roundCount = new JLabel(update.settings().rounds() + " ROUNDS", JLabel.Type.SUBTITLE);
        var roundDuration = new JLabel(update.settings().roundDurationSeconds() + " SECONDS / ROUND",
                JLabel.Type.SUBTITLE);
        var gameInfo = new VBox(6, roundCount, roundDuration);
        gameInfo.setAlignment(Pos.TOP_RIGHT);

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var header = new HBox(roomInfo, spacer, gameInfo);
        header.setAlignment(Pos.TOP_LEFT);
        return header;
    }

    private VBox buildPlayers(LobbyUpdate update, boolean host) {
        List<PlayerInfo> players = update.players();
        var label = new JLabel("PLAYERS · " + players.size() + " / " + update.settings().maxPlayers(),
                JLabel.Type.FIELD);

        var grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.setMaxWidth(Double.MAX_VALUE);
        var left = new ColumnConstraints();
        left.setPercentWidth(50);
        var right = new ColumnConstraints();
        right.setPercentWidth(50);
        grid.getColumnConstraints().addAll(left, right);

        for (int i = 0; i < players.size(); i++) {
            grid.add(playerRow(players.get(i), update.hostId(), host, i), i % 2, i / 2);
        }

        var section = new VBox(16, label, grid);
        BorderPane.setAlignment(section, Pos.TOP_LEFT);
        BorderPane.setMargin(section, new Insets(28, 0, 0, 0));
        return section;
    }

    private HBox playerRow(PlayerInfo player, int hostId, boolean iAmHost, int index) {
        var dot = new Region();
        dot.getStyleClass().addAll("row__dot", "badge-" + (index % 7 + 1));

        var name = new Label(player.nickname());
        name.getStyleClass().add("row__title");

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var row = new HBox(dot, name, spacer);
        row.getStyleClass().add("row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setCursor(Cursor.DEFAULT);
        row.setMaxWidth(Double.MAX_VALUE);

        var songs = new Label(player.hasPlaylist() ? player.songCount() + " SONGS" : "LOADING..");
        songs.getStyleClass().add("row__meta");
        row.getChildren().add(songs);

        boolean isMe = player.id() == ctx.session().userId();
        boolean isHost = player.id() == hostId;

        if (isMe) {
            var tag = new Label(isHost ? "HOST · YOU" : "YOU");
            tag.getStyleClass().add("tag");
            row.getChildren().add(tag);
        } else if (isHost) {
            var tag = new Label("HOST");
            tag.getStyleClass().add("tag");
            row.getChildren().add(tag);
        } else if (iAmHost) {
            var kick = new Label("KICK ×");
            kick.getStyleClass().add("row__kick");
            kick.setCursor(Cursor.HAND);
            kick.setOnMouseClicked(e -> ctx.api().kickPlayer(player.id()));
            row.getChildren().add(kick);
        }
        return row;
    }

    private HBox buildActions(boolean host) {
        var leaveRoom = new JButton("‹  LEAVE ROOM", JButton.Variant.GHOST, false,
                () -> ctx.api().leaveRoom(() -> ctx.show(new MainMenuScreen(ctx))));

        var actions = new HBox(24, leaveRoom);
        actions.setAlignment(Pos.CENTER_LEFT);
        BorderPane.setMargin(actions, new Insets(24, 0, 0, 0));

        if (host) {
            var startGame = new JButton("START GAME", JButton.Variant.PRIMARY,
                    () -> ctx.api().startGame());
            startGame.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(startGame, Priority.ALWAYS);
            actions.getChildren().add(startGame);
        } else {
            var waiting = new JLabel("WAITING FOR HOST…", JLabel.Type.SUBTITLE);
            var spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            actions.getChildren().addAll(spacer, waiting);
        }
        return actions;
    }
}
