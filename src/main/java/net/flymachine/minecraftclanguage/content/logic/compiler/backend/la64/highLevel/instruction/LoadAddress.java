package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class LoadAddress implements HighLevelInstruction {
    public HighLevelOperand obj;
    public HighLevelOperand dst;

    public LoadAddress(HighLevelOperand obj, HighLevelOperand dst) {
        this.obj = obj;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
