package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;

public interface AstVisitor<T> {
    T visit(ProgramNode node);

    T visit(FunctionDefinitionNode node);

    T visit(ReturnNode node);

    T visit(UnaryExpressionNode node);

    T visit(BinaryExpressionNode node);

    T visit(DeclarationNode node);

    T visit(ExpressionStatementNode node);

    T visit(NullStatementNode node);

    T visit(VariableNode node);

    T visit(AssignmentNode node);

    T visit(IncrementDecrementNode node);

    T visit(IfStatementNode node);

    T visit(ConditionalExpressionNode node);

    T visit(GotoNode node);

    T visit(CompoundStatementNode node);

    T visit(BreakNode node);

    T visit(ContinueNode node);

    T visit(WhileLoopNode node);

    T visit(ForLoopNode node);

    T visit(SwitchStatementNode node);

    T visit(FunctionCallNode node);

    T visit(ConstantNode node);

    T visit(CastExpressionNode node);

    T visit(AddressOfNode node);

    T visit(DereferenceNode node);
}
