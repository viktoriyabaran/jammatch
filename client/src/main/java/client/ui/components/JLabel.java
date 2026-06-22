package client.ui.components;

import javafx.scene.control.Label;

public class JLabel extends Label {

    public enum Type {
        SECTION("section"),
        SUBTITLE("subtitle"),
        NAV("nav"),
        SYSTEM("system-message"),
        FIELD("field"),
        META("meta"),
        BODY("body"),
        VALUE("value"),
        DATA("data"),
        CODE("code"),
        WORDMARK("wordmark"),
        DISPLAY("display"),
        HEADLINE("headline"),
        TITLE("title"),
        NUMERAL("numeral");

        private final String styleClass;

        Type(String styleClass) {
            this.styleClass = styleClass;
        }
    }

    public JLabel(String text, Type type) {
        super(text);
        getStyleClass().add(type.styleClass);
    }
}
