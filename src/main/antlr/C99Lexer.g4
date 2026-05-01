lexer grammar C99Lexer;

// Keywords

Int: 'int';
Return: 'return';
Void: 'void';

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

