package client.ui.components;

import javafx.scene.control.Button;

public class JButton extends Button {
    public enum Variant { PRIMARY, SECONDARY, DANGER }

    public JButton(String text, Variant variant, Runnable onClick) {
        super(text);
        getStyleClass().addAll("btn", switch (variant) {
            case PRIMARY   -> "btn-primary";
            case SECONDARY -> "btn-secondary";
            case DANGER    -> "btn-danger";
        });
        setOnAction(e -> onClick.run());
    }
}
