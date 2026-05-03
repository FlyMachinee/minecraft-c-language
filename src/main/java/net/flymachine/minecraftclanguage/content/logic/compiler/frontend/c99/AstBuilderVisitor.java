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
        List<BlockItemNode> body = new ArrayList<>();
        var blockItemList = ctx.compoundStatement().blockItemList();
        if (blockItemList != null) {
            for (var blockItem : blockItemList.blockItem()) {
                BlockItemNode item = (BlockItemNode) visit(blockItem);
                if (item != null) {
                    body.add(item);
                }
            }
        }
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
    public BlockItemNode visitExpressionStatement(C99Parser.ExpressionStatementContext ctx) {
        if (ctx.expression() != null) {
            return new ExpressionStatementNode((ExpressionNode) visit(ctx.expression()));
        } else {
            return new NullStatementNode(getSourceLocation(ctx.Semicolon()));
        }
    }

    @Override
    public ReturnNode visitJumpStatement(C99Parser.JumpStatementContext ctx) {
        ExpressionNode expression = (ExpressionNode) visit(ctx.expression());
        return new ReturnNode(getSourceLocation(ctx.Return()), expression);
    }

    @Override
    public IdentifierNode visitIdentifierExpression(C99Parser.IdentifierExpressionContext ctx) {
        String identifier = ctx.Identifier().getText();
        return new IdentifierNode(getSourceLocation(ctx.Identifier()), identifier);
    }

    @Override
    public IntConstantNode visitIntegerConstantExpression(C99Parser.IntegerConstantExpressionContext ctx) {
        int value = Integer.parseInt(ctx.IntegerConstant().getText());
        return new IntConstantNode(getSourceLocation(ctx.IntegerConstant()), value);
    }

    @Override
    public ExpressionNode visitParenthesizedExpression(C99Parser.ParenthesizedExpressionContext ctx) {
        return (ExpressionNode) visit(ctx.expression());
    }

    @Override
    public ExpressionNode visitPostfixIncrementOperatorExpression(C99Parser.PostfixIncrementOperatorExpressionContext ctx) {
        ExpressionNode operand = (ExpressionNode) visit(ctx.postfixExpression());
        return new IncrementDecrementNode(getSourceLocation(ctx.PlusPlus()), true, false, operand);
    }

    @Override
    public ExpressionNode visitPostfixDecrementOperatorExpression(C99Parser.PostfixDecrementOperatorExpressionContext ctx) {
        ExpressionNode operand = (ExpressionNode) visit(ctx.postfixExpression());
        return new IncrementDecrementNode(getSourceLocation(ctx.MinusMinus()), false, false, operand);
    }

    @Override
    public ExpressionNode visitPrefixIncrementOperatorExpression(C99Parser.PrefixIncrementOperatorExpressionContext ctx) {
        ExpressionNode operand = (ExpressionNode) visit(ctx.unaryExpression());
        return new IncrementDecrementNode(getSourceLocation(ctx.PlusPlus()), true, true, operand);
    }

    @Override
    public ExpressionNode visitPrefixDecrementOperatorExpression(C99Parser.PrefixDecrementOperatorExpressionContext ctx) {
        ExpressionNode operand = (ExpressionNode) visit(ctx.unaryExpression());
        return new IncrementDecrementNode(getSourceLocation(ctx.MinusMinus()), false, true, operand);
    }

    @Override
    public UnaryExpressionNode visitUnaryOperatorExpression(C99Parser.UnaryOperatorExpressionContext ctx) {
        UnaryOperatorNode op = visitUnaryOperator(ctx.unaryOperator());
        ExpressionNode operand = (ExpressionNode) visit(ctx.castExpression());
        return new UnaryExpressionNode(op, operand);
    }

    @Override
    public UnaryOperatorNode visitUnaryOperator(C99Parser.UnaryOperatorContext ctx) {
        String operator = ctx.getText();
        UnaryOperator op = UnaryOperator.fromSymbol(operator);
        return new UnaryOperatorNode(getSourceLocation(ctx), op);
    }

    @Override
    public BinaryExpressionNode visitMultiplicativeOperatorExpression(C99Parser.MultiplicativeOperatorExpressionContext ctx) {
        BinaryOperatorNode op = visitMultiplicativeOperator(ctx.multiplicativeOperator());
        ExpressionNode lhs = (ExpressionNode) visit(ctx.multiplicativeExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.castExpression());
        return new BinaryExpressionNode(op, lhs, rhs);
    }

    @Override
    public BinaryOperatorNode visitMultiplicativeOperator(C99Parser.MultiplicativeOperatorContext ctx) {
        String operator = ctx.getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        return new BinaryOperatorNode(getSourceLocation(ctx), op);
    }

    @Override
    public BinaryExpressionNode visitAdditiveOperatorExpression(C99Parser.AdditiveOperatorExpressionContext ctx) {
        BinaryOperatorNode op = visitAdditiveOperator(ctx.additiveOperator());
        ExpressionNode lhs = (ExpressionNode) visit(ctx.additiveExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.multiplicativeExpression());
        return new BinaryExpressionNode(op, lhs, rhs);
    }

    @Override
    public BinaryOperatorNode visitAdditiveOperator(C99Parser.AdditiveOperatorContext ctx) {
        String operator = ctx.getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        return new BinaryOperatorNode(getSourceLocation(ctx), op);
    }

    @Override
    public BinaryExpressionNode visitShiftOperatorExpression(C99Parser.ShiftOperatorExpressionContext ctx) {
        BinaryOperatorNode op = visitShiftOperator(ctx.shiftOperator());
        ExpressionNode lhs = (ExpressionNode) visit(ctx.shiftExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.additiveExpression());
        return new BinaryExpressionNode(op, lhs, rhs);
    }

    @Override
    public BinaryOperatorNode visitShiftOperator(C99Parser.ShiftOperatorContext ctx) {
        String operator = ctx.getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        return new BinaryOperatorNode(getSourceLocation(ctx), op);
    }

    @Override
    public BinaryExpressionNode visitBitwiseAndOperatorExpression(C99Parser.BitwiseAndOperatorExpressionContext ctx) {
        ExpressionNode lhs = (ExpressionNode) visit(ctx.andExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.equalityExpression());
        return new BinaryExpressionNode(
            new BinaryOperatorNode(
                getSourceLocation(ctx.andExpression()),
                BinaryOperator.BITWISE_AND), lhs, rhs);
    }

    @Override
    public BinaryExpressionNode visitBitwiseExclusiveOrOperatorExpression(C99Parser.BitwiseExclusiveOrOperatorExpressionContext ctx) {
        ExpressionNode lhs = (ExpressionNode) visit(ctx.exclusiveOrExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.andExpression());
        return new BinaryExpressionNode(
            new BinaryOperatorNode(
                getSourceLocation(ctx.exclusiveOrExpression()),
                BinaryOperator.BITWISE_XOR), lhs, rhs);
    }

    @Override
    public BinaryExpressionNode visitBitwiseInclusiveOrOperatorExpression(C99Parser.BitwiseInclusiveOrOperatorExpressionContext ctx) {
        ExpressionNode lhs = (ExpressionNode) visit(ctx.inclusiveOrExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.exclusiveOrExpression());
        return new BinaryExpressionNode(
            new BinaryOperatorNode(
                getSourceLocation(ctx.inclusiveOrExpression()),
                BinaryOperator.BITWISE_OR), lhs, rhs);
    }

    @Override
    public BinaryExpressionNode visitRelationalOperatorExpression(C99Parser.RelationalOperatorExpressionContext ctx) {
        BinaryOperatorNode op = visitRelationalOperator(ctx.relationalOperator());
        ExpressionNode lhs = (ExpressionNode) visit(ctx.relationalExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.shiftExpression());
        return new BinaryExpressionNode(op, lhs, rhs);
    }

    @Override
    public BinaryOperatorNode visitRelationalOperator(C99Parser.RelationalOperatorContext ctx) {
        String operator = ctx.getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        return new BinaryOperatorNode(getSourceLocation(ctx), op);
    }

    @Override
    public BinaryExpressionNode visitEqualityOperatorExpression(C99Parser.EqualityOperatorExpressionContext ctx) {
        BinaryOperatorNode op = visitEqualityOperator(ctx.equalityOperator());
        ExpressionNode lhs = (ExpressionNode) visit(ctx.equalityExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.relationalExpression());
        return new BinaryExpressionNode(op, lhs, rhs);
    }

    @Override
    public BinaryOperatorNode visitEqualityOperator(C99Parser.EqualityOperatorContext ctx) {
        String operator = ctx.getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        return new BinaryOperatorNode(getSourceLocation(ctx), op);
    }

    @Override
    public BinaryExpressionNode visitLogicalAndOperatorExpression(C99Parser.LogicalAndOperatorExpressionContext ctx) {
        ExpressionNode lhs = (ExpressionNode) visit(ctx.logicalAndExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.inclusiveOrExpression());
        return new BinaryExpressionNode(
            new BinaryOperatorNode(
                getSourceLocation(ctx.logicalAndExpression()),
                BinaryOperator.LOGICAL_AND), lhs, rhs);
    }

    @Override
    public BinaryExpressionNode visitLogicalOrOperatorExpression(C99Parser.LogicalOrOperatorExpressionContext ctx) {
        ExpressionNode lhs = (ExpressionNode) visit(ctx.logicalOrExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.logicalAndExpression());
        return new BinaryExpressionNode(
            new BinaryOperatorNode(
                getSourceLocation(ctx.logicalOrExpression()),
                BinaryOperator.LOGICAL_OR), lhs, rhs);
    }

    @Override
    public AssignmentNode visitAssignmentOperatorExpression(C99Parser.AssignmentOperatorExpressionContext ctx) {
        AssignmentOperatorNode op = visitAssignmentOperator(ctx.assignmentOperator());
        ExpressionNode lhs = (ExpressionNode) visit(ctx.unaryExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.assignmentExpression());
        return new AssignmentNode(op, lhs, rhs);
    }

    @Override
    public AssignmentOperatorNode visitAssignmentOperator(C99Parser.AssignmentOperatorContext ctx) {
        String operator = ctx.getText();
        AssignmentOperator op = AssignmentOperator.fromSymbol(operator);
        return new AssignmentOperatorNode(getSourceLocation(ctx), op);
    }
}
