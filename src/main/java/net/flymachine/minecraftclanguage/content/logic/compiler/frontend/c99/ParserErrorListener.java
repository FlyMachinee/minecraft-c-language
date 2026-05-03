package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logger.Logger;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99Parser;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.ErrorHandleUtil;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceFile;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.IntervalSet;

public class ParserErrorListener extends BaseErrorListener {

    private Logger logger;
    private SourceFile sourceFile;

    public ParserErrorListener() {
        this.logger = new ConsoleLogger();
    }

    public ParserErrorListener(Logger logger) {
        this.logger = logger;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public SourceFile getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
    }

    @Override
    public void syntaxError(
        Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
        int charPositionInLine, String msg, RecognitionException e) {

        C99Parser parser = (C99Parser) recognizer;
        TokenStream stream = parser.getInputStream();

        Token offendingToken = (Token) offendingSymbol;
        String errorToken = offendingToken.getText();
        if (errorToken == null) {
            errorToken = "<EOF>";
        }
        int tokenLength = errorToken.equals("<EOF>") ? 0 : errorToken.length();

        String previousTokenText;
        int idx = offendingToken.getTokenIndex();
        if (idx > 0) {
            Token prev = stream.get(idx - 1);
            previousTokenText = prev.getText();
        } else {
            previousTokenText = "start of input";
        }

        String expectingList;
        IntervalSet expectedTokens;
        if (e != null) {
            expectedTokens = e.getExpectedTokens();
        } else {
            expectedTokens = parser.getExpectedTokens();
        }
        if (expectedTokens != null && !expectedTokens.isNil()) {
            expectingList = expectedTokens.toString(parser.getVocabulary());
        } else {
            expectingList = "nothing";
        }

        String errorInfo =
            "unexpected '" + logger.formatWithColor(errorToken, Logger.Color.WHITE) +
            "' after '" + logger.formatWithColor(previousTokenText, Logger.Color.WHITE) +
            "', expecting " + expectingList;
        ErrorHandleUtil.logErrorWithSourceLine(logger, sourceFile, line, charPositionInLine, tokenLength, errorInfo);
    }
}
