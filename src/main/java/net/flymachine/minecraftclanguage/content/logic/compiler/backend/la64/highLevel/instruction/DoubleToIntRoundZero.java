package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

/**
 * 如有需要，会将 src 加载至 FT0，结果存放至 T0 中
 * <p>
 * 可能使用的临时寄存器：FT0 T0 T1
 */
public class DoubleToIntRoundZero implements HighLevelInstruction {
    /**
     * 如有需要，会将 src 加载至 FT0
     */
    public HighLevelOperand src;
    /**
     * 如有需要，会将结果存放至 T0 中，然后再存放到 dst
     */
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
