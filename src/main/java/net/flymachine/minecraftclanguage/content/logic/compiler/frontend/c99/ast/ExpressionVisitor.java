package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacValue;

/**
 * 对表达式进行求值，可能会因为求值而生成求值过程的三地址码，返回表达式的值
 */
public interface ExpressionVisitor {

    TacValue visit(ConstantNode constant);

    TacValue visit(UnaryExpressionNode unaryExp);

    TacValue visit(BinaryExpressionNode binaryExp);

    TacValue visit(AssignmentNode assignment);

    TacValue visit(VariableNode variable);

    TacValue visit(IncrementDecrementNode incrementDecrement);

    TacValue visit(ConditionalExpressionNode condExp);

    TacValue visit(FunctionCallNode funcCall);

    TacValue visit(CastExpressionNode castExp);
}
