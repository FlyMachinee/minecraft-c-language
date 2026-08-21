package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;

public interface ExpressionVisitor<T> {

    T visit(ConstantNode constant);

    T visit(UnaryExpressionNode unaryExp);

    T visit(BinaryExpressionNode binaryExp);

    T visit(AssignmentNode assignment);

    T visit(VariableNode variable);

    T visit(IncrementDecrementNode incrementDecrement);

    T visit(ConditionalExpressionNode condExp);

    T visit(FunctionCallNode funcCall);

    T visit(CastExpressionNode castExp);

    T visit(AddressOfNode addrOf);

    T visit(DereferenceNode deref);

}
