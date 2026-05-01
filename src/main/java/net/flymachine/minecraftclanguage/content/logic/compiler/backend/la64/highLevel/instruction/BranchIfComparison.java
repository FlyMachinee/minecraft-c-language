package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;

public class BranchIfComparison implements HighLevelInstruction {
    public Comparison cond;
    public HighLevelOperand lhs;
    public HighLevelOperand rhs;
    public String target;

    public BranchIfComparison(Comparison cond, HighLevelOperand lhs, HighLevelOperand rhs, String target) {
        this.cond = cond;
        this.lhs = lhs;
        this.rhs = rhs;
        this.target = target;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visitBranchIfComparison(this);
    }
}
