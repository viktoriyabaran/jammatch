package client.ui.screens;

import client.ui.AppContext;
import client.ui.components.JCountdown;
import client.ui.components.JLabel;
import client.ui.components.JTimer;
import client.ui.components.JVinyl;
import javafx.animation.FadeTransition;
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
import javafx.util.Duration;

import java.util.List;
import java.util.Set;

public class GameRoundScreen extends StackPane {

    private record Player(int id, String nickname, int badge) {}

    private static final int TOTAL_ROUNDS = 5;
    private static final int ROUND_SECONDS = 30;

    private static final List<Player> PLAYERS = List.of(
            new Player(1, "vika_b", 1),
            new Player(2, "dmytro", 2),
            new Player(3, "olena", 3),
            new Player(4, "marko", 4),
            new Player(5, "sofi", 5));

    private static final List<String> ROUND_COVERS = List.of(
            "https://i.scdn.co/image/ab67616d0000b27336980633307bdb638a88ce87",
            "https://i.scdn.co/image/ab67616d0000b27336980633307bdb638a88ce87",
            "https://i.scdn.co/image/ab67616d0000b27336980633307bdb638a88ce87",
            "https://i.scdn.co/image/ab67616d0000b27336980633307bdb638a88ce87",
            "https://i.scdn.co/image/ab67616d0000b27336980633307bdb638a88ce87");

    private static final Set<Integer> VOTED = Set.of(1, 3, 5);

    private final AppContext ctx;

    private final int currentRound = 3;
    private final int myScore = 640;
    private final int trackOwnerId = 2;
    private Integer myVote = null;

    private final JLabel status = new JLabel("", JLabel.Type.META);
    private final HBox votes = new HBox(16);
    private final VBox header = new VBox(20);
    private final BorderPane content = new BorderPane();
    private final JCountdown countdown = new JCountdown(3, this::startRound);

    public GameRoundScreen(AppContext ctx) {
        this.ctx = ctx;
        getStyleClass().add("bg-bloom");

        ctx.api().onRoomClosed(reason -> {
            new Alert(Alert.AlertType.INFORMATION, reason).showAndWait();
            ctx.show(new MainMenuScreen(ctx));
        });

        content.setPadding(new Insets(36, 80, 36, 80));
        content.setTop(buildHeader());
        content.setCenter(buildStage());
        content.setBottom(buildVotes());

        getChildren().addAll(content, countdown);
    }

    private VBox buildHeader() {
        var round = new HBox(8,
                roundPart("ROUND", "round-label"),
                roundPart(String.valueOf(currentRound), "round-label--accent"),
                roundPart("OF " + TOTAL_ROUNDS, "round-label"));
        round.setAlignment(Pos.CENTER_LEFT);

        var score = new HBox(14,
                roundPart("YOUR SCORE", "round-label--dim"),
                roundPart(String.valueOf(myScore), "round-label"));
        score.setAlignment(Pos.CENTER_RIGHT);

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var topRow = new HBox(round, spacer, score);
        topRow.setAlignment(Pos.CENTER_LEFT);

        header.getChildren().add(topRow);
        return header;
    }

    private void startRound() {
        var fade = new FadeTransition(Duration.millis(280), countdown);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            getChildren().remove(countdown);
            header.getChildren().add(new JTimer(ROUND_SECONDS, this::showResult));
        });
        fade.play();
    }

    private Label roundPart(String text, String styleClass) {
        var label = new Label(text);
        label.getStyleClass().addAll("round-label", styleClass);
        return label;
    }

    private VBox buildStage() {
        var vinyl = new JVinyl(ROUND_COVERS.get(currentRound - 1));

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
        int voted = VOTED.size();
        status.setText(voted + " OF " + PLAYERS.size() + " VOTED" + (myVote != null ? "  ·  YOU VOTED" : ""));
    }

    private HBox buildVotes() {
        votes.getChildren().clear();
        votes.setAlignment(Pos.CENTER);
        BorderPane.setMargin(votes, new Insets(28, 0, 0, 0));
        for (Player player : PLAYERS) {
            var card = voteCard(player);
            HBox.setHgrow(card, Priority.ALWAYS);
            votes.getChildren().add(card);
        }
        return votes;
    }

    private HBox voteCard(Player player) {
        var dot = new Region();
        dot.getStyleClass().addAll("row__dot", "badge-" + player.badge());

        var name = new Label(player.nickname());
        name.getStyleClass().add("vote-card__name");

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var card = new HBox(12, dot, name, spacer);
        card.getStyleClass().add("vote-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);

        boolean mine = myVote != null && myVote == player.id();
        if (mine) {
            card.getStyleClass().add("vote-card--selected");
            var check = new Label("✓");
            check.getStyleClass().add("vote-card__check");
            card.getChildren().add(check);
        } else if (VOTED.contains(player.id())) {
            var votedDot = new Region();
            votedDot.getStyleClass().add("vote-card__voted");
            card.getChildren().add(votedDot);
        }

        if (myVote == null) {
            card.setCursor(Cursor.HAND);
            card.setOnMouseClicked(e -> castVote(player.id()));
        }
        return card;
    }

    private void castVote(int playerId) {
        if (myVote != null) {
            return;
        }
        myVote = playerId;
        refreshStatus();
        buildVotes();
    }

    private void showResult() {
        ctx.show(new RoundResultScreen(ctx, currentRound, TOTAL_ROUNDS,
                ROUND_COVERS.get(currentRound - 1), nicknameOf(trackOwnerId),
                myVote != null && myVote == trackOwnerId, myScore));
    }

    private String nicknameOf(int playerId) {
        return PLAYERS.stream().filter(p -> p.id() == playerId).findFirst()
                .map(Player::nickname).orElse("?");
    }
}
