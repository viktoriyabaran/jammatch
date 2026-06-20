package client.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class JButton extends Button {

    public enum Variant { PRIMARY, SECONDARY, GHOST }

    public JButton(String text, Variant variant, Runnable onClick) {
        this(text, variant, true, onClick);
    }

    public JButton(String text, Variant variant, boolean chevron, Runnable onClick) {
        getStyleClass().addAll("btn", switch (variant) {
            case PRIMARY   -> "btn-primary";
            case SECONDARY -> "btn-secondary";
            case GHOST     -> "btn-ghost";
        });

        var label = new Label(text.toUpperCase());
        label.getStyleClass().add("btn-text");

        var content = new HBox(label);

        if (chevron && variant != Variant.GHOST) {
            content.setAlignment(Pos.CENTER_LEFT);
            var spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            var chevronLabel = new Label("›");
            chevronLabel.getStyleClass().add("chev");
            content.getChildren().addAll(spacer, chevronLabel);
            content.prefWidthProperty().bind(widthProperty().subtract(48));
            setMaxWidth(Double.MAX_VALUE);
        } else {
            content.setAlignment(Pos.CENTER);
        }

        setGraphic(content);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setOnAction(e -> onClick.run());
    }
}
