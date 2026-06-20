package client.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class Stepper extends HBox {

    private final int min;
    private final int max;
    private final int step;
    private final String suffix;
    private final Label valueLabel = new Label();
    private int value;

    public Stepper(String label, int min, int max, int step, int initial, String suffix) {
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
        this.value = initial;

        getStyleClass().add("stepper");
        setAlignment(Pos.CENTER_LEFT);
        setMaxWidth(Double.MAX_VALUE);

        var name = new Label(label);
        name.getStyleClass().add("stepper-label");

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var minus = new Label("–");
        minus.getStyleClass().add("stepper-btn");
        minus.setOnMouseClicked(e -> set(value - step));

        valueLabel.getStyleClass().add("stepper-value");

        var plus = new Label("+");
        plus.getStyleClass().add("stepper-btn");
        plus.setOnMouseClicked(e -> set(value + step));

        var controls = new HBox(minus, valueLabel, plus);
        controls.getStyleClass().add("stepper-controls");
        controls.setAlignment(Pos.CENTER);

        getChildren().addAll(name, spacer, controls);
        render();
    }

    private void set(int next) {
        value = Math.max(min, Math.min(max, next));
        render();
    }

    private void render() {
        valueLabel.setText(value + suffix);
    }

    public int value() {
        return value;
    }
}
