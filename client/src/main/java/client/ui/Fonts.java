package client.ui;

import javafx.scene.text.Font;

public final class Fonts {
    public static void load() {
        var grotesk = Font.loadFont(Fonts.class.getResourceAsStream("/fonts/SpaceGrotesk-Light.ttf"), 10);
        var mono    = Font.loadFont(Fonts.class.getResourceAsStream("/fonts/SpaceMono-Regular.ttf"), 10);
        System.out.println("grotesk -> family=[" + grotesk.getFamily() + "] name=[" + grotesk.getName() + "]");
        System.out.println("mono    -> family=[" + mono.getFamily() + "] name=[" + mono.getName() + "]");
    }
}
