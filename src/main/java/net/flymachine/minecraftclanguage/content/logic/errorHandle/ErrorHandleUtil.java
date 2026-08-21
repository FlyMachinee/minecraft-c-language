package net.flymachine.minecraftclanguage.content.logic.errorHandle;

import net.flymachine.minecraftclanguage.content.logger.Logger;

public final class ErrorHandleUtil {

    private ErrorHandleUtil() { }

    public static void logErrorWithSourceLine(
        Logger logger, SourceFile file, SourceLocation location, String message) {
        logErrorWithSourceLine(logger, file, location.line(), location.column(), location.length(), message);
    }

    public static void logNoteWithSourceLine(
        Logger logger, SourceFile file, SourceLocation location, String message) {
        logNoteWithSourceLine(logger, file, location.line(), location.column(), location.length(), message);
    }

    public static void logWarningWithSourceLine(
        Logger logger, SourceFile file, SourceLocation location, String message) {
        logWarningWithSourceLine(logger, file, location.line(), location.column(), location.length(), message);
    }

    public static void logErrorWithSourceLine(
        Logger logger, SourceFile file, int line, int column, int len, String message) {

        String location = file.getFileName() + ":" + line + ":" + (column + 1) + ": ";
        logger.logLine(
            logger.formatWithColor(location, Logger.Color.WHITE) +
            logger.formatWithColor("error: ", Logger.Color.RED) + message);
        logSourceLineWithColor(logger, file, line, column, len, Logger.Color.RED);
    }

    public static void logNoteWithSourceLine(
        Logger logger, SourceFile file, int line, int column, int len, String message) {

        String location = file.getFileName() + ":" + line + ":" + (column + 1) + ": ";
        logger.logLine(
            logger.formatWithColor(location, Logger.Color.WHITE) +
            logger.formatWithColor("note: ", Logger.Color.CYAN) + message);
        logSourceLineWithColor(logger, file, line, column, len, Logger.Color.CYAN);
    }

    public static void logWarningWithSourceLine(
        Logger logger, SourceFile file, int line, int column, int len, String message) {

        String location = file.getFileName() + ":" + line + ":" + (column + 1) + ": ";
        logger.logLine(
            logger.formatWithColor(location, Logger.Color.WHITE) +
            logger.formatWithColor("warning: ", Logger.Color.MAGENTA) + message);
        logSourceLineWithColor(logger, file, line, column, len, Logger.Color.MAGENTA);
    }

    public static void logSourceLineWithColor(
        Logger logger, SourceFile file, int line, int column, int len, Logger.Color color) {

        String sourceLine = file.getLine(line);
        if (sourceLine == null) {
            return;
        }

        column = Math.min(column, sourceLine.length());
        len = Math.min(len, sourceLine.length() - column);

        sourceLine = sourceLine.substring(0, column) +
                     logger.formatWithColor(sourceLine.substring(column, column + len), color) +
                     sourceLine.substring(column + len);

        logger.logLine(String.format("%5d | %s", line, sourceLine));
        logger.logLine("      | " +
                       " ".repeat(column) +
                       logger.formatWithColor("^" + "~".repeat(Math.max(0, len - 1)), color));
    }
}
