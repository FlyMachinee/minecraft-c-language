package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class Move implements HighLevelInstruction {
    public final AsmType asmType;
    public HighLevelOperand src;
    public HighLevelOperand dst;

    public Move(AsmType asmType, HighLevelOperand src, HighLevelOperand dst) {
        this.asmType = asmType;
        this.src = src;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
