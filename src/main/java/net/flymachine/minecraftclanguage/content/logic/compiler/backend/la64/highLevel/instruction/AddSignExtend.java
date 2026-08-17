package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class AddSignExtend implements HighLevelInstruction {
    public HighLevelOperand src;
    public HighLevelOperand dst;

    public AddSignExtend(HighLevelOperand src, HighLevelOperand dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
