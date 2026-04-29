package net.flymachine.minecraftclanguage.content.logic.errorHandle;

import net.flymachine.minecraftclanguage.content.logger.Logger;
import org.antlr.v4.runtime.CharStream;

public final class ErrorHandleUtil {

    private ErrorHandleUtil() { }

    public static String getSourceLine(CharStream stream, int line) {
        String fullText = stream.toString();
        String[] lines = fullText.split("\n", -1);
        if (line >= 1 && line <= lines.length) {
            return lines[line - 1].replace("\r", "");
        }
        return "";
    }

    public static void logSourceLineWithColor(
        Logger logger, CharStream stream, int line, int position, int len,
        Logger.Color color) {
        String sourceLine = ErrorHandleUtil.getSourceLine(stream, line);
        sourceLine = sourceLine.substring(0, position) +
                     logger.formatWithColor(sourceLine.substring(position, position + len), color) +
                     sourceLine.substring(position + len);
        logger.logLine(String.format("%5d | %s", line, sourceLine));
        logger.logLine("      | " +
                       " ".repeat(position) +
                       logger.formatWithColor("^" + "~".repeat(Math.max(0, len - 1)), color));
    }
}
