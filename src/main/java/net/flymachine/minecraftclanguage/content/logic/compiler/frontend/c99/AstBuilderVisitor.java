package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.AssignmentOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99Parser;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99ParserBaseVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 将生成的语法树转换为自定义 AST
 */
public final class AstBuilderVisitor extends C99ParserBaseVisitor<AstNode> {

    private static SourceLocation getSourceLocation(ParserRuleContext ctx) {
        Token startToken = ctx.getStart();
        Token endToken = ctx.getStop();
        int line = startToken.getLine();
        int column = startToken.getCharPositionInLine();
        return new SourceLocation(line, column, startToken.getStartIndex(), endToken.getStopIndex());
    }

    private static SourceLocation getSourceLocation(TerminalNode terminalNode) {
        Token token = terminalNode.getSymbol();
        int line = token.getLine();
        int column = token.getCharPositionInLine();
        return new SourceLocation(line, column, token.getStartIndex(), token.getStopIndex());
    }

    @Override
    public ProgramNode visitCompilationUnit(C99Parser.CompilationUnitContext ctx) {
        return visitTranslationUnit(ctx.translationUnit());
    }

    @Override
    public ProgramNode visitTranslationUnit(C99Parser.TranslationUnitContext ctx) {
        FunctionDefinitionNode functionDefinition = (FunctionDefinitionNode) visit(ctx.externalDeclaration());
        return new ProgramNode(functionDefinition);
    }

    @Override
    public FunctionDefinitionNode visitFunctionDefinition(C99Parser.FunctionDefinitionContext ctx) {
        String name = ctx.declarator().directDeclarator().Identifier().getText();
        SourceLocation nameLocation = getSourceLocation(ctx.declarator().directDeclarator().Identifier());
        CompoundStatementNode body = (CompoundStatementNode) visit(ctx.compoundStatement());
        return new FunctionDefinitionNode(new IdentifierNode(nameLocation, name), body);
    }

    @Override
    public DeclarationNode visitDeclaration(C99Parser.DeclarationContext ctx) {
        var list = ctx.initDeclaratorList();
        if (list != null) {
            return (DeclarationNode) visit(list);
        } else {
            return null;
        }
    }

    @Override
    public DeclarationNode visitInitDeclarator(C99Parser.InitDeclaratorContext ctx) {
        String name = ctx.declarator().directDeclarator().Identifier().getText();
        SourceLocation nameLocation = getSourceLocation(ctx.declarator().directDeclarator().Identifier());
        var initializer = ctx.initializer();
        if (initializer != null) {
            ExpressionNode initValue = (ExpressionNode) visit(initializer);
            return new DeclarationNode(new IdentifierNode(nameLocation, name), initValue);
        } else {
            return new DeclarationNode(new IdentifierNode(nameLocation, name));
        }
    }

    @Override
    public StatementNode visitLabeledStatement(C99Parser.LabeledStatementContext ctx) {
        StatementNode statement = (StatementNode) visit(ctx.statement());
        if (ctx.Identifier() != null) {
            String label = ctx.Identifier().getText();
            SourceLocation labelLocation = getSourceLocation(ctx.Identifier());
            statement.gotoLabels.add(new StatementNode.GotoLabelInfo(new IdentifierNode(labelLocation, label)));
            return statement;
        } else {
            throw new IllegalStateException("Unknown labeled statement");
        }
    }

    @Override
    public CompoundStatementNode visitCompoundStatement(C99Parser.CompoundStatementContext ctx) {
        List<BlockItemNode> blockItems = new ArrayList<>();
        if (ctx.blockItemList() != null) {
            for (C99Parser.BlockItemContext blockItemCtx : ctx.blockItemList().blockItem()) {
                BlockItemNode blockItem = (BlockItemNode) visit(blockItemCtx);
                blockItems.add(blockItem);
            }
        }
        return new CompoundStatementNode(
            SourceLocation.concat(
                getSourceLocation(ctx.LeftBrace()),
                getSourceLocation(ctx.RightBrace())), blockItems);
    }

    @Override
    public StatementNode visitExpressionStatement(C99Parser.ExpressionStatementContext ctx) {
        if (ctx.expression() != null) {
            return new ExpressionStatementNode((ExpressionNode) visit(ctx.expression()));
        } else {
            return new NullStatementNode(getSourceLocation(ctx.Semicolon()));
        }
    }

