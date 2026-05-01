package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;

public class Unary implements HighLevelInstruction {
    public UnaryOperator op;
    public HighLevelOperand src;
    public HighLevelOperand dst;

    public Unary(UnaryOperator op, HighLevelOperand src, HighLevelOperand dst) {
        this.op = op;
        this.src = src;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visitUnary(this);
    }
}
