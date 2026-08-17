package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class DoubleFromInt implements HighLevelInstruction {
    public HighLevelOperand src;
    public HighLevelOperand dst;
    public AsmType srcAsmType;

    public DoubleFromInt(HighLevelOperand src, HighLevelOperand dst, AsmType srcAsmType) {
        this.src = src;
        this.dst = dst;
        this.srcAsmType = srcAsmType;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
