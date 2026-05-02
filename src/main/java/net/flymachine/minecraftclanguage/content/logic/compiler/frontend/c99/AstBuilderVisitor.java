package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99Parser;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99ParserBaseVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 将生成的语法树转换为自定义 AST
 */
public final class AstBuilderVisitor extends C99ParserBaseVisitor<AstNode> {

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
        return new FunctionDefinitionNode(name, body);
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
        var initializer = ctx.initializer();
        if (initializer != null) {
            ExpressionNode initValue = (ExpressionNode) visit(initializer);
            return new DeclarationNode(name, initValue);
        } else {
            return new DeclarationNode(name, null);
        }
    }

    @Override
    public BlockItemNode visitExpressionStatement(C99Parser.ExpressionStatementContext ctx) {
        if (ctx.expression() != null) {
            return (ExpressionNode) visit(ctx.expression());
        } else {
            return new NullStatementNode();
        }
    }

    @Override
    public ReturnNode visitJumpStatement(C99Parser.JumpStatementContext ctx) {
        ExpressionNode expression = (ExpressionNode) visit(ctx.expression());
        return new ReturnNode(expression);
    }

    @Override
    public VariableNode visitIdentifierExpression(C99Parser.IdentifierExpressionContext ctx) {
        String identifier = ctx.Identifier().getText();
        return new VariableNode(identifier);
    }

    @Override
    public IntConstantNode visitIntegerConstantExpression(C99Parser.IntegerConstantExpressionContext ctx) {
        int value = Integer.parseInt(ctx.IntegerConstant().getText());
        return new IntConstantNode(value);
    }

    @Override
    public ExpressionNode visitParenthesizedExpression(C99Parser.ParenthesizedExpressionContext ctx) {
        return (ExpressionNode) visit(ctx.expression());
    }

    @Override
    public UnaryExpressionNode visitUnaryOperatorExpression(C99Parser.UnaryOperatorExpressionContext ctx) {
        String operator = ctx.unaryOperator().getText();
        UnaryOperator op = UnaryOperator.fromSymbol(operator);
        ExpressionNode operand = (ExpressionNode) visit(ctx.castExpression());
        return new UnaryExpressionNode(op, operand);
    }

    @Override
    public BinaryExpressionNode visitMultiplicativeOperatorExpression(C99Parser.MultiplicativeOperatorExpressionContext ctx) {
        String operator = ctx.multiplicativeOperator().getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        ExpressionNode lhs = (ExpressionNode) visit(ctx.multiplicativeExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.castExpression());
        return new BinaryExpressionNode(op, lhs, rhs);
    }

    @Override
    public BinaryExpressionNode visitAdditiveOperatorExpression(C99Parser.AdditiveOperatorExpressionContext ctx) {
        String operator = ctx.additiveOperator().getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        ExpressionNode lhs = (ExpressionNode) visit(ctx.additiveExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.multiplicativeExpression());
        return new BinaryExpressionNode(op, lhs, rhs);
    }

    @Override
    public BinaryExpressionNode visitShiftOperatorExpression(C99Parser.ShiftOperatorExpressionContext ctx) {
        String operator = ctx.shiftOperator().getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        ExpressionNode lhs = (ExpressionNode) visit(ctx.shiftExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.additiveExpression());
        return new BinaryExpressionNode(op, lhs, rhs);
    }

    @Override
    public BinaryExpressionNode visitBitwiseAndOperatorExpression(C99Parser.BitwiseAndOperatorExpressionContext ctx) {
        ExpressionNode lhs = (ExpressionNode) visit(ctx.andExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.equalityExpression());
        return new BinaryExpressionNode(BinaryOperator.BITWISE_AND, lhs, rhs);
    }

    @Override
    public BinaryExpressionNode visitBitwiseExclusiveOrOperatorExpression(C99Parser.BitwiseExclusiveOrOperatorExpressionContext ctx) {
        ExpressionNode lhs = (ExpressionNode) visit(ctx.exclusiveOrExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.andExpression());
        return new BinaryExpressionNode(BinaryOperator.BITWISE_XOR, lhs, rhs);
    }

    @Override
    public BinaryExpressionNode visitBitwiseInclusiveOrOperatorExpression(C99Parser.BitwiseInclusiveOrOperatorExpressionContext ctx) {
        ExpressionNode lhs = (ExpressionNode) visit(ctx.inclusiveOrExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.exclusiveOrExpression());
        return new BinaryExpressionNode(BinaryOperator.BITWISE_OR, lhs, rhs);
    }

    @Override
    public BinaryExpressionNode visitRelationalOperatorExpression(C99Parser.RelationalOperatorExpressionContext ctx) {
        String operator = ctx.relationalOperator().getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        ExpressionNode lhs = (ExpressionNode) visit(ctx.relationalExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.shiftExpression());
        return new BinaryExpressionNode(op, lhs, rhs);
    }

    @Override
    public BinaryExpressionNode visitEqualityOperatorExpression(C99Parser.EqualityOperatorExpressionContext ctx) {
        String operator = ctx.equalityOperator().getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        ExpressionNode lhs = (ExpressionNode) visit(ctx.equalityExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.relationalExpression());
        return new BinaryExpressionNode(op, lhs, rhs);
    }

    @Override
    public BinaryExpressionNode visitLogicalAndOperatorExpression(C99Parser.LogicalAndOperatorExpressionContext ctx) {
        ExpressionNode lhs = (ExpressionNode) visit(ctx.logicalAndExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.inclusiveOrExpression());
        return new BinaryExpressionNode(BinaryOperator.LOGICAL_AND, lhs, rhs);
    }

    @Override
    public BinaryExpressionNode visitLogicalOrOperatorExpression(C99Parser.LogicalOrOperatorExpressionContext ctx) {
        ExpressionNode lhs = (ExpressionNode) visit(ctx.logicalOrExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.logicalAndExpression());
        return new BinaryExpressionNode(BinaryOperator.LOGICAL_OR, lhs, rhs);
    }

    @Override
    public AssignmentNode visitAssignmentOperatorExpression(C99Parser.AssignmentOperatorExpressionContext ctx) {
        ExpressionNode lhs = (ExpressionNode) visit(ctx.unaryExpression());
        ExpressionNode rhs = (ExpressionNode) visit(ctx.assignmentExpression());
        return new AssignmentNode(lhs, rhs);
    }
}
