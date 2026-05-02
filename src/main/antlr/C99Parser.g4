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
    : Identifier                        # IdentifierExpression
    | IntegerConstant                   # IntegerConstantExpression
    | LeftParen expression RightParen   # ParenthesizedExpression
    ;

// ISO 6.5.2, Postfix Operators
postfixExpression
    : primaryExpression             # DummyPrimaryExpressionToPostfixExpression
    | postfixExpression PlusPlus    # PostfixIncrementOperatorExpression
    | postfixExpression MinusMinus  # PostfixDecrementOperatorExpression
    ;

// ISO 6.5.3, Unary Operators
unaryExpression
    : postfixExpression                 # DummyPostfixExpressionToUnaryExpression
    | PlusPlus unaryExpression          # PrefixIncrementOperatorExpression
    | MinusMinus unaryExpression        # PrefixDecrementOperatorExpression
    | unaryOperator castExpression      # UnaryOperatorExpression
    ;

unaryOperator
    : Minus
    | Tilde
    | Not
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
    : additiveExpression                                # DummyAdditiveExpressionToShiftExpression
    | shiftExpression shiftOperator additiveExpression  # ShiftOperatorExpression
    ;

shiftOperator
    : LeftShift
    | RightShift
    ;

// ISO 6.5.8, Relational Operators
relationalExpression
    : shiftExpression                                           # DummyShiftExpressionToRelationalExpression
    | relationalExpression relationalOperator shiftExpression   # RelationalOperatorExpression
    ;

relationalOperator
    : Less
    | Greater
    | LessEqual
    | GreaterEqual
    ;

// ISO 6.5.9, Equality Operators
equalityExpression
    : relationalExpression                                      # DummyRelationalExpressionToEqualityExpression
    | equalityExpression equalityOperator relationalExpression  # EqualityOperatorExpression
    ;

equalityOperator
    : Equal
    | NotEqual
    ;

// ISO 6.5.10, Bitwise AND Operator
andExpression
    : equalityExpression                    # DummyEqualityExpressionToAndExpression
    | andExpression And equalityExpression  # BitwiseAndOperatorExpression
    ;

// ISO 6.5.11, Bitwise exclusive OR Operator
exclusiveOrExpression
    : andExpression                             # DummyAndExpressionToExclusiveOrExpression
    | exclusiveOrExpression Caret andExpression # BitwiseExclusiveOrOperatorExpression
    ;

// ISO 6.5.12, Bitwise inclusive OR Operator
inclusiveOrExpression
    : exclusiveOrExpression                             # DummyExclusiveOrExpressionToInclusiveOrExpression
    | inclusiveOrExpression Or exclusiveOrExpression    # BitwiseInclusiveOrOperatorExpression
    ;

// ISO 6.5.13, Logical AND Operator
logicalAndExpression
    : inclusiveOrExpression                              # DummyInclusiveOrExpressionToLogicalAndExpression
    | logicalAndExpression AndAnd inclusiveOrExpression  # LogicalAndOperatorExpression
    ;

// ISO 6.5.14, Logical OR Operator
logicalOrExpression
    : logicalAndExpression                          # DummyLogicalAndExpressionToLogicalOrExpression
    | logicalOrExpression OrOr logicalAndExpression # LogicalOrOperatorExpression
    ;

// ISO 6.5.15, Conditional Operator
conditionalExpression
    : logicalOrExpression
    ;

// ISO 6.5.16, Assignment Operators
assignmentExpression
    : conditionalExpression                                     # DummyConditionalExpressionToAssignmentExpression
    | unaryExpression assignmentOperator assignmentExpression   # AssignmentOperatorExpression
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
declarationSpecifiers
    : typeSpecifier declarationSpecifiers?
    ;
initDeclaratorList
    : initDeclarator
    ;
initDeclarator
    : declarator (Assign initializer)?
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

// ISO 6.7.8, Initialization
initializer
    : assignmentExpression
    ;

// ISO 6.8, Statements and Blocks
statement
    : expressionStatement
    | jumpStatement
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

// ISO 6.8.6, Jump Statements
jumpStatement
    : Return expression Semicolon
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