package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99Parser;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.IntervalSet;

public final class ParserErrorListener extends BaseErrorListener {

    private final DiagnosticReporter reporter;

    public ParserErrorListener(DiagnosticReporter reporter) {
        this.reporter = reporter;
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
            "unexpected '" + reporter.white(errorToken) +
            "' after '" + reporter.white(previousTokenText) +
            "', expecting " + expectingList;
        reporter.error(line, charPositionInLine, tokenLength, errorInfo);
    }
}
