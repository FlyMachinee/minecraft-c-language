package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class DoubleToIntRoundZero implements HighLevelInstruction {
    public HighLevelOperand src;
    public HighLevelOperand dst;
    public AsmType dstAsmType;

    public DoubleToIntRoundZero(HighLevelOperand src, HighLevelOperand dst, AsmType dstAsmType) {
        this.src = src;
        this.dst = dst;
        this.dstAsmType = dstAsmType;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
