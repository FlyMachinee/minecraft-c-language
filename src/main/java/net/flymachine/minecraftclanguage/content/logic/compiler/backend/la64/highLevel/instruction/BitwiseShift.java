package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class BitwiseShift implements HighLevelInstruction {
    public final boolean isLeftShift;
    public final AsmType asmType;
    public final boolean isUnsigned;
    public HighLevelOperand lhs;
    public HighLevelOperand rhs;
    public HighLevelOperand dst;

    public BitwiseShift(
        boolean isLeftShift, AsmType asmType, boolean isUnsigned,
        HighLevelOperand lhs, HighLevelOperand rhs, HighLevelOperand dst) {

        if (asmType == AsmType.DOUBLE) {
            throw new IllegalArgumentException("BitwiseShift does not support DOUBLE type");
        }
        this.isLeftShift = isLeftShift;
        this.asmType = asmType;
        this.isUnsigned = isUnsigned;
        this.lhs = lhs;
        this.rhs = rhs;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
