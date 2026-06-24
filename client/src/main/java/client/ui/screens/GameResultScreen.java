package client.ui.screens;

import client.net.GameState;
import client.ui.AppContext;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import common.messages.GameMessages.GameOver;
import common.messages.GameMessages.LeaderboardEntry;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class GameResultScreen extends BorderPane {

    private record Standing(int rank, String name, int badge, int correct, int score) {
        boolean winner() { return rank == 1; }
        boolean podium() { return rank <= 3; }
    }

    private static final double REVEAL_MS = 380;
    private static final double INITIAL_DELAY_MS = 420;
    private static final double STAGGER_MS = 260;
    private static final double RAMP_MS = 70;
    private static final double WINNER_SUSPENSE_MS = 1100;

    private final AppContext ctx;
    private final GameState game;
    private final int totalGuesses;
    private final List<Standing> standings;
    private final List<HBox> rows = new ArrayList<>();

    public GameResultScreen(AppContext ctx, GameOver go) {
        this.ctx = ctx;
        this.game = ctx.game();
        this.totalGuesses = game.totalRounds();
        this.standings = standings(go);

        setPadding(new Insets(36, 80, 36, 80));
        getStyleClass().add("bg-bloom");

        ctx.api().onRoomClosed(reason -> {
            new Alert(Alert.AlertType.INFORMATION, reason).showAndWait();
            ctx.show(new MainMenuScreen(ctx));
        });
        ctx.api().onRoundStart(rs -> {
            game.applyRoundStart(rs);
            ctx.show(new GameRoundScreen(ctx, rs));
        });

        setTop(header());
        setCenter(board());
        setBottom(footer());

        revealRows();
    }

    private List<Standing> standings(GameOver go) {
        var result = new ArrayList<Standing>();
        for (LeaderboardEntry e : go.leaderboard()) {
            result.add(new Standing(e.rank(), e.nickname(), game.badge(e.userId()),
                    game.correct(e.userId()), e.score()));
        }
        return result;
    }

    private VBox header() {
        return new VBox(6,
                label("FINAL STANDINGS", "result-label", "result-label--accent"),
                new JLabel("Game Over", JLabel.Type.HEADLINE));
    }

    private GridPane board() {
        var grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.setMaxWidth(Double.MAX_VALUE);

        var left = new ColumnConstraints();
        left.setPercentWidth(50);
        var right = new ColumnConstraints();
        right.setPercentWidth(50);
        grid.getColumnConstraints().addAll(left, right);

        for (int i = 0; i < standings.size(); i++) {
            var row = row(standings.get(i));
            rows.add(row);
            grid.add(row, i % 2, i / 2);
        }

        BorderPane.setAlignment(grid, Pos.CENTER_LEFT);
        BorderPane.setMargin(grid, new Insets(28, 0, 0, 0));
        return grid;
    }

    private HBox footer() {
        boolean host = game.hostId() == ctx.session().userId();

        var leaveRoom = new JButton("‹  LEAVE ROOM", JButton.Variant.GHOST, false,
                () -> ctx.api().leaveRoom(() -> ctx.show(new MainMenuScreen(ctx))));
        var footer = new HBox(24, leaveRoom);
        footer.setAlignment(Pos.CENTER_LEFT);

        if (host) {
            var playAgain = new JButton("PLAY AGAIN", JButton.Variant.PRIMARY, () -> ctx.api().startGame(() -> {
            }));
            playAgain.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(playAgain, Priority.ALWAYS);
            footer.getChildren().add(playAgain);
        } else {
            var spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            var waiting = new JLabel("WAITING FOR HOST…", JLabel.Type.SUBTITLE);
            footer.getChildren().addAll(spacer, waiting);
        }
        BorderPane.setMargin(footer, new Insets(24, 0, 0, 0));
        return footer;
    }

    private void revealRows() {
        double delay = INITIAL_DELAY_MS;
        for (int rank = rows.size(); rank >= 1; rank--) {
            reveal(rows.get(rank - 1), delay, rank == 1);
            double gap = STAGGER_MS + (rows.size() - rank) * RAMP_MS;
            if (rank == 2) gap += WINNER_SUSPENSE_MS;
            delay += gap;
        }
    }

    private void reveal(HBox row, double delayMs, boolean winner) {
        row.setOpacity(0);
        row.setTranslateY(16);

        var fade = new FadeTransition(Duration.millis(REVEAL_MS), row);
        fade.setFromValue(0);
        fade.setToValue(1);

        var rise = new TranslateTransition(Duration.millis(REVEAL_MS), row);
        rise.setFromY(16);
        rise.setToY(0);
        rise.setInterpolator(Interpolator.EASE_OUT);

        var animation = new ParallelTransition(row, fade, rise);

        if (winner) {
            row.setScaleX(0.85);
            row.setScaleY(0.85);
            var overshoot = new ScaleTransition(Duration.millis(300), row);
            overshoot.setFromX(0.85);
            overshoot.setFromY(0.85);
            overshoot.setToX(1.05);
            overshoot.setToY(1.05);
            overshoot.setInterpolator(Interpolator.EASE_OUT);
            var settle = new ScaleTransition(Duration.millis(220), row);
            settle.setFromX(1.05);
            settle.setFromY(1.05);
            settle.setToX(1);
            settle.setToY(1);
            settle.setInterpolator(Interpolator.EASE_BOTH);
            animation.getChildren().add(new SequentialTransition(overshoot, settle));
        }

        animation.setDelay(Duration.millis(delayMs));
        animation.play();
    }

    private HBox row(Standing s) {
        var rank = label(String.format("%02d", s.rank()), "go-rank");
        style(rank, s.winner(), "go-rank--win");
        style(rank, !s.podium(), "go-rank--out");

        var dot = new Region();
        dot.getStyleClass().addAll("row__dot", "badge-" + s.badge());

        var name = label(s.name(), "go-name");
        name.setMinWidth(Region.USE_PREF_SIZE);
        style(name, !s.podium(), "go-name--out");

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var count = label(s.correct() + " / " + totalGuesses, "go-count");
        count.setMinWidth(54);
        count.setAlignment(Pos.CENTER_RIGHT);

        var score = label(String.valueOf(s.score()), "go-score");
        style(score, !s.podium(), "go-score--out");
        score.setMinWidth(80);
        score.setAlignment(Pos.CENTER_RIGHT);

        var trailing = new HBox(18, count, score);
        trailing.setAlignment(Pos.CENTER_RIGHT);
        if (s.winner()) {
            var badge = label("WINNER", "go-winner");
            badge.setMinWidth(Region.USE_PREF_SIZE);
            trailing.getChildren().add(0, badge);
        }

        var row = new HBox(16, rank, dot, name, spacer, trailing);
        row.getStyleClass().add("go-row");
        style(row, s.podium(), "go-row--card");
        style(row, s.winner(), "go-row--win");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private static Label label(String text, String... classes) {
        var label = new Label(text);
        label.getStyleClass().addAll(classes);
        return label;
    }

    private static void style(Node node, boolean on, String styleClass) {
        if (on) node.getStyleClass().add(styleClass);
    }
}
