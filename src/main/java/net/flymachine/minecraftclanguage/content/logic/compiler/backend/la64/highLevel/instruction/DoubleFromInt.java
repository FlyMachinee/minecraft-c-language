package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

/**
 * 如有需要，会将 src 加载至 T0 中，结果存放在 FT0 中
 * <p>
 * 可能使用的临时寄存器：T0, FT0
 */
public class DoubleFromInt implements HighLevelInstruction {
    /**
     * 如有需要，会将 src 加载至 T0 中
     */
    public HighLevelOperand src;
    /**
     * 如有需要，会将结果存放在 FT0 中，然后再存放至 dst
     */
    public HighLevelOperand dst;
    public AsmType srcAsmType;

    public DoubleFromInt(HighLevelOperand src, HighLevelOperand dst, AsmType srcAsmType) {
        if (srcAsmType == AsmType.DOUBLE) {
            throw new IllegalArgumentException("Invalid AsmType for DoubleFromInt");
        }
        this.src = src;
        this.dst = dst;
        this.srcAsmType = srcAsmType;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