    @Override
    public StatementNode visitSelectionStatement(C99Parser.SelectionStatementContext ctx) {
        if (ctx.If() != null) {
            ExpressionNode cond = (ExpressionNode) visit(ctx.expression());
            StatementNode thenStmt = (StatementNode) visit(ctx.statement(0));
            if (ctx.Else() != null) {
                StatementNode elseStmt = (StatementNode) visit(ctx.statement(1));
                return new IfStatementNode(cond, thenStmt, elseStmt);
            } else {
                return new IfStatementNode(cond, thenStmt);
            }
        }
        return null;
    }

    @Override
    public StatementNode visitJumpStatement(C99Parser.JumpStatementContext ctx) {
        if (ctx.Goto() != null) {
            String identifier = ctx.Identifier().getText();
            SourceLocation identifierLocation = getSourceLocation(ctx.Identifier());
            return new GotoNode(getSourceLocation(ctx.Goto()), new IdentifierNode(identifierLocation, identifier));
        } else if (ctx.Return() != null) {
            ExpressionNode expression = (ExpressionNode) visit(ctx.expression());
            return new ReturnNode(getSourceLocation(ctx.Return()), expression);
        } else {
            throw new IllegalStateException("Unknown jump statement");
        }
    }

    @Override
    public ExpressionNode visitPrimaryExpression(C99Parser.PrimaryExpressionContext ctx) {
        if (ctx.Identifier() != null) {
            String identifier = ctx.Identifier().getText();
            return new IdentifierNode(getSourceLocation(ctx.Identifier()), identifier);
        } else if (ctx.IntegerConstant() != null) {
            int value = Integer.parseInt(ctx.IntegerConstant().getText());
            return new IntConstantNode(getSourceLocation(ctx.IntegerConstant()), value);
        } else if (ctx.LeftParen() != null) {
            return (ExpressionNode) visit(ctx.expression());
        } else {
            throw new IllegalStateException("Unknown primary expression");
        }
    }

    @Override
    public ExpressionNode visitPostfixExpression(C99Parser.PostfixExpressionContext ctx) {
        if (ctx.primaryExpression() != null) {
            return (ExpressionNode) visit(ctx.primaryExpression());
        } else if (ctx.PlusPlus() != null) {
            ExpressionNode operand = (ExpressionNode) visit(ctx.postfixExpression());
            return new IncrementDecrementNode(getSourceLocation(ctx.PlusPlus()), true, false, operand);
        } else if (ctx.MinusMinus() != null) {
            ExpressionNode operand = (ExpressionNode) visit(ctx.postfixExpression());
            return new IncrementDecrementNode(getSourceLocation(ctx.MinusMinus()), false, false, operand);
        } else {
            throw new IllegalStateException("Unknown postfix expression");
        }
    }

    @Override
    public ExpressionNode visitUnaryExpression(C99Parser.UnaryExpressionContext ctx) {
        if (ctx.postfixExpression() != null) {
            return (ExpressionNode) visit(ctx.postfixExpression());
        } else if (ctx.PlusPlus() != null) {
            ExpressionNode operand = (ExpressionNode) visit(ctx.unaryExpression());
            return new IncrementDecrementNode(getSourceLocation(ctx.PlusPlus()), true, true, operand);
        } else if (ctx.MinusMinus() != null) {
            ExpressionNode operand = (ExpressionNode) visit(ctx.unaryExpression());
            return new IncrementDecrementNode(getSourceLocation(ctx.MinusMinus()), false, true, operand);
        } else if (ctx.unaryOperator() != null) {
            UnaryOperatorNode operator = (UnaryOperatorNode) visit(ctx.unaryOperator());
            ExpressionNode operand = (ExpressionNode) visit(ctx.castExpression());
            return new UnaryExpressionNode(operator, operand);
        } else {
            throw new IllegalStateException("Unknown unary operator");
        }
    }

    @Override
    public UnaryOperatorNode visitUnaryOperator(C99Parser.UnaryOperatorContext ctx) {
        String operator = ctx.getText();
        UnaryOperator op = UnaryOperator.fromSymbol(operator);
        return new UnaryOperatorNode(getSourceLocation(ctx), op);
    }

