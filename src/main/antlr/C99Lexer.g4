lexer grammar C99Lexer;

@lexer::header {
    import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.C99LexerBase;
}

options {
    superClass = C99LexerBase;
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
Enum: 'enum';
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
Struct: 'struct';
Switch: 'switch';
Union: 'union';
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
Arrow: '->';
Dot: '.';

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

