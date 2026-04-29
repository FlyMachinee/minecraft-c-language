package net.flymachine.minecraftclanguage.content.logger;

public class ConsoleLogger implements Logger {

    @Override
    public void log(String message) {
        System.out.print(message);
    }

    @Override
    public void logLine(String message) {
        System.out.println(message);
    }

    @Override
    public String formatWithColor(String text, Color color) {
        String colorCode = switch (color) {
            case RESET -> "\u001B[0m";
            case BLACK -> "\u001B[30m";
            case RED -> "\u001B[31m";
            case GREEN -> "\u001B[32m";
            case YELLOW -> "\u001B[33m";
            case BLUE -> "\u001B[34m";
            case MAGENTA -> "\u001B[35m";
            case CYAN -> "\u001B[36m";
            case WHITE -> "\u001B[37m";
            case LIGHT_BLACK -> "\u001B[90m";
            case LIGHT_RED -> "\u001B[91m";
            case LIGHT_GREEN -> "\u001B[92m";
            case LIGHT_YELLOW -> "\u001B[93m";
            case LIGHT_BLUE -> "\u001B[94m";
            case LIGHT_MAGENTA -> "\u001B[95m";
            case LIGHT_CYAN -> "\u001B[96m";
            case LIGHT_WHITE -> "\u001B[97m";
        };
        return colorCode + text + "\u001B[0m";
    }
}
