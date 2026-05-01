package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class BranchIfNotZero implements HighLevelInstruction {
    public HighLevelOperand cond;
    public String target;

    public BranchIfNotZero(HighLevelOperand cond, String target) {
        this.cond = cond;
        this.target = target;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visitBranchIfNotZero(this);
    }
}
