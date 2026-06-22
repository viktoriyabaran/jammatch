package client.ui.components;

import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.TextInputControl;

import java.util.ArrayList;
import java.util.List;

public class JSystemMessage extends JLabel {

    private static final String ERROR = "system-message--error";
    private static final String SUCCESS = "system-message--success";

    private final List<Node> errorTargets = new ArrayList<>();

    public JSystemMessage() {
        super("", Type.SYSTEM);
        setWrapText(true);
    }

    public JSystemMessage markOnError(Node... targets) {
        for (Node target : targets) {
            errorTargets.add(target);
        }
        return this;
    }

    public void info(String message) {
        set(message, null, false);
    }

    public void success(String message) {
        set(message, SUCCESS, false);
    }

    public void error(String message) {
        set(message, ERROR, true);
    }

    public void clear() {
        set("", null, false);
    }

    private void set(String message, String modifier, boolean isError) {
        getStyleClass().removeAll(ERROR, SUCCESS);
        if (modifier != null) {
            getStyleClass().add(modifier);
        }
        setText(message == null ? "" : message.toUpperCase());

        for (Node target : errorTargets) {
            String errorClass = errorClassFor(target);
            target.getStyleClass().remove(errorClass);
            if (isError) {
                target.getStyleClass().add(errorClass);
            }
        }
    }

    private static String errorClassFor(Node target) {
        if (target instanceof TextInputControl) {
            return "input--error";
        }
        if (target instanceof ButtonBase) {
            return "btn--error";
        }
        return "error";
    }
}
