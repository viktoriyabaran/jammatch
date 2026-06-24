package client.ui.screens;

import client.net.GameState;
import client.ui.AppContext;
import client.ui.AudioService;
import client.ui.components.JCountdown;
import client.ui.components.JLabel;
import client.ui.components.JTimer;
import client.ui.components.JVinyl;
import common.messages.GameMessages.RoundEnd;
import common.messages.GameMessages.RoundStart;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class GameRoundScreen extends StackPane {

    private static final int COUNTDOWN_FROM = 3;

    private final AppContext ctx;
    private final GameState game;
    private final int roundNumber;
    private final List<Integer> options;
    private final BorderPane content = new BorderPane();

    private Integer myVote = null;
    private int votedCount = 0;
    private int totalVoters;

    private final JLabel status = new JLabel("", JLabel.Type.META);
    private final HBox votes = new HBox(16);
    private JTimer timer;

    private JCountdown countdown;
    private boolean countdownShown = false;
    private boolean deferNavigation = false;
    private boolean countdownDone = false;
    private RoundEnd pendingEnd;

    public GameRoundScreen(AppContext ctx, RoundStart rs) {
        this.ctx = ctx;
        this.game = ctx.game();
        this.roundNumber = rs.roundNumber();
        this.options = rs.options();
        this.totalVoters = game.order().size();

        getStyleClass().add("bg-bloom");
        content.setPadding(new Insets(36, 80, 36, 80));
        getChildren().add(content);

        ctx.api().onRoomClosed(reason -> {
            ctx.audio().stop();
            new Alert(Alert.AlertType.INFORMATION, reason).showAndWait();
            ctx.show(new MainMenuScreen(ctx));
        });
        ctx.api().onVoteProgress(vp -> {
            votedCount = vp.votedCount();
            totalVoters = vp.totalCount();
            refreshStatus();
            if (totalVoters > 0 && votedCount >= totalVoters) {
                showCountdown(false);
            }
        });
        ctx.api().onRoundEnd(re -> {
            if (re.roundNumber() != roundNumber) {
                return;
            }
            if (timer != null) {
                timer.stop();
            }
            pendingEnd = re;
            if (!deferNavigation || countdownDone) {
                advance();
            }
        });

        content.setTop(buildHeader(rs));
        content.setCenter(buildStage(rs));
        content.setBottom(buildVotes());

        ctx.audio().play(rs.videoId(), AudioService.DEFAULT_OFFSET);
    }

    private VBox buildHeader(RoundStart rs) {
        var round = new HBox(8,
                roundPart("ROUND", "round-label"),
                roundPart(String.valueOf(roundNumber), "round-label--accent"),
                roundPart("OF " + rs.totalRounds(), "round-label"));
        round.setAlignment(Pos.CENTER_LEFT);

        var score = new HBox(14,
                roundPart("YOUR SCORE", "round-label--dim"),
                roundPart(String.valueOf(game.score(ctx.session().userId())), "round-label"));
        score.setAlignment(Pos.CENTER_RIGHT);

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var topRow = new HBox(round, spacer, score);
        topRow.setAlignment(Pos.CENTER_LEFT);

        timer = new JTimer(rs.durationSeconds(), null);
        timer.atSecondsRemaining(COUNTDOWN_FROM, () -> showCountdown(true));
        return new VBox(20, topRow, timer);
    }

    private Label roundPart(String text, String styleClass) {
        var label = new Label(text);
        label.getStyleClass().addAll("round-label", styleClass);
        return label;
    }

    private VBox buildStage(RoundStart rs) {
        var vinyl = new JVinyl(rs.coverUrl());

        var prompt = new Label("WHOSE TRACK IS THIS?");
        prompt.getStyleClass().add("round-prompt");

        refreshStatus();

        var caption = new VBox(14, prompt, status);
        caption.setAlignment(Pos.CENTER);
        VBox.setMargin(caption, new Insets(36, 0, 0, 0));

        var stage = new VBox(vinyl, caption);
        stage.setAlignment(Pos.CENTER);
        BorderPane.setAlignment(stage, Pos.CENTER);
        return stage;
    }

    private void refreshStatus() {
        status.setText(votedCount + " OF " + totalVoters + " VOTED" + (myVote != null ? "  ·  YOU VOTED" : ""));
    }

    private HBox buildVotes() {
        votes.getChildren().clear();
        votes.setAlignment(Pos.CENTER);
        BorderPane.setMargin(votes, new Insets(28, 0, 0, 0));
        for (Integer id : options) {
            var card = voteCard(id);
            HBox.setHgrow(card, Priority.ALWAYS);
            votes.getChildren().add(card);
        }
        return votes;
    }

    private HBox voteCard(int playerId) {
        var dot = new Region();
        dot.getStyleClass().addAll("row__dot", "badge-" + game.badge(playerId));

        var name = new Label(game.nickname(playerId));
        name.getStyleClass().add("vote-card__name");

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var card = new HBox(12, dot, name, spacer);
        card.getStyleClass().add("vote-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);

        boolean mine = myVote != null && myVote == playerId;
        if (mine) {
            card.getStyleClass().add("vote-card--selected");
            var check = new Label("✓");
            check.getStyleClass().add("vote-card__check");
            card.getChildren().add(check);
        }

        if (myVote == null) {
            card.setCursor(Cursor.HAND);
            card.setOnMouseClicked(e -> castVote(playerId));
        }
        return card;
    }

    private void castVote(int playerId) {
        if (myVote != null) {
            return;
        }
        myVote = playerId;
        ctx.api().submitVote(roundNumber, playerId);
        refreshStatus();
        content.setBottom(buildVotes());
    }

    private void showCountdown(boolean votable) {
        if (countdownShown) {
            return;
        }
        countdownShown = true;
        deferNavigation = !votable;
        Runnable onComplete = votable ? () -> getChildren().remove(countdown) : this::onCountdownComplete;
        countdown = new JCountdown(COUNTDOWN_FROM, onComplete, !votable);
        getChildren().add(countdown);
    }

    private void onCountdownComplete() {
        countdownDone = true;
        if (pendingEnd != null) {
            advance();
        } else {
            getChildren().remove(countdown);
        }
    }

    private void advance() {
        if (pendingEnd == null) {
            return;
        }
        game.applyRoundEnd(pendingEnd);
        ctx.show(new RoundResultScreen(ctx, pendingEnd));
    }
}
