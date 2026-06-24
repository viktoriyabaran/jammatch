package client.ui.screens;

import client.net.GameState;
import client.ui.AppContext;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import common.messages.GameMessages.RoundEnd;
import common.messages.GameMessages.RoundResult;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RoundResultScreen extends BorderPane {

    private static final double COVER_SIZE = 180;

    private final AppContext ctx;
    private final GameState game;
    private final RoundEnd result;
    private final int roundNumber;
    private final int ownerId;

    private boolean ready = false;
    private int readyCount = 0;
    private int totalCount;

    public RoundResultScreen(AppContext ctx, RoundEnd re) {
        this.ctx = ctx;
        this.game = ctx.game();
        this.result = re;
        this.roundNumber = re.roundNumber();
        this.ownerId = re.correctUserId();
        this.totalCount = game.order().size();

        setPadding(new Insets(32, 72, 28, 72));
        getStyleClass().add("bg-bloom");

        ctx.api().onRoomClosed(reason -> {
            ctx.audio().stop();
            new Alert(Alert.AlertType.INFORMATION, reason).showAndWait();
            ctx.show(new MainMenuScreen(ctx));
        });
        ctx.api().onReadyUpdate(ru -> {
            readyCount = ru.readyCount();
            totalCount = ru.totalCount();
            setBottom(buildFooter());
        });
        ctx.api().onRoundStart(rs -> {
            if (rs.roundNumber() <= roundNumber) {
                return;
            }
            game.applyRoundStart(rs);
            ctx.show(new GameRoundScreen(ctx, rs));
        });
        ctx.api().onGameOver(go -> ctx.show(new GameResultScreen(ctx, go)));

        setTop(eyebrow("ROUND " + roundNumber + " OF " + game.totalRounds() + "  ·  RESULT", "result-label--dim"));
        setCenter(buildColumns());
        setBottom(buildFooter());
    }

    private VBox buildColumns() {
        var columns = new HBox(56, buildReveal(), buildPanels());
        columns.setAlignment(Pos.TOP_LEFT);
        var wrap = new VBox(columns);
        BorderPane.setMargin(wrap, new Insets(26, 0, 0, 0));
        return wrap;
    }

    private VBox buildReveal() {
        var cover = new StackPane();
        cover.getStyleClass().add("result-cover");
        if (result.coverUrl() != null && !result.coverUrl().isBlank()) {
            var image = new Image(result.coverUrl(), COVER_SIZE, COVER_SIZE, false, true, true);
            var view = new ImageView(image);
            view.setFitWidth(COVER_SIZE);
            view.setFitHeight(COVER_SIZE);
            cover.getChildren().add(view);
        }

        var dot = new Region();
        dot.getStyleClass().addAll("row__dot", "badge-" + game.badge(ownerId));
        HBox.setMargin(dot, new Insets(12, 0, 0, 0));
        var heading = new Label("It was " + game.nickname(ownerId) + "’s track!");
        heading.getStyleClass().add("title");
        heading.setWrapText(true);
        heading.setMaxWidth(260);
        var headingRow = new HBox(12, dot, heading);
        headingRow.setAlignment(Pos.TOP_LEFT);
        VBox.setMargin(headingRow, new Insets(22, 0, 0, 0));

        var title = new JLabel(trackTitle(), JLabel.Type.VALUE);
        VBox.setMargin(title, new Insets(14, 0, 0, 0));

        var reveal = new VBox(cover, headingRow, title);
        reveal.setMinWidth(300);
        reveal.setMaxWidth(300);

        Integer ownerVote = voteOf(ownerId);
        if (ownerVote != null) {
            var ownerVoted = new HBox(10,
                    tinted("TRACK OWNER VOTED FOR", "result-label--dim"),
                    tinted(game.nickname(ownerVote), "fg-badge-" + game.badge(ownerVote)));
            ownerVoted.setAlignment(Pos.CENTER_LEFT);
            VBox.setMargin(ownerVoted, new Insets(16, 0, 0, 0));
            reveal.getChildren().add(ownerVoted);
        }
        return reveal;
    }

    private String trackTitle() {
        String title = result.title() != null ? result.title() : "Unknown track";
        String artist = result.artist();
        return (artist != null && !artist.isBlank() ? title + " — " + artist : title).toUpperCase();
    }

    private Integer voteOf(int userId) {
        for (RoundResult r : result.results()) {
            if (r.userId() == userId) {
                return r.votedUserId() >= 0 ? r.votedUserId() : null;
            }
        }
        return null;
    }

    private VBox buildPanels() {
        var votes = new VBox();
        for (RoundResult r : result.results()) {
            if (r.userId() != ownerId) {
                votes.getChildren().add(voteRow(r));
            }
        }
        VBox.setMargin(votes, new Insets(10, 0, 0, 0));

        var board = new VBox();
        for (Standing s : leaderboard()) {
            board.getChildren().add(standingRow(s));
        }
        VBox.setMargin(board, new Insets(10, 0, 0, 0));

        var leaderboardLabel = eyebrow("LEADERBOARD", "result-label--dim");
        VBox.setMargin(leaderboardLabel, new Insets(30, 0, 0, 0));

        var panels = new VBox(eyebrow("VOTES", "result-label--dim"), votes, leaderboardLabel, board);
        panels.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(panels, Priority.ALWAYS);
        panels.setMaxWidth(Double.MAX_VALUE);
        return panels;
    }

    private HBox voteRow(RoundResult r) {
        var dot = new Region();
        dot.getStyleClass().addAll("row__dot", "badge-" + game.badge(r.userId()));

        var voter = new Label(game.nickname(r.userId()));
        voter.getStyleClass().add("result-vote__voter");
        var arrow = new Label("→");
        arrow.getStyleClass().add("result-vote__arrow");
        var target = new Label(r.votedUserId() >= 0 ? game.nickname(r.votedUserId()) : "—");
        target.getStyleClass().add("result-vote__target");

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label verdict;
        if (r.correct()) {
            verdict = new Label("✓ CORRECT");
            verdict.getStyleClass().add("result-vote__correct");
        } else {
            verdict = new Label("×");
            verdict.getStyleClass().add("result-vote__wrong");
        }

        var row = new HBox(10, dot, voter, arrow, target, spacer, verdict);
        row.getStyleClass().add("result-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private record Standing(int rank, int userId, int delta, int score) {
    }

    private List<Standing> leaderboard() {
        var deltas = new java.util.HashMap<Integer, Integer>();
        for (RoundResult r : result.results()) {
            deltas.put(r.userId(), r.roundPoints());
        }
        var sorted = new ArrayList<>(game.order());
        sorted.sort(Comparator.comparingInt(game::score).reversed());

        var standings = new ArrayList<Standing>();
        for (int i = 0; i < sorted.size(); i++) {
            int id = sorted.get(i);
            standings.add(new Standing(i + 1, id, deltas.getOrDefault(id, 0), game.score(id)));
        }
        return standings;
    }

    private HBox standingRow(Standing s) {
        var rank = new Label(String.valueOf(s.rank()));
        rank.getStyleClass().add("result-rank");

        var dot = new Region();
        dot.getStyleClass().addAll("row__dot", "badge-" + game.badge(s.userId()));

        var name = new Label(game.nickname(s.userId()));
        name.getStyleClass().add("lb-name");

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var delta = new Label(s.delta() > 0 ? "+" + s.delta() : "—");
        delta.getStyleClass().add("lb-delta");

        var trailing = new HBox(8, delta);
        trailing.setAlignment(Pos.CENTER_RIGHT);

        var gap = new Region();
        gap.setMinWidth(30);

        var score = new Label(String.valueOf(s.score()));
        score.getStyleClass().add("lb-score");

        var row = new HBox(14, rank, dot, name, spacer, trailing, gap, score);
        row.getStyleClass().add("lb-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private VBox buildFooter() {
        var status = new HBox(8,
                tinted("PLAYERS READY", "result-label--dim"),
                tinted(readyCount + " / " + totalCount, "result-label--accent"));
        status.setAlignment(Pos.CENTER_LEFT);

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        JButton next;
        if (ready) {
            next = new JButton("WAITING…", JButton.Variant.SECONDARY, false, () -> {
            });
            next.setDisable(true);
        } else {
            next = new JButton("NEXT", JButton.Variant.PRIMARY, false, this::markReady);
        }
        next.setMinWidth(190);

        var bar = new HBox(status, spacer, next);
        bar.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(bar, new Insets(20, 0, 0, 0));

        var footer = new VBox(bar);
        BorderPane.setMargin(footer, new Insets(24, 0, 0, 0));
        return footer;
    }

    private void markReady() {
        ready = true;
        ctx.api().playerReady();
        setBottom(buildFooter());
    }

    private Label eyebrow(String text, String modifier) {
        return tinted(text, modifier);
    }

    private Label tinted(String text, String modifier) {
        var label = new Label(text);
        label.getStyleClass().addAll("result-label", modifier);
        return label;
    }
}
