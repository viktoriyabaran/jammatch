package client.ui.screens;

import client.ui.AppContext;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import client.ui.components.JRow;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class AddPlaylistScreen extends BorderPane {

    private static final double COLUMN_WIDTH = 660;

    private record Playlist(String name, int tracks, String visibility) {}

    private static final List<Playlist> SAVED = List.of(
            new Playlist("late night drives", 68, "PUBLIC"),
            new Playlist("gym pump", 31, "PUBLIC"),
            new Playlist("throwback 2010s", 120, "PUBLIC"),
            new Playlist("focus / deep work", 54, "PUBLIC"),
            new Playlist("sunday slow", 22, "PUBLIC"),
            new Playlist("road trip", 88, "PUBLIC"),
            new Playlist("rainy day", 40, "PUBLIC"),
            new Playlist("party starters", 73, "PUBLIC"));

    private JRow selectedRow;
    private Playlist selectedPlaylist;

    public AddPlaylistScreen(AppContext ctx) {
        var back = new JLabel("‹  BACK", JLabel.Type.NAV);
        back.setCursor(Cursor.HAND);
        back.setOnMouseClicked(e -> ctx.show(new CreateRoomScreen(ctx)));

        var sectionName = new JLabel("ADD PLAYLIST", JLabel.Type.SECTION);
        var sectionDescription = new JLabel(
                "Pick one you've used before, or paste a new public YouTube Music link.",
                JLabel.Type.BODY);
        sectionDescription.setWrapText(true);
        sectionDescription.setMaxWidth(COLUMN_WIDTH);
        var section = new VBox(14, sectionName, sectionDescription);

        var savedLabel = new JLabel("SAVED PLAYLISTS", JLabel.Type.FIELD);
        var savedCount = new JLabel(SAVED.size() + " SAVED", JLabel.Type.FIELD);
        var headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        var listHeader = new HBox(savedLabel, headerSpacer, savedCount);
        listHeader.setMinWidth(COLUMN_WIDTH);
        listHeader.setMaxWidth(COLUMN_WIDTH);

        var listContent = new VBox(10);
        for (Playlist playlist : SAVED) {
            var cover = new Region();
            cover.getStyleClass().add("row__cover");
            var row = new JRow(cover, playlist.name(),
                    playlist.tracks() + " TRACKS · " + playlist.visibility());
            row.setOnMouseClicked(e -> select(row, playlist));
            listContent.getChildren().add(row);
        }

        var scroll = new ScrollPane(listContent);
        scroll.getStyleClass().add("list-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMinWidth(COLUMN_WIDTH);
        scroll.setMaxWidth(COLUMN_WIDTH);

        var dividerLabel = new JLabel("OR PASTE A LINK", JLabel.Type.FIELD);
        dividerLabel.setMaxWidth(Double.MAX_VALUE);
        dividerLabel.setAlignment(Pos.CENTER);

        var playlistLink = new TextField();
        playlistLink.getStyleClass().addAll("input", "input--default");
        playlistLink.setPromptText("ENTER PLAYLIST LINK..");
        playlistLink.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(playlistLink, Priority.ALWAYS);
        var validate = new JButton("VALIDATE", JButton.Variant.SECONDARY, false,
                () -> { /* TODO: validate the pasted link */ });
        var pasteRow = new HBox(12, playlistLink, validate);
        pasteRow.setAlignment(Pos.CENTER_LEFT);
        pasteRow.setMinWidth(COLUMN_WIDTH);
        pasteRow.setMaxWidth(COLUMN_WIDTH);

        var continueButton = new JButton("CONTINUE", JButton.Variant.PRIMARY,
                () -> ctx.show(new LobbyScreen(ctx))); // TODO: carry the chosen playlist forward
        continueButton.setMinWidth(COLUMN_WIDTH);
        continueButton.setMaxWidth(COLUMN_WIDTH);

        var column = new VBox(section, listHeader, scroll, dividerLabel, pasteRow, continueButton);
        column.setAlignment(Pos.CENTER_LEFT);
        column.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        VBox.setMargin(listHeader, new Insets(28, 0, 0, 0));
        VBox.setMargin(scroll, new Insets(12, 0, 0, 0));
        VBox.setMargin(dividerLabel, new Insets(24, 0, 0, 0));
        VBox.setMargin(pasteRow, new Insets(16, 0, 0, 0));
        VBox.setMargin(continueButton, new Insets(20, 0, 0, 0));

        setTop(back);
        setCenter(column);
        BorderPane.setAlignment(column, Pos.CENTER);
        setPadding(new Insets(36, 80, 36, 80));
        getStyleClass().add("bg-corner");
    }

    private void select(JRow row, Playlist playlist) {
        if (selectedRow != null) {
            selectedRow.setSelected(false);
        }
        selectedRow = row;
        selectedPlaylist = playlist;
        row.setSelected(true);
    }
}
