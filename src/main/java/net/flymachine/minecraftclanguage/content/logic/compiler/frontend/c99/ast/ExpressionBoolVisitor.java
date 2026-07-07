package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;

/**
 * 对表达式进行求值，在求值末尾处生成“若求值结果为真”则跳转的指令
 * <p>
 * 需要保证传入的标签始终有定义，即使该方法最终断言始终跳转或永不跳转也不例外
 * <p>
 * exp - 要求值的表达式
 * <p>
 * jumpTarget - 生成跳转指令的跳转目标
 * <p>
 * inverse - 是否将表达式条件取反
 * <p>
 * 如果生成的跳转指令无条件跳转则返回 {@link BoolGenResult#ALWAYS_JUMP}，如果永不跳转则返回
 * {@link BoolGenResult#NEVER_JUMP}，否则返回 {@link BoolGenResult#VARIOUS}。
 * 返回 {@link BoolGenResult#ALWAYS_JUMP} 或 {@link BoolGenResult#NEVER_JUMP} 时将不生成任何跳转指令
 */
public interface ExpressionBoolVisitor {

    BoolGenResult visit(ConstantNode constant, String jumpTarget, boolean inverse);

    BoolGenResult visit(UnaryExpressionNode unaryExp, String jumpTarget, boolean inverse);

    BoolGenResult visit(BinaryExpressionNode binaryExp, String jumpTarget, boolean inverse);

    BoolGenResult visit(AssignmentNode assignment, String jumpTarget, boolean inverse);

    BoolGenResult visit(VariableNode variable, String jumpTarget, boolean inverse);

    BoolGenResult visit(IncrementDecrementNode incrementDecrement, String jumpTarget, boolean inverse);

    BoolGenResult visit(ConditionalExpressionNode condExp, String jumpTarget, boolean inverse);

    BoolGenResult visit(FunctionCallNode funcCall, String jumpTarget, boolean inverse);

    BoolGenResult visit(CastExpressionNode castExp, String jumpTarget, boolean inverse);

    /**
     * 作为该 visitor 的返回值
     */
    enum BoolGenResult {
        /**
         * 跳转指令的跳转结果在该阶段无法断言
         */
        VARIOUS,

        /**
         * 断言跳转指令无条件跳转
         */
        ALWAYS_JUMP,

        /**
         * 断言跳转指令永不跳转
         */
        NEVER_JUMP
    }
}