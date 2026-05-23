package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class AddSi12 implements HighLevelInstruction {
    public final AsmType asmType;
    public HighLevelOperand src;
    public int si12;
    public HighLevelOperand dst;

    public AddSi12(AsmType asmType, HighLevelOperand src, int si12, HighLevelOperand dst) {
        this.asmType = asmType;
        this.src = src;
        this.si12 = si12;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visitAddSi12(this);
    }
}
