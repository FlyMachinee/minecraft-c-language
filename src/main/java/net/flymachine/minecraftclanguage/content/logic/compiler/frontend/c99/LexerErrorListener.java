package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logger.Logger;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99Lexer;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.ErrorHandleUtil;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceFile;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.Interval;

public final class LexerErrorListener extends BaseErrorListener {

    private final DiagnosticReporter reporter;

    public LexerErrorListener(DiagnosticReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void syntaxError(
        Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
        String msg, RecognitionException e) {

        C99Lexer lexer = (C99Lexer) recognizer;
        String text = lexer.getInputStream().getText(Interval.of(
            lexer._tokenStartCharIndex,
            lexer.getInputStream().index()));
        String errorToken = lexer.getErrorDisplay(text);
        int tokenLength = text.equals("<EOF>") ? 0 : text.length();

        String message = "unrecognized token '" + reporter.white(errorToken) + "', ignored";
        reporter.error(line, charPositionInLine, tokenLength, message);
    }
}
