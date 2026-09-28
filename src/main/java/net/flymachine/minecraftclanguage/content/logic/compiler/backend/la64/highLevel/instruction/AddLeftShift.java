package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class AddLeftShift implements HighLevelInstruction {
    public HighLevelOperand base;
    public HighLevelOperand index;
    public int shiftAmount;
    public HighLevelOperand dst;

    public AddLeftShift(HighLevelOperand base, HighLevelOperand index, int shiftAmount, HighLevelOperand dst) {
        if (shiftAmount < 1 || shiftAmount > 4) {
            throw new IllegalArgumentException("shiftAmount must be between 1 and 4");
        }
        this.base = base;
        this.index = index;
        this.shiftAmount = shiftAmount;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
