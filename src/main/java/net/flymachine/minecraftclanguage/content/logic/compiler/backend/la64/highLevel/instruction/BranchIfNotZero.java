package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class BranchIfNotZero implements HighLevelInstruction {
    public final AsmType asmType;
    public HighLevelOperand cond;
    public final String target;

    public BranchIfNotZero(AsmType asmType, HighLevelOperand cond, String target) {
        this.asmType = asmType;
        this.cond = cond;
        this.target = target;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visitBranchIfNotZero(this);
    }
}
