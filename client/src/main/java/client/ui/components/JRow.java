package client.ui.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class JRow extends HBox {

    private static final String SELECTED = "row--selected";

    private final Label check = new Label("✓");

    public JRow(Node leading, String title, String meta) {
        getStyleClass().add("row");
        setAlignment(Pos.CENTER_LEFT);

        var titleLabel = new Label(title);
        titleLabel.getStyleClass().add("row__title");
        var metaLabel = new Label(meta);
        metaLabel.getStyleClass().add("row__meta");
        var text = new VBox(4, titleLabel, metaLabel);

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        check.getStyleClass().add("row__check");
        check.setVisible(false);

        getChildren().addAll(leading, text, spacer, check);
    }

    public void setSelected(boolean selected) {
        check.setVisible(selected);
        getStyleClass().remove(SELECTED);
        if (selected) {
            getStyleClass().add(SELECTED);
        }
    }
}
