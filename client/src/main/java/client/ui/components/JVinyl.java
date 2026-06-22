package client.ui.components;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class JVinyl extends StackPane {

    private static final double SIZE = 300;
    private static final double CENTER = SIZE / 2;
    private static final double COVER = 140;

    public JVinyl(String albumImageUrl) {
        var discBase = new Region();
        discBase.getStyleClass().add("vinyl");

        var grooves = new Canvas(SIZE, SIZE);
        GraphicsContext g = grooves.getGraphicsContext2D();
        g.setLineWidth(1);
        g.setStroke(Color.web("white", 0.045));
        for (double r = CENTER; r > 70; r -= 4) {
            g.strokeOval(CENTER - r, CENTER - r, r * 2, r * 2);
        }

        var cover = new StackPane();
        if (albumImageUrl != null && !albumImageUrl.isBlank()) {
            var image = new Image(albumImageUrl, COVER, COVER, false, true, true);
            var view = new ImageView(image);
            view.setFitWidth(COVER);
            view.setFitHeight(COVER);
            view.setClip(new Circle(COVER / 2, COVER / 2, COVER / 2));
            view.setEffect(new InnerShadow(18, Color.web("black", 0.5)));
            cover.getChildren().add(view);
        }

        var spinningGroup = new StackPane(discBase, grooves, cover);

        var spin = new RotateTransition(Duration.seconds(9), spinningGroup);
        spin.setByAngle(360);
        spin.setInterpolator(Interpolator.LINEAR);
        spin.setCycleCount(Animation.INDEFINITE);
        spin.play();

        var sheen = new Region();
        sheen.getStyleClass().add("vinyl-sheen");
        sheen.setMouseTransparent(true);

        var spindle = new Circle(6.5, Color.web("#08040c"));

        getChildren().addAll(spinningGroup, sheen, spindle);
        setMinSize(SIZE, SIZE);
        setMaxSize(SIZE, SIZE);
    }
}
