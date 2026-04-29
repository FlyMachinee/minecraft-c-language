package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logger.Logger;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99Lexer;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.ErrorHandleUtil;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.Interval;

public class LexerErrorListener extends BaseErrorListener {

    private Logger logger;
    private boolean hasErrors = false;

    public LexerErrorListener() {
        this.logger = new ConsoleLogger();
    }

    public LexerErrorListener(Logger logger) {
        this.logger = logger;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    @Override
    public void syntaxError(
        Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
        String msg, RecognitionException e) {

        C99Lexer lexer = (C99Lexer) recognizer;
        String sourceName = lexer.getInputStream().getSourceName();
        String text = lexer.getInputStream().getText(Interval.of(
            lexer._tokenStartCharIndex,
            lexer.getInputStream().index()));
        String errorToken = lexer.getErrorDisplay(text);
        int tokenLength = text.equals("<EOF>") ? 0 : text.length();

        logger.logLine(
            logger.formatWithColor(sourceName + ":" + line + ":" + charPositionInLine + ": ", Logger.Color.WHITE) +
            logger.formatWithColor("error: ", Logger.Color.RED) +
            "unrecognized token '" + logger.formatWithColor(errorToken, Logger.Color.WHITE) + "', ignored");

        CharStream stream = lexer.getInputStream();
        ErrorHandleUtil.logSourceLineWithColor(logger, stream, line, charPositionInLine, tokenLength, Logger.Color.RED);
        hasErrors = true;
    }
}
