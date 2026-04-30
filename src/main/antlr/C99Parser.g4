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
    : IntegerConstant                   # IntegerConstantExpression
    | LeftParen expression RightParen   # ParenthesizedExpression
    ;

// ISO 6.5.2, Postfix Operators
postfixExpression
    : primaryExpression
    ;

// ISO 6.5.3, Unary Operators
unaryExpression
    : postfixExpression                 # DummyPostfixExpressionToUnaryExpression
    | unaryOperator castExpression      # UnaryOperatorExpression
    ;

unaryOperator
    : Minus
    | Tilde
    ;

// ISO 6.5.4, Cast Operators
castExpression
    : unaryExpression
    ;

// ISO 6.5.5, Multiplicative Operators
multiplicativeExpression
    : castExpression                                                    # DummyCastExpressionToMultiplicativeExpression
    | multiplicativeExpression multiplicativeOperator castExpression    # MultiplicativeOperatorExpression
    ;

multiplicativeOperator
    : Star
    | Divide
    | Modulo
    ;

// ISO 6.5.6, Additive Operators
additiveExpression
    : multiplicativeExpression                                      # DummyMultiplicativeExpressionToAdditiveExpression
    | additiveExpression additiveOperator multiplicativeExpression  # AdditiveOperatorExpression
    ;

additiveOperator
    : Plus
    | Minus
    ;

// ISO 6.5.7, Bitwise Shift Operators
shiftExpression
    : additiveExpression
    ;

// ISO 6.5.8, Relational Operators
relationalExpression
    : shiftExpression
    ;

// ISO 6.5.9, Equality Operators
equalityExpression
    : relationalExpression
    ;

// ISO 6.5.10, Bitwise AND Operator
andExpression
    : equalityExpression
    ;

// ISO 6.5.11, Bitwise exclusive OR Operator
exclusiveOrExpression
    : andExpression
    ;

// ISO 6.5.12, Bitwise inclusive OR Operator
inclusiveOrExpression
    : exclusiveOrExpression
    ;

// ISO 6.5.13, Logical AND Operator
logicalAndExpression
    : inclusiveOrExpression
    ;

// ISO 6.5.14, Logical OR Operator
logicalOrExpression
    : logicalAndExpression
    ;

// ISO 6.5.15, Conditional Operator
conditionalExpression
    : logicalOrExpression
    ;

// ISO 6.5.16, Assignment Operators
assignmentExpression
    : conditionalExpression
    ;

// ISO 6.5.17, Comma Operator
expression:
    assignmentExpression
    ;

// ISO 6.7, Declarations
declarationSpecifiers
    : typeSpecifier declarationSpecifiers?
    ;

// ISO 6.7.2, Type Specifiers
typeSpecifier
    : Void
    | Int
    ;

// ISO 6.7.5, Declarators
// Rewrote
declarator:
    directDeclarator
    ;
directDeclarator
    : (
        Identifier
        | LeftParen declarator RightParen
    ) (
        LeftParen parameterTypeList RightParen
    )*
    ;
parameterTypeList
    : Void
    ;

// ISO 6.8, Statements and Blocks
statement
    : jumpStatement
    ;

// ISO 6.8.2, Compound Statements
compoundStatement
    : LeftBrace blockItemList? RightBrace
    ;
blockItemList:
    blockItem
    ;
blockItem
    : statement
    ;

// ISO 6.8.6, Jump Statements
jumpStatement
    : Return expression? Semicolon
    ;

// ISO 6.9, External Definitions
translationUnit:
    externalDeclaration
    ;
externalDeclaration
    : functionDefinition
    ;

// ISO 6.9.1, HighLevelFunction Definitions
functionDefinition
    : declarationSpecifiers declarator compoundStatement
    ;