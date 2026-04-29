package net.flymachine.minecraftclanguage.content.logger;

public interface Logger {
    void log(String message);

    void logLine(String message);

    String formatWithColor(String text, Color color);

    enum Color {
        RESET, BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE,
        LIGHT_BLACK, LIGHT_RED, LIGHT_GREEN, LIGHT_YELLOW, LIGHT_BLUE, LIGHT_MAGENTA, LIGHT_CYAN, LIGHT_WHITE
    }
}