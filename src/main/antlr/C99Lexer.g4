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
Return: 'return';
Static: 'static';
Switch: 'switch';
Void: 'void';
While: 'while';

// Identifiers

Identifier: IdentifierNondigit (IdentifierNondigit | Digit)*;

fragment IdentifierNondigit: Nondigit;
fragment Nondigit: [a-zA-Z_];
fragment Digit: [0-9];

// Constants

IntegerConstant:
    DecimalConstant
    | '0';

fragment DecimalConstant: NonzeroDigit Digit*;

fragment NonzeroDigit: [1-9];

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

