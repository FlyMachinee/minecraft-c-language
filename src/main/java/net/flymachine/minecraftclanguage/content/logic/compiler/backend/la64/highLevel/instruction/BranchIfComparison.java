package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;

public class BranchIfComparison implements HighLevelInstruction {
    public final Comparison cond;
    public final boolean isUnsigned;
    public final AsmType asmType;
    public HighLevelOperand lhs;
    public HighLevelOperand rhs;
    public final String target;

    public BranchIfComparison(
        Comparison cond, boolean isUnsigned, AsmType asmType,
        HighLevelOperand lhs, HighLevelOperand rhs, String target) {

        this.cond = cond;
        this.isUnsigned = isUnsigned;
        this.asmType = asmType;
        this.lhs = lhs;
        this.rhs = rhs;
        this.target = target;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
