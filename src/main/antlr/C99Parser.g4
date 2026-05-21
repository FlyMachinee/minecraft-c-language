parser grammar C99Parser;

// Based on the C99 standard (ISO/IEC 9899:1999)

options {
    tokenVocab=C99Lexer;
}

// Start Rule
compilationUnit:
    translationUnit? EOF
    ;

// ISO 6.5.1, Primary Expressions
primaryExpression
    : Identifier
    | IntegerConstant
    | LeftParen expression RightParen
    ;

// ISO 6.5.2, Postfix Operators
postfixExpression
    : primaryExpression
    | postfixExpression LeftParen argumentExpressionList? RightParen
    | postfixExpression PlusPlus
    | postfixExpression MinusMinus
    ;
argumentExpressionList
    : assignmentExpression (Comma assignmentExpression)*
    ;

// ISO 6.5.3, Unary Operators
unaryExpression
    : postfixExpression
    | PlusPlus unaryExpression
    | MinusMinus unaryExpression
    | unaryOperator castExpression
    ;

unaryOperator
    : Minus
    | Tilde
    | Not
    ;

// ISO 6.5.4, Cast Operators
castExpression
    : unaryExpression
    | LeftParen typeName RightParen castExpression
    ;

// ISO 6.5.5, Multiplicative Operators
multiplicativeExpression
    : castExpression
    | multiplicativeExpression multiplicativeOperator castExpression
    ;

multiplicativeOperator
    : Star
    | Divide
    | Modulo
    ;

// ISO 6.5.6, Additive Operators
additiveExpression
    : multiplicativeExpression
    | additiveExpression additiveOperator multiplicativeExpression
    ;

additiveOperator
    : Plus
    | Minus
    ;

// ISO 6.5.7, Bitwise Shift Operators
shiftExpression
    : additiveExpression
    | shiftExpression shiftOperator additiveExpression
    ;

shiftOperator
    : LeftShift
    | RightShift
    ;

// ISO 6.5.8, Relational Operators
relationalExpression
    : shiftExpression
    | relationalExpression relationalOperator shiftExpression
    ;

relationalOperator
    : Less
    | Greater
    | LessEqual
    | GreaterEqual
    ;

// ISO 6.5.9, Equality Operators
equalityExpression
    : relationalExpression
    | equalityExpression equalityOperator relationalExpression
    ;

equalityOperator
    : Equal
    | NotEqual
    ;

// ISO 6.5.10, Bitwise AND Operator
andExpression
    : equalityExpression
    | andExpression And equalityExpression
    ;

// ISO 6.5.11, Bitwise exclusive OR Operator
exclusiveOrExpression
    : andExpression
    | exclusiveOrExpression Caret andExpression
    ;

// ISO 6.5.12, Bitwise inclusive OR Operator
inclusiveOrExpression
    : exclusiveOrExpression
    | inclusiveOrExpression Or exclusiveOrExpression
    ;

// ISO 6.5.13, Logical AND Operator
logicalAndExpression
    : inclusiveOrExpression
    | logicalAndExpression AndAnd inclusiveOrExpression
    ;

// ISO 6.5.14, Logical OR Operator
logicalOrExpression
    : logicalAndExpression
    | logicalOrExpression OrOr logicalAndExpression
    ;

// ISO 6.5.15, Conditional Operator
conditionalExpression
    : logicalOrExpression
    | logicalOrExpression Question expression Colon conditionalExpression
    ;

// ISO 6.5.16, Assignment Operators
assignmentExpression
    : conditionalExpression
    | unaryExpression assignmentOperator assignmentExpression
    ;
assignmentOperator
    : Assign
    | StarAssign
    | DivideAssign
    | ModuloAssign
    | PlusAssign
    | MinusAssign
    | LeftShiftAssign
    | RightShiftAssign
    | AndAssign
    | CaretAssign
    | OrAssign
    ;

// ISO 6.5.17, Comma Operator
expression:
    assignmentExpression
    ;

// ISO 6.7, Declarations
declaration
    : declarationSpecifiers initDeclaratorList? Semicolon
    ;
declarationSpecifiers // rewrote
    : declarationSpecifier+
    ;
declarationSpecifier // added
    : storageClassSpecifier
    | typeSpecifier
    ;
initDeclaratorList
    : initDeclarator
    ;
initDeclarator
    : declarator (Assign initializer)?
    ;

// ISO 6.7.1, Storage-Class Specifiers
storageClassSpecifier
    : Static
    | Extern
    ;

// ISO 6.7.2, Type Specifiers
typeSpecifier
    : Void
    | Int
    | Long
    | Signed
    | Unsigned
    ;

// ISO 6.7.2.1, Structure And Union Specifiers
specifierQualifierList // rewrote
    : specifierQualifier+
    ;
specifierQualifier // added
    : typeSpecifier
    ;

// ISO 6.7.5, Declarators
declarator:
    directDeclarator
    ;
directDeclarator
    : Identifier
    | LeftParen declarator RightParen
    | directDeclarator LeftParen parameterTypeList RightParen
    ;
parameterTypeList
    : parameterList
    ;
parameterList
    : parameterDeclaration (Comma parameterDeclaration)*
    ;
parameterDeclaration
    : declarationSpecifiers declarator
    | declarationSpecifiers abstractDeclarator?
    ;

// ISO 6.7.6, Type Names
typeName
    : specifierQualifierList abstractDeclarator?
    ;
abstractDeclarator
    : directAbstractDeclarator
    ;
directAbstractDeclarator
    : LeftParen abstractDeclarator RightParen
    | LeftParen parameterTypeList? RightParen
    | directAbstractDeclarator LeftParen parameterTypeList? RightParen
    ;

// ISO 6.7.8, Initialization
initializer
    : assignmentExpression
    ;

// ISO 6.8, Statements and Blocks
statement
    : labeledStatement
    | compoundStatement
    | expressionStatement
    | selectionStatement
    | iterationStatement
    | jumpStatement
    ;

// ISO 6.8.1, Labeled Statements
labeledStatement
    : Identifier Colon statement
    | Case IntegerConstant Colon statement
    | Default Colon statement
    ;

// ISO 6.8.2, Compound Statements
compoundStatement
    : LeftBrace blockItemList? RightBrace
    ;
blockItemList:
    blockItem+
    ;
blockItem
    : declaration
    | statement
    ;

// ISO 6.8.3, Expression and Null Statements
expressionStatement
    : expression? Semicolon
    ;

// ISO 6.8.4, Selection Statements
selectionStatement
    : If LeftParen expression RightParen statement (Else statement)?
    | Switch LeftParen expression RightParen statement
    ;

// ISO 6.8.5, Iteration Statements
iterationStatement
    : While LeftParen expression RightParen statement
    | Do statement While LeftParen expression RightParen Semicolon
    | For LeftParen init=expression? Semicolon cond=expression? Semicolon step=expression? RightParen statement
    | For LeftParen declaration cond=expression? Semicolon step=expression? RightParen statement
    ;

// ISO 6.8.6, Jump Statements
jumpStatement
    : Goto Identifier Semicolon
    | Continue Semicolon
    | Break Semicolon
    | Return expression Semicolon
    ;

// ISO 6.9, External Definitions
translationUnit
    : externalDeclaration+
    ;
externalDeclaration
    : functionDefinition
    | declaration
    ;

// ISO 6.9.1, Function Definitions
functionDefinition
    : declarationSpecifiers declarator compoundStatement
    ;