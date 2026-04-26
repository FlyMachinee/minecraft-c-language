package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class Move implements HighLevelInstruction {
    public HighLevelOperand src;
    public HighLevelOperand dst;

    public Move(HighLevelOperand src, HighLevelOperand dst) {
        this.src = src;
        this.dst = dst;
    }
}
