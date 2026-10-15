package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.util.EscapeUnescapeHelper;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;

public abstract class C99LexerBase extends Lexer {
    private DiagnosticReporter reporter;
    private EscapeUnescapeHelper escapeUnescapeHelper;

    protected C99LexerBase(CharStream input) {
        super(input);
    }

    public void setDiagnosticReporter(DiagnosticReporter diagnosticReporter) {
        this.reporter = diagnosticReporter;
        this.escapeUnescapeHelper = new EscapeUnescapeHelper(diagnosticReporter);
    }

    protected void checkStringLiteral(String fullText) {
        SourceLocation loc = currentLocation();

        if (fullText.length() < 2) {
            reporter.error(loc, "malformed string literal");
            return;
        }
        if (fullText.charAt(fullText.length() - 1) != '"') {
            reporter.error(loc, "missing terminating \" character");
            return;
        }

        String content = fullText.substring(1, fullText.length() - 1);
        try {
            escapeUnescapeHelper.unescapeBytes(content);
        } catch (IllegalArgumentException e) {
            reporter.error(loc, e.getMessage());
        }
    }

    /**
     * 供 CharacterConstant 规则调用。
     */
    protected void checkCharLiteral(String fullText) {
        SourceLocation loc = currentLocation();

        if (fullText.charAt(fullText.length() - 1) != '\'') {
            reporter.error(loc, "missing terminating ' character");
            return;
        }
        if (fullText.length() < 3) {
            reporter.error(loc, "empty character constant");
            return;
        }

        String content = fullText.substring(1, fullText.length() - 1);
        try {
            escapeUnescapeHelper.evalChar(content);
        } catch (IllegalArgumentException e) {
            reporter.error(loc, e.getMessage());
        }
    }

    /**
     * 供 FloatingConstant 规则调用。
     */
    protected void checkFloatingLiteral(String fullText) {
        SourceLocation loc = currentLocation();
        try {
            Double.parseDouble(fullText);
        } catch (NumberFormatException e) {
            reporter.error(loc, "malformed floating constant");
        }
    }

    /**
     * 供 IntegerConstant 规则调用。
     */
    protected void checkIntegerLiteral(String fullText) {
        String text = fullText.toLowerCase();
        SourceLocation loc = currentLocation();
        int pos = 0;

        if (text.length() > 2 && text.startsWith("0x")) {
            pos = 2;
            while (pos < text.length() && isHexadecimalDigit(text.charAt(pos))) {
                pos++;
            }
        } else if (text.length() > 1 && text.startsWith("0")) {
            while (pos < text.length() && text.charAt(pos) >= '0' && text.charAt(pos) <= '7') {
                pos++;
            }
            if (pos < text.length() && (text.charAt(pos) == '8' || text.charAt(pos) == '9')) {
                reporter.error(loc, "invalid octal digit '" +
                                    reporter.white(String.valueOf(text.charAt(pos))) +
                                    "' in octal constant");
                return;
            }
        } else {
            while (pos < text.length() && Character.isDigit(text.charAt(pos))) {
                pos++;
            }
        }

        String postfix = text.substring(pos);
        switch (postfix) {
            case "", "u", "l", "ul", "lu" -> { }
            default -> {
                String rawPostfix = fullText.substring(pos);
                reporter.error(loc, "invalid suffix '" + reporter.white(rawPostfix) +
                                    "' on integer constant");
            }
        }
    }

    private boolean isHexadecimalDigit(char c) {
        return (c >= '0' && c <= '9') ||
               (c >= 'a' && c <= 'f') ||
               (c >= 'A' && c <= 'F');
    }

    /**
     * 当前 token 在源文件中的位置。
     */
    protected SourceLocation currentLocation() {
        int start = _tokenStartCharIndex;
        int line = _tokenStartLine;
        int column = _tokenStartCharPositionInLine;
        int length = getText().length();
        int end = start + length - 1;
        return new SourceLocation(line, column, start, end);
    }
}
