package io.github.kamilszewc.javaansitextcolorizer;



/**
 * Class used to define mechanism of coloring text in terminal/console using ANSI characters
 */
public class Colorizer {

    static boolean isSystemSupported = true;
    static {
        final String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            if (!osName.endsWith("11")) isSystemSupported = false;
        }
    }

    /**
     * Ansi colors of text
     */
    public enum Color {
        RESET("\033[0m"),

        BLACK("\033[0;30m"),
        RED("\033[0;31m"),
        GREEN("\033[0;32m"),
        YELLOW("\033[0;33m"),
        BLUE("\033[0;34m"),
        MAGENTA("\033[0;35m"),
        CYAN("\033[0;36m"),
        WHITE("\033[0;37m"),

        BLACK_BOLD("\033[1;30m"),
        RED_BOLD("\033[1;31m"),
        GREEN_BOLD("\033[1;32m"),
        YELLOW_BOLD("\033[1;33m"),
        BLUE_BOLD("\033[1;34m"),
        MAGENTA_BOLD("\033[1;35m"),
        CYAN_BOLD("\033[1;36m"),
        WHITE_BOLD("\033[1;37m"),

        BLACK_BRIGHT("\033[0;90m"),
        RED_BRIGHT("\033[0;91m"),
        GREEN_BRIGHT("\033[0;92m"),
        YELLOW_BRIGHT("\033[0;93m"),
        BLUE_BRIGHT("\033[0;94m"),
        MAGENTA_BRIGHT("\033[0;95m"),
        CYAN_BRIGHT("\033[0;96m"),
        WHITE_BRIGHT("\033[0;97m"),

        BLACK_BOLD_BRIGHT("\033[1;90m"),
        RED_BOLD_BRIGHT("\033[1;91m"),
        GREEN_BOLD_BRIGHT("\033[1;92m"),
        YELLOW_BOLD_BRIGHT("\033[1;93m"),
        BLUE_BOLD_BRIGHT("\033[1;94m"),
        MAGENTA_BOLD_BRIGHT("\033[1;95m"),
        CYAN_BOLD_BRIGHT("\033[1;96m"),
        WHITE_BOLD_BRIGHT("\033[1;97m"),

        BLACK_UNDERLINED("\033[4;30m"),
        RED_UNDERLINED("\033[4;31m"),
        GREEN_UNDERLINED("\033[4;32m"),
        YELLOW_UNDERLINED("\033[4;33m"),
        BLUE_UNDERLINED("\033[4;34m"),
        MAGENTA_UNDERLINED("\033[4;35m"),
        CYAN_UNDERLINED("\033[4;36m"),
        WHITE_UNDERLINED("\033[4;37m");

        private final String color;

        Color(String color) {
            this.color = color;
        }

        @Override
        public String toString() {
            return color;
        }
    }

    /**
     * Ansi colors of background
     */
    public enum BackgroundColor {
        RESET("\033[0m"),

        BLACK("\033[0;40m"),
        RED("\033[0;41m"),
        GREEN("\033[0;42m"),
        YELLOW("\033[0;43m"),
        BLUE("\033[0;44m"),
        MAGENTA("\033[0;45m"),
        CYAN("\033[0;46m"),
        WHITE("\033[0;47m"),

        BACK_BRIGHT("\033[0;100m"),
        RED_BRIGHT("\033[0;101m"),
        GREEN_BRIGHT("\033[0;102m"),
        YELLOW_BRIGHT("\033[0;103m"),
        BLUE_BRIGHT("\033[0;104m"),
        MAGENTA_BRIGHT("\033[0;105m"),
        CYAN_BRIGHT("\033[0;106m"),
        WHITE_BRIGHT("\033[0;107m");

        private final String backgroundColor;

        BackgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor;
        }

        @Override
        public String toString() {
            return backgroundColor;
        }
    }

    /**
     * Default constructor
     */
    private Colorizer() {
    }

    /**
     * Paints text with color
     * @param text Text
     * @param color Selected text color
     * @return Colored text
     */
    public static String color(String text, Color color) {
        if (isSystemSupported) {
            return color.toString() + text + Color.RESET;
        } else {
            return text;
        }
    }

    /**
     * Paints text of string
     * @param text Text
     * @param backgroundColor Selected background color
     * @return Colored text
     */
    public static String color(String text, BackgroundColor backgroundColor) {
        if (isSystemSupported) {
            return backgroundColor.toString() + text + Color.RESET;
        } else {
            return text;
        }
    }
}