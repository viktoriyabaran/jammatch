package client.ui.screens;

import client.ui.AppContext;
import client.ui.components.JButton;
import client.ui.components.JLabel;
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

import java.util.List;

public class RoundResultScreen extends BorderPane {

    private record Vote(String voter, int voterBadge, String target, boolean correct) {}

    private record Standing(int rank, String name, int badge, String delta, int score) {}

    private static final int ROUND = 3;
    private static final int TOTAL_ROUNDS = 5;

    private static final String OWNER = "dmytro";
    private static final int OWNER_BADGE = 2;
    private static final String OWNER_VOTED_FOR = "olena";
    private static final int OWNER_VOTED_BADGE = 3;
    private static final String TRACK_TITLE = "MIDNIGHT CITY — M83";
    private static final String COVER_URL = "https://i.scdn.co/image/ab67616d0000b27336980633307bdb638a88ce87";
    private static final double COVER_SIZE = 180;

    private static final List<Vote> VOTES = List.of(
            new Vote("vi", 1, "dmytro", true),
            new Vote("olena", 3, "marko", false),
            new Vote("marko", 4, "dmytro", true),
            new Vote("sofi", 5, "vika_b", false));

    private static final List<Standing> LEADERBOARD = List.of(
            new Standing(1, "dmytro", 2, "+200", 1240),
            new Standing(2, "vika_b", 1, "+150", 980),
            new Standing(3, "marko", 4, "+150",540));

    private static final int PLAYERS_TOTAL = 5;
    private static final int OTHERS_READY = 2;

    private final AppContext ctx;

    private boolean ready = false;

    public RoundResultScreen(AppContext ctx) {
        this.ctx = ctx;
        setPadding(new Insets(32, 72, 28, 72));
        getStyleClass().add("bg-bloom");

        ctx.api().onRoomClosed(reason -> {
            new Alert(Alert.AlertType.INFORMATION, reason).showAndWait();
            ctx.show(new MainMenuScreen(ctx));
        });

        setTop(eyebrow("ROUND " + ROUND + " OF " + TOTAL_ROUNDS + "  ·  RESULT", "result-label--dim"));
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
        var image = new Image(COVER_URL, COVER_SIZE, COVER_SIZE, false, true, true);
        var view = new ImageView(image);
        view.setFitWidth(COVER_SIZE);
        view.setFitHeight(COVER_SIZE);
        cover.getChildren().add(view);

        var dot = new Region();
        dot.getStyleClass().addAll("row__dot", "badge-" + OWNER_BADGE);
        HBox.setMargin(dot, new Insets(12, 0, 0, 0));
        var heading = new Label("It was " + OWNER + "’s track!");
        heading.getStyleClass().add("title");
        heading.setWrapText(true);
        heading.setMaxWidth(260);
        var headingRow = new HBox(12, dot, heading);
        headingRow.setAlignment(Pos.TOP_LEFT);
        VBox.setMargin(headingRow, new Insets(22, 0, 0, 0));

        var title = new JLabel(TRACK_TITLE, JLabel.Type.VALUE);
        VBox.setMargin(title, new Insets(14, 0, 0, 0));

        var ownerVoted = new HBox(10,
                tinted("TRACK OWNER VOTED FOR", "result-label--dim"),
                tinted(OWNER_VOTED_FOR, "fg-badge-" + OWNER_VOTED_BADGE));
        ownerVoted.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(ownerVoted, new Insets(16, 0, 0, 0));

        var reveal = new VBox(cover, headingRow, title, ownerVoted);
        reveal.setMinWidth(300);
        reveal.setMaxWidth(300);
        return reveal;
    }

    private VBox buildPanels() {
        var votes = new VBox();
        for (Vote v : VOTES) {
            votes.getChildren().add(voteRow(v));
        }
        VBox.setMargin(votes, new Insets(10, 0, 0, 0));

        var board = new VBox();
        for (Standing s : LEADERBOARD) {
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

    private HBox voteRow(Vote v) {
        var dot = new Region();
        dot.getStyleClass().addAll("row__dot", "badge-" + v.voterBadge());

        var voter = new Label(v.voter());
        voter.getStyleClass().add("result-vote__voter");
        var arrow = new Label("→");
        arrow.getStyleClass().add("result-vote__arrow");
        var target = new Label(v.target());
        target.getStyleClass().add("result-vote__target");

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label verdict;
        if (v.correct()) {
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

    private HBox standingRow(Standing s) {
        var rank = new Label(String.valueOf(s.rank()));
        rank.getStyleClass().add("result-rank");

        var dot = new Region();
        dot.getStyleClass().addAll("row__dot", "badge-" + s.badge());

        var name = new Label(s.name());
        name.getStyleClass().add("lb-name");

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var delta = new Label(s.delta());
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
        int readyCount = OTHERS_READY + (ready ? 1 : 0);
        var status = new HBox(8,
                tinted("PLAYERS READY", "result-label--dim"),
                tinted(readyCount + " / " + PLAYERS_TOTAL, "result-label--accent"));
        status.setAlignment(Pos.CENTER_LEFT);

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        JButton next;
        if (ready) {
            next = new JButton("WAITING…", JButton.Variant.SECONDARY, false, () -> {});
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
