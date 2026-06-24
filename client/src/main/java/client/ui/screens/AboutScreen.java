package client.ui.screens;

import client.ui.AppContext;
import client.ui.components.JButton;
import client.ui.components.JLabel;
import client.ui.components.JStepper;
import client.ui.components.JSystemMessage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class AboutScreen extends BorderPane {

    private static final double COLUMN_WIDTH = 380;

    public AboutScreen(AppContext ctx) {
        var back = new JLabel("‹  BACK", JLabel.Type.NAV);
        back.setCursor(Cursor.HAND);
        back.setOnMouseClicked(e -> ctx.show(new MainMenuScreen(ctx)));

        var top = new BorderPane();
        top.setLeft(back);
        BorderPane.setAlignment(back, Pos.TOP_LEFT);

        var section = new JLabel("ABOUT", JLabel.Type.SECTION);
        var aboutText = new JLabel("""
                jammatch - is a multiplayer game in which players connect their YouTube playlists.
                each round a random track plays and players have to guess which participant's playlist it belongs to
                to connect your playlist without errors, make sure to paste the correct link
                if an error occurs, try to check if your playlist is public or allows access to it by link
                otherwise, we will not be able to connect it :(
                
                remember, that what you have in your playlist is limited by your imagination only!
                you can guess each others hear-me-outs, breakup songs or something you would never listen to
                the only rule we have - no russian artists!
                
                was created as a part of Development of Client-Server Applications course in NaUKMA, teacher: Kurpiak O.M.
                find the repo here: https://github.com/viktoriyabaran/jammatch
                """, JLabel.Type.BODY);

        setTop(top);
        var column = new VBox(section, aboutText);
        column.setAlignment(Pos.CENTER_LEFT);
        column.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        setCenter(column);
        BorderPane.setAlignment(column, Pos.CENTER);
        setPadding(new Insets(36, 80, 36, 80));
        getStyleClass().add("bg-center");
    }
}
