package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;

public class Compare implements HighLevelInstruction {
    public final Comparison cond;
    public final boolean isUnsigned;
    public final AsmType asmType;
    public HighLevelOperand lhs;
    public HighLevelOperand rhs;
    public HighLevelOperand dst;

    public Compare(
        Comparison cond, boolean isUnsigned, AsmType asmType,
        HighLevelOperand lhs, HighLevelOperand rhs, HighLevelOperand dst) {

        this.cond = cond;
        this.isUnsigned = isUnsigned;
        this.asmType = asmType;
        this.lhs = lhs;
        this.rhs = rhs;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visitCompare(this);
    }
}
