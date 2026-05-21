lexer grammar C99Lexer;

// Keywords

Break: 'break';
Case: 'case';
Continue: 'continue';
Default: 'default';
Do: 'do';
Else: 'else';
Extern: 'extern';
For: 'for';
Goto: 'goto';
If: 'if';
Int: 'int';
Long: 'long';
Return: 'return';
Signed: 'signed';
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

IntegerConstant
    : (DecimalConstant | OctalConstant | HexadecimalConstant) IntegerSuffix?;

fragment DecimalConstant: NonzeroDigit Digit*;
fragment OctalConstant: '0' OctalDegit*;
fragment HexadecimalConstant: HexadecimalPrefix HexadecimalDigit+;
fragment HexadecimalPrefix: '0' [xX];
fragment NonzeroDigit: [1-9];
fragment OctalDegit: [0-7];
fragment HexadecimalDigit: [0-9a-fA-F];
fragment IntegerSuffix
    : UnsignedSuffix LongSuffix?
    | LongSuffix UnsignedSuffix?
    ;
fragment UnsignedSuffix: 'u' | 'U';
fragment LongSuffix: 'l' | 'L';

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

