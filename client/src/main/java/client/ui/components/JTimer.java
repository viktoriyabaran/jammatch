package client.ui.components;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class JTimer extends HBox {

    private final DoubleProperty remaining = new SimpleDoubleProperty();
    private final double total;
    private final Timeline timeline;

    public JTimer(double totalSeconds, Runnable onFinished) {
        this.total = totalSeconds;
        this.remaining.set(totalSeconds);

        setAlignment(Pos.CENTER);
        setSpacing(28);

        var track = new Region();
        track.getStyleClass().add("timer-track");
        track.setMaxWidth(Double.MAX_VALUE);

        var fill = new Region();
        fill.getStyleClass().add("timer-fill");

        var bar = new StackPane(track, fill);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bar, Priority.ALWAYS);

        fill.prefWidthProperty().bind(bar.widthProperty().multiply(remaining.divide(total)));
        fill.maxWidthProperty().bind(fill.prefWidthProperty());
        fill.minWidthProperty().bind(fill.prefWidthProperty());

        var time = new Label();
        time.getStyleClass().add("timer-time");
        time.textProperty().bind(Bindings.createStringBinding(this::format, remaining));

        getChildren().addAll(bar, time);

        timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(remaining, total, Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(total), new KeyValue(remaining, 0, Interpolator.LINEAR)));
        if (onFinished != null) {
            timeline.setOnFinished(e -> onFinished.run());
        }
        timeline.play();
    }

    private String format() {
        int secs = (int) Math.ceil(remaining.get());
        return secs / 60 + ":" + String.format("%02d", secs % 60);
    }

    public void stop() {
        timeline.stop();
    }
}