    @Override
    public ExpressionNode visitMultiplicativeExpression(C99Parser.MultiplicativeExpressionContext ctx) {
        if (ctx.multiplicativeOperator() != null) {
            BinaryOperatorNode op = visitMultiplicativeOperator(ctx.multiplicativeOperator());
            ExpressionNode lhs = (ExpressionNode) visit(ctx.multiplicativeExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.castExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.castExpression() != null) {
            return (ExpressionNode) visit(ctx.castExpression());
        } else {
            throw new IllegalStateException("Unknown multiplicative expression");
        }
    }

    @Override
    public BinaryOperatorNode visitMultiplicativeOperator(C99Parser.MultiplicativeOperatorContext ctx) {
        String operator = ctx.getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        return new BinaryOperatorNode(getSourceLocation(ctx), op);
    }

    @Override
    public ExpressionNode visitAdditiveExpression(C99Parser.AdditiveExpressionContext ctx) {
        if (ctx.additiveOperator() != null) {
            BinaryOperatorNode op = visitAdditiveOperator(ctx.additiveOperator());
            ExpressionNode lhs = (ExpressionNode) visit(ctx.additiveExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.multiplicativeExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.multiplicativeExpression() != null) {
            return (ExpressionNode) visit(ctx.multiplicativeExpression());
        } else {
            throw new IllegalStateException("Unknown additive expression");
        }
    }

    @Override
    public BinaryOperatorNode visitAdditiveOperator(C99Parser.AdditiveOperatorContext ctx) {
        String operator = ctx.getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        return new BinaryOperatorNode(getSourceLocation(ctx), op);
    }

    @Override
    public ExpressionNode visitShiftExpression(C99Parser.ShiftExpressionContext ctx) {
        if (ctx.shiftOperator() != null) {
            BinaryOperatorNode op = visitShiftOperator(ctx.shiftOperator());
            ExpressionNode lhs = (ExpressionNode) visit(ctx.shiftExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.additiveExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.additiveExpression() != null) {
            return (ExpressionNode) visit(ctx.additiveExpression());
        } else {
            throw new IllegalStateException("Unknown shift expression");
        }
    }

    @Override
    public BinaryOperatorNode visitShiftOperator(C99Parser.ShiftOperatorContext ctx) {
        String operator = ctx.getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        return new BinaryOperatorNode(getSourceLocation(ctx), op);
    }

    @Override
    public ExpressionNode visitRelationalExpression(C99Parser.RelationalExpressionContext ctx) {
        if (ctx.relationalOperator() != null) {
            BinaryOperatorNode op = visitRelationalOperator(ctx.relationalOperator());
            ExpressionNode lhs = (ExpressionNode) visit(ctx.relationalExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.shiftExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.shiftExpression() != null) {
            return (ExpressionNode) visit(ctx.shiftExpression());
        } else {
            throw new IllegalStateException("Unknown relational expression");
        }
    }

    @Override
    public BinaryOperatorNode visitRelationalOperator(C99Parser.RelationalOperatorContext ctx) {
        String operator = ctx.getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        return new BinaryOperatorNode(getSourceLocation(ctx), op);
    }

    @Override
    public ExpressionNode visitEqualityExpression(C99Parser.EqualityExpressionContext ctx) {
        if (ctx.equalityOperator() != null) {
            BinaryOperatorNode op = visitEqualityOperator(ctx.equalityOperator());
            ExpressionNode lhs = (ExpressionNode) visit(ctx.equalityExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.relationalExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.relationalExpression() != null) {
            return (ExpressionNode) visit(ctx.relationalExpression());
        } else {
            throw new IllegalStateException("Unknown equality expression");
        }
    }

    @Override
    public BinaryOperatorNode visitEqualityOperator(C99Parser.EqualityOperatorContext ctx) {
        String operator = ctx.getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        return new BinaryOperatorNode(getSourceLocation(ctx), op);
    }

    @Override
    public ExpressionNode visitAndExpression(C99Parser.AndExpressionContext ctx) {
        if (ctx.And() != null) {
            BinaryOperatorNode op = new BinaryOperatorNode(getSourceLocation(ctx.And()), BinaryOperator.BITWISE_AND);
            ExpressionNode lhs = (ExpressionNode) visit(ctx.andExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.equalityExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.equalityExpression() != null) {
            return (ExpressionNode) visit(ctx.equalityExpression());
        } else {
            throw new IllegalStateException("Unknown and expression");
        }
    }

    @Override
    public ExpressionNode visitExclusiveOrExpression(C99Parser.ExclusiveOrExpressionContext ctx) {
        if (ctx.Caret() != null) {
            BinaryOperatorNode op = new BinaryOperatorNode(getSourceLocation(ctx.Caret()), BinaryOperator.BITWISE_XOR);
            ExpressionNode lhs = (ExpressionNode) visit(ctx.exclusiveOrExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.andExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.andExpression() != null) {
            return (ExpressionNode) visit(ctx.andExpression());
        } else {
            throw new IllegalStateException("Unknown exclusive or expression");
        }
    }

    @Override
    public ExpressionNode visitInclusiveOrExpression(C99Parser.InclusiveOrExpressionContext ctx) {
        if (ctx.Or() != null) {
            BinaryOperatorNode op = new BinaryOperatorNode(getSourceLocation(ctx.Or()), BinaryOperator.BITWISE_OR);
            ExpressionNode lhs = (ExpressionNode) visit(ctx.inclusiveOrExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.exclusiveOrExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.exclusiveOrExpression() != null) {
            return (ExpressionNode) visit(ctx.exclusiveOrExpression());
        } else {
            throw new IllegalStateException("Unknown inclusive or expression");
        }
    }

    @Override
    public ExpressionNode visitLogicalAndExpression(C99Parser.LogicalAndExpressionContext ctx) {
        if (ctx.AndAnd() != null) {
            BinaryOperatorNode op = new BinaryOperatorNode(getSourceLocation(ctx.AndAnd()), BinaryOperator.LOGICAL_AND);
            ExpressionNode lhs = (ExpressionNode) visit(ctx.logicalAndExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.inclusiveOrExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.inclusiveOrExpression() != null) {
            return (ExpressionNode) visit(ctx.inclusiveOrExpression());
        } else {
            throw new IllegalStateException("Unknown logical and expression");
        }
    }

    @Override
    public ExpressionNode visitLogicalOrExpression(C99Parser.LogicalOrExpressionContext ctx) {
        if (ctx.OrOr() != null) {
            BinaryOperatorNode op = new BinaryOperatorNode(getSourceLocation(ctx.OrOr()), BinaryOperator.LOGICAL_OR);
            ExpressionNode lhs = (ExpressionNode) visit(ctx.logicalOrExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.logicalAndExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.logicalAndExpression() != null) {
            return (ExpressionNode) visit(ctx.logicalAndExpression());
        } else {
            throw new IllegalStateException("Unknown logical or expression");
        }
    }

    @Override
    public ExpressionNode visitConditionalExpression(C99Parser.ConditionalExpressionContext ctx) {
        if (ctx.Question() != null) {
            ExpressionNode cond = (ExpressionNode) visit(ctx.logicalOrExpression());
            ExpressionNode thenExpr = (ExpressionNode) visit(ctx.expression());
            ExpressionNode elseExpr = (ExpressionNode) visit(ctx.conditionalExpression());
            return new ConditionalExpressionNode(cond, thenExpr, elseExpr);
        } else if (ctx.logicalOrExpression() != null) {
            return (ExpressionNode) visit(ctx.logicalOrExpression());
        } else {
            throw new IllegalStateException("Unknown conditional expression");
        }
    }

    @Override
    public ExpressionNode visitAssignmentExpression(C99Parser.AssignmentExpressionContext ctx) {
        if (ctx.conditionalExpression() != null) {
            return (ExpressionNode) visit(ctx.conditionalExpression());
        } else if (ctx.assignmentOperator() != null) {
            AssignmentOperatorNode op = visitAssignmentOperator(ctx.assignmentOperator());
            ExpressionNode lhs = (ExpressionNode) visit(ctx.unaryExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.assignmentExpression());
            return new AssignmentNode(op, lhs, rhs);
        } else {
            throw new IllegalStateException("Unknown assignment expression");
        }
    }

    @Override
    public AssignmentOperatorNode visitAssignmentOperator(C99Parser.AssignmentOperatorContext ctx) {
        String operator = ctx.getText();
        AssignmentOperator op = AssignmentOperator.fromSymbol(operator);
        return new AssignmentOperatorNode(getSourceLocation(ctx), op);
    }
}
