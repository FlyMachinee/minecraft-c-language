lexer grammar C99Lexer;

@lexer::header {
    import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;
    import net.flymachine.minecraftclanguage.content.logic.compiler.common.util.EscapeUnescapeHelper;
    import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
}

@lexer::members {
    private DiagnosticReporter reporter;
    private EscapeUnescapeHelper escapeUnescapeHelper;

    public void setDiagnosticReporter(DiagnosticReporter diagnosticReporter) {
        this.reporter = diagnosticReporter;
        this.escapeUnescapeHelper = new EscapeUnescapeHelper(diagnosticReporter);
    }

    private void checkStringLiteral(String fullText) {
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

    private void checkCharLiteral(String fullText) {
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

    private void checkFloatingLiteral(String fullText) {
        SourceLocation loc = currentLocation();
        try {
            java.lang.Double.parseDouble(fullText);
        } catch (NumberFormatException e) {
            reporter.error(loc, "malformed floating constant");
        }
    }

    private void checkIntegerLiteral(String fullText) {
        String text = fullText.toLowerCase();
        SourceLocation loc = currentLocation();
        int pos = 0;

        if (text.length() > 2 && text.startsWith("0x")) {
            pos = 2;
            // fragment HexadecimalConstant: HexadecimalPrefix HexadecimalDigit+;
            while (pos < text.length() && isHexadecimalDigit(text.charAt(pos))) {
                pos++;
            }
        } else if (text.length() > 1 && text.startsWith("0")) {
            // fragment OctalConstant: '0' OctalDigit*;
            while (pos < text.length() && text.charAt(pos) >= '0' && text.charAt(pos) <= '7') {
                pos++;
            }
            if (pos < text.length() && (text.charAt(pos) == '8' || text.charAt(pos) == '9')) {
                reporter.error(loc, "invalid octal digit '" + reporter.white(String.valueOf(text.charAt(pos))) +
                                    "' in octal constant");
                return;
            }
        } else {
            // fragment DecimalConstant: NonzeroDigit Digit*;
            while (pos < text.length() && Character.isDigit(text.charAt(pos))) {
                pos++;
            }
        }
        String postfix = text.substring(pos);
        switch (postfix) {
            case "", "u", "l", "ul", "lu" -> { }
            default -> {
                String rawPostfix = fullText.substring(pos);
                reporter.error(loc, "invalid suffix '" + reporter.white(rawPostfix) + "' on integer constant");
            }
        }
    }

    private boolean isHexadecimalDigit(char c) {
        return (c >= '0' && c <= '9') ||
               (c >= 'a' && c <= 'f') ||
               (c >= 'A' && c <= 'F');
    }

    private SourceLocation currentLocation() {
        int start = _tokenStartCharIndex;
        int line = _tokenStartLine;
        int column = _tokenStartCharPositionInLine;
        int length = getText().length();
        int end = start + length - 1;
        return new SourceLocation(line, column, start, end);
    }
}

// Keywords

Break: 'break';
Case: 'case';
Char: 'char';
Const: 'const';
Continue: 'continue';
Default: 'default';
Do: 'do';
Double: 'double';
Else: 'else';
Extern: 'extern';
For: 'for';
Goto: 'goto';
If: 'if';
Int: 'int';
Long: 'long';
Return: 'return';
Signed: 'signed';
Sizeof: 'sizeof';
Static: 'static';
Switch: 'switch';
Unsigned: 'unsigned';
Void: 'void';
While: 'while';

// Identifiers

Identifier: IdentifierNondigit (IdentifierNondigit | Digit)*;

fragment IdentifierNondigit: Nondigit;
fragment Nondigit: [a-zA-Z_];
fragment Digit: [0-9];

// Constants

FloatingConstant
    : (DecimalFloatingConstant | HexadecimalFloatingConstant) { checkFloatingLiteral(getText()); }
    ;

IntegerConstant
    : (DecimalConstant | OctalConstant | HexadecimalConstant) IntegerSuffix? { checkIntegerLiteral(getText()); };

fragment DecimalConstant: NonzeroDigit Digit*;
fragment OctalConstant: '0' OctalDigit*;
fragment HexadecimalConstant: HexadecimalPrefix HexadecimalDigit+;
fragment HexadecimalPrefix: '0' [xX];
fragment NonzeroDigit: [1-9];
fragment OctalDigit: [0-7];
fragment HexadecimalDigit: [0-9a-fA-F];
fragment IntegerSuffix: [a-zA-Z0-9._]+;

fragment DecimalFloatingConstant
    : FractionalConstant ExponentPart?
    | DigitSequence ExponentPart
    ;
fragment HexadecimalFloatingConstant
    : HexadecimalPrefix (HexadecimalFractionalConstant | HexadecimalDigitSequence) BinaryExponentPart
    ;
fragment FractionalConstant
    : DigitSequence? '.' DigitSequence FloatTail?
    | DigitSequence '.' FloatTail?
    ;
fragment ExponentPart: [eE] Sign? FloatTail?;
fragment Sign: [+-];
fragment DigitSequence: Digit+;
fragment HexadecimalFractionalConstant
    : HexadecimalDigitSequence? '.' HexadecimalDigitSequence FloatTail?
    | HexadecimalDigitSequence '.' FloatTail?
    ;
fragment BinaryExponentPart: [pP] Sign? FloatTail?;
fragment HexadecimalDigitSequence: HexadecimalDigit+;
fragment FloatTail: [a-zA-Z0-9._]+;

CharacterConstant: '\'' CChar* '\''? { checkCharLiteral(getText()); };
fragment CChar: ~['\\\r\n] | EscapeSequence | '\\' ~[\r\n];
fragment EscapeSequence
    : SimpleEscapeSequence
    | OctalEscapeSequence
    | HexadecimalEscapeSequence
    ;
fragment SimpleEscapeSequence: '\\' ['"?\\abfnrtv];
fragment OctalEscapeSequence: '\\' OctalDigit OctalDigit? OctalDigit?;
fragment HexadecimalEscapeSequence: '\\x' HexadecimalDigit*;

// String Literals

StringLiteral: '"' SChar* '"'? { checkStringLiteral(getText()); };
fragment SChar: ~["\\\r\n] | EscapeSequence | '\\' ~[\r\n];

// Operators

Less: '<';
LessEqual: '<=';
Greater: '>';
GreaterEqual: '>=';
LeftShift: '<<';
RightShift: '>>';
Plus: '+';
PlusPlus: '++';
Minus: '-';
MinusMinus: '--';
Star: '*';
Divide: '/';
Modulo: '%';
And: '&';
AndAnd: '&&';
Or: '|';
OrOr: '||';
Caret: '^';
Not: '!';
Tilde: '~';
Question: '?';
Colon: ':';
Comma: ',';
Assign: '=';
StarAssign: '*=';
DivideAssign: '/=';
ModuloAssign: '%=';
PlusAssign: '+=';
MinusAssign: '-=';
LeftShiftAssign: '<<=';
RightShiftAssign: '>>=';
AndAssign: '&=';
CaretAssign: '^=';
OrAssign: '|=';
Equal: '==';
NotEqual: '!=';

// Parentheses

LeftParen: '(';
RightParen: ')';
LeftBracket: '[';
RightBracket: ']';
LeftBrace: '{';
RightBrace: '}';

// Punctuators

Semicolon: ';';

// Directives

Directive: '#' ~ [\n]* -> skip;

// Whitespace and Comments

Whitespace: [ \t]+ -> skip;
Newline: ('\r'? '\n') -> skip;
BlockComment: '/*' .*? '*/' -> skip;
LineComment: '//' ~ [\r\n]* -> skip;

