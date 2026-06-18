package client.ui;

import javafx.scene.text.Font;

public final class Fonts {
    public static void load() {
        Font.loadFont(Fonts.class.getResourceAsStream("/fonts/SpaceGrotesk-Light.ttf"), 10);
        Font.loadFont(Fonts.class.getResourceAsStream("/fonts/SpaceMono-Regular.ttf"), 10);
    }
}
