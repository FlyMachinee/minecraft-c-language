package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class CopyByteArray implements HighLevelInstruction {
    public final byte[] data;
    public HighLevelOperand dst;

    public CopyByteArray(byte[] data, HighLevelOperand dst) {
        this.data = data;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
