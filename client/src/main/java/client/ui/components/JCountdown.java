package client.ui.components;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class JCountdown extends StackPane {

    private final Label number = new Label();

    public JCountdown(int from, Runnable onComplete) {
        this(from, onComplete, true);
    }

    public JCountdown(int from, Runnable onComplete, boolean scrim) {
        getStyleClass().add("countdown-overlay");
        if (!scrim) {
            getStyleClass().add("countdown-overlay--clear");
            setMouseTransparent(true);
        }
        setAlignment(Pos.CENTER);

        number.getStyleClass().add("countdown-num");
        getChildren().add(number);

        tick(from, onComplete);
    }

    private void tick(int n, Runnable onComplete) {
        if (n == 0) {
            onComplete.run();
            return;
        }
        number.setText(String.valueOf(n));

        var scaleIn = new ScaleTransition(Duration.millis(240), number);
        scaleIn.setFromX(0.5);
        scaleIn.setFromY(0.5);
        scaleIn.setToX(1);
        scaleIn.setToY(1);
        scaleIn.setInterpolator(Interpolator.EASE_OUT);

        var fadeIn = new FadeTransition(Duration.millis(200), number);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        var hold = new PauseTransition(Duration.millis(500));

        var fadeOut = new FadeTransition(Duration.millis(260), number);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        var step = new SequentialTransition(new ParallelTransition(scaleIn, fadeIn), hold, fadeOut);
        step.setOnFinished(e -> tick(n - 1, onComplete));
        step.play();
    }
}
