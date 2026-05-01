package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;

public class Binary implements HighLevelInstruction {
    public BinaryOperator op;
    public HighLevelOperand lhs;
    public HighLevelOperand rhs;
    public HighLevelOperand dst;

    public Binary(BinaryOperator op, HighLevelOperand lhs, HighLevelOperand rhs, HighLevelOperand dst) {
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visitBinary(this);
    }
}
