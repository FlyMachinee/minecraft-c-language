package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99Parser;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99ParserBaseVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;

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
        StatementNode body = visitCompoundStatement(ctx.compoundStatement());
        return new FunctionDefinitionNode(name, body);
    }

    @Override
    public StatementNode visitCompoundStatement(C99Parser.CompoundStatementContext ctx) {
        return (StatementNode) visit(ctx.blockItemList());
    }

    @Override
    public ReturnNode visitJumpStatement(C99Parser.JumpStatementContext ctx) {
        ExpressionNode expression = (ExpressionNode) visit(ctx.expression());
        return new ReturnNode(expression);
    }

    @Override
    public ExpressionNode visitPrimaryExpression(C99Parser.PrimaryExpressionContext ctx) {
        int value = Integer.parseInt(ctx.IntegerConstant().getText());
        return new IntConstantNode(value);
    }
}
