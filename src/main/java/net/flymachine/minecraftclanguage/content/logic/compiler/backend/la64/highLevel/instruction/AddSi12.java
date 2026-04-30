package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class AddSi12 implements HighLevelInstruction {
    public HighLevelOperand src;
    public int si12;
    public HighLevelOperand dst;

    public AddSi12(HighLevelOperand src, int si12, HighLevelOperand dst) {
        this.src = src;
        this.si12 = si12;
        this.dst = dst;
    }
}
