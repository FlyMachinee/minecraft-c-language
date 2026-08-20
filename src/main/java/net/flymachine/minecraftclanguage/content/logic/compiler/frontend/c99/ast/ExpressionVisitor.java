package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacConstant;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacValue;

/**
 * 对表达式进行求值，可能会因为求值而生成求值过程的三地址码，返回表达式的值
 */
public interface ExpressionVisitor {

    ExpEvalResult visit(ConstantNode constant);

    ExpEvalResult visit(UnaryExpressionNode unaryExp);

    ExpEvalResult visit(BinaryExpressionNode binaryExp);

    ExpEvalResult visit(AssignmentNode assignment);

    ExpEvalResult visit(VariableNode variable);

    ExpEvalResult visit(IncrementDecrementNode incrementDecrement);

    ExpEvalResult visit(ConditionalExpressionNode condExp);

    ExpEvalResult visit(FunctionCallNode funcCall);

    ExpEvalResult visit(CastExpressionNode castExp);

    ExpEvalResult visit(AddressOfNode addrOf);

    ExpEvalResult visit(DereferenceNode deref);

    sealed interface ExpEvalResult { }

    record PlainOperand(TacValue object) implements ExpEvalResult {
        public PlainOperand(Constant constant) {
            this(new TacConstant(constant));
        }
    }

    record DereferencedPointer(TacValue pointer) implements ExpEvalResult { }

}
