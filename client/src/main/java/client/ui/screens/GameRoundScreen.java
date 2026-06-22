package client.ui.screens;

import client.ui.AppContext;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import client.ui.components.JVinyl;
import common.messages.RoomMessages.LobbyUpdate;
import common.messages.RoomMessages.PlayerInfo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;

import java.util.List;

public class GameRoundScreen extends BorderPane {

    private final AppContext ctx;

    public GameRoundScreen(AppContext ctx) {
        this.ctx = ctx;
        setPadding(new Insets(36, 80, 36, 80));
        getStyleClass().add("bg-bloom");

        ctx.api().onRoomClosed(reason -> {
            new Alert(Alert.AlertType.INFORMATION, reason).showAndWait();
            ctx.show(new MainMenuScreen(ctx));
        });
        ctx.api().onLobbyUpdate(this::render);
    }

    private void render(LobbyUpdate update) {
        setCenter(buildVinyl());
    }

    private VBox buildVinyl() {
        var vinyl = new JVinyl("https://i.scdn.co/image/ab67616d0000b27336980633307bdb638a88ce87");
        VBox box = new VBox(vinyl);
        BorderPane.setAlignment(box, Pos.CENTER);
        BorderPane.setMargin(box, new Insets(28, 0, 0, 0));
        return box;
    }
}
