package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;

public interface AstVisitor<T> {
    T visit(ProgramNode node);

    T visit(FunctionDefinitionNode node);

    T visit(ReturnNode node);

    T visit(IntConstantNode node);

    T visit(UnaryExpressionNode node);

    T visit(BinaryExpressionNode node);

    T visit(DeclarationNode node);

    T visit(NullStatementNode node);

    T visit(VariableNode node);

    T visit(AssignmentNode node);
}
