package org.hugebrain16;

import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import io.github.kamilszewc.javaansitextcolorizer.Colorizer.BackgroundColor;
import io.github.kamilszewc.javaansitextcolorizer.Colorizer.Color;

public class Util {
    public static final HashMap<String, BackgroundColor> BackgroundColors = new HashMap<>();
    public static final Random random = new Random();

    static {
        BackgroundColors.put("green", BackgroundColor.GREEN);
        BackgroundColors.put("yellow", BackgroundColor.YELLOW);
        BackgroundColors.put("cyan", BackgroundColor.CYAN);
        BackgroundColors.put("magenta", BackgroundColor.MAGENTA);
        BackgroundColors.put("red", BackgroundColor.RED);
        BackgroundColors.put("blue", BackgroundColor.BLUE);
        BackgroundColors.put("white", BackgroundColor.WHITE);
        BackgroundColors.put("black", BackgroundColor.BLACK);
    }

    public static BackgroundColor randomBackgroundColor() {
        return new Vector<>(BackgroundColors.values()).get(random.nextInt(BackgroundColors.size()));
    };

    public static String paint(String string, BackgroundColor color) {
        String result = color.toString();
        switch (color) {
            case WHITE:
                return result += (Color.BLACK + string) + Color.RESET;
            case BLACK:
                return result += (Color.WHITE + string) + Color.RESET;
            default:
                return result += string + Color.RESET;
        }
    }
}
