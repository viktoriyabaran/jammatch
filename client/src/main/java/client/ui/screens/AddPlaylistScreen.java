package client.ui.screens;

import client.ui.AppContext;
import client.ui.components.JButton;
import client.ui.components.JCover;
import client.ui.components.JLabel;
import client.ui.components.JRow;
import client.ui.components.JSystemMessage;
import common.messages.SessionMessages.SavedPlaylist;
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
import javafx.scene.text.TextAlignment;

import java.util.HashMap;
import java.util.Map;

public class AddPlaylistScreen extends BorderPane {

    private static final double COLUMN_WIDTH = 660;

    private final AppContext ctx;
    private final JSystemMessage systemMessage = new JSystemMessage();
    private final JLabel savedCount = new JLabel("0 SAVED", JLabel.Type.FIELD);
    private final VBox listContent = new VBox(10);
    private final Map<String, JRow> rowsByUrl = new HashMap<>();
    private final Map<String, SavedPlaylist> savedByUrl = new HashMap<>();
    private final TextField playlistLink = new TextField();
    private final JButton validate;

    private JRow selectedRow;
    private SavedPlaylist selectedPlaylist;

    public AddPlaylistScreen(AppContext ctx) {
        this.ctx = ctx;

        var back = new JLabel("‹  BACK", JLabel.Type.NAV);
        back.setCursor(Cursor.HAND);
        back.setOnMouseClicked(e -> ctx.show(new CreateRoomScreen(ctx)));

        systemMessage.setTextAlignment(TextAlignment.RIGHT);
        systemMessage.setMaxWidth(460);
        var top = new BorderPane();
        top.setLeft(back);
        top.setRight(systemMessage);
        BorderPane.setAlignment(back, Pos.TOP_LEFT);
        BorderPane.setAlignment(systemMessage, Pos.TOP_RIGHT);

        var sectionName = new JLabel("ADD PLAYLIST", JLabel.Type.SECTION);
        var sectionDescription = new JLabel(
                "Pick one you've used before, or paste a new public YouTube Music link.",
                JLabel.Type.BODY);
        sectionDescription.setWrapText(true);
        sectionDescription.setMaxWidth(COLUMN_WIDTH);
        var section = new VBox(14, sectionName, sectionDescription);

        var savedLabel = new JLabel("SAVED PLAYLISTS", JLabel.Type.FIELD);
        var headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        var listHeader = new HBox(savedLabel, headerSpacer, savedCount);
        listHeader.setMinWidth(COLUMN_WIDTH);
        listHeader.setMaxWidth(COLUMN_WIDTH);

        ctx.api().listSavedPlaylists(saved -> {
            listContent.getChildren().clear();
            rowsByUrl.clear();
            savedByUrl.clear();
            for (SavedPlaylist playlist : saved) {
                addPlaylistRow(playlist, false);
            }
            refreshCount();
        });

        var scroll = new ScrollPane(listContent);
        scroll.getStyleClass().add("list-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMinWidth(COLUMN_WIDTH);
        scroll.setMaxWidth(COLUMN_WIDTH);

        var dividerLabel = new JLabel("OR PASTE A LINK", JLabel.Type.FIELD);
        dividerLabel.setMaxWidth(Double.MAX_VALUE);
        dividerLabel.setAlignment(Pos.CENTER);

        playlistLink.getStyleClass().addAll("input", "input--default");
        playlistLink.setPromptText("ENTER PLAYLIST LINK..");
        playlistLink.setMaxWidth(Double.MAX_VALUE);
        playlistLink.textProperty().addListener((obs, was, now) -> systemMessage.clear());
        HBox.setHgrow(playlistLink, Priority.ALWAYS);

        validate = new JButton("VALIDATE", JButton.Variant.SECONDARY, false, this::onValidate);
        systemMessage.markOnError(playlistLink, validate);
        var pasteRow = new HBox(12, playlistLink, validate);
        pasteRow.setAlignment(Pos.CENTER_LEFT);
        pasteRow.setMinWidth(COLUMN_WIDTH);
        pasteRow.setMaxWidth(COLUMN_WIDTH);

        var continueButton = new JButton("CONTINUE", JButton.Variant.PRIMARY, this::onContinue);
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

        setTop(top);
        setCenter(column);
        BorderPane.setAlignment(column, Pos.CENTER);
        setPadding(new Insets(36, 80, 36, 80));
        getStyleClass().add("bg-center");
    }

    private void onValidate() {
        String url = playlistLink.getText().trim();
        if (url.isEmpty()) {
            systemMessage.error("Paste a playlist link first");
            return;
        }
        String alreadyAdded = findSavedByLink(url);
        if (alreadyAdded != null) {
            playlistLink.clear();
            select(rowsByUrl.get(alreadyAdded), savedByUrl.get(alreadyAdded));
            systemMessage.info("Already in your saved playlists");
            return;
        }
        systemMessage.clear();
        validate.setDisable(true);
        systemMessage.info("Validating playlist…");
        ctx.api().validatePlaylist(url, playlist -> {
            validate.setDisable(false);
            playlistLink.clear();
            addPlaylistRow(playlist, true);
            refreshCount();
            systemMessage.success("Added " + playlist.name() + " · " + playlist.trackCount() + " songs");
        }, message -> {
            validate.setDisable(false);
            systemMessage.error(message);
        });
    }

    private void onContinue() {
        String url = selectedPlaylist != null ? selectedPlaylist.url() : null;
        if (url == null) {
            systemMessage.error("Pick a saved playlist please");
            return;
        }
        ctx.api().submitPlaylist(url, () -> ctx.show(new LobbyScreen(ctx)), systemMessage::error);
    }

    private void addPlaylistRow(SavedPlaylist playlist, boolean select) {
        JRow existing = rowsByUrl.remove(playlist.url());
        savedByUrl.remove(playlist.url());
        if (existing != null) {
            listContent.getChildren().remove(existing);
        }

        var title = playlist.name() != null ? playlist.name() : playlist.url();
        var meta = playlist.trackCount() > 0 ? playlist.trackCount() + " SONGS" : playlist.url();
        var row = new JRow(new JCover(playlist.coverUrl()), title, meta);
        row.setOnMouseClicked(e -> select(row, playlist));

        listContent.getChildren().add(0, row);
        rowsByUrl.put(playlist.url(), row);
        savedByUrl.put(playlist.url(), playlist);
        if (select) {
            select(row, playlist);
        }
    }

    private void select(JRow row, SavedPlaylist playlist) {
        if (selectedRow != null) {
            selectedRow.setSelected(false);
        }
        selectedRow = row;
        selectedPlaylist = playlist;
        row.setSelected(true);
    }

    private void refreshCount() {
        savedCount.setText(rowsByUrl.size() + " SAVED");
    }

    private String findSavedByLink(String url) {
        String id = playlistId(url);
        for (String savedUrl : savedByUrl.keySet()) {
            if (playlistId(savedUrl).equals(id)) {
                return savedUrl;
            }
        }
        return null;
    }

    private static String playlistId(String url) {
        int index = url.indexOf("list=");
        if (index < 0) {
            return url;
        }
        String rest = url.substring(index + 5);
        int amp = rest.indexOf('&');
        return amp < 0 ? rest : rest.substring(0, amp);
    }
}
