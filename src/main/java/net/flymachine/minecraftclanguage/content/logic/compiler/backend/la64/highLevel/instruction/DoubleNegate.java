package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

/**
 * 如有需要，会将 src 加载至 FT0，结果存放在 FT0
 * <p>
 * 可能使用的临时寄存器：FT0 T0
 */
public class DoubleNegate implements HighLevelInstruction {
    /**
     * 如有需要，会将 src 加载至 FT0
     */
    public HighLevelOperand src;
    /**
     * 如有需要，会将结果存放在 FT0，然后再存放到 dst
     */
    public HighLevelOperand dst;

    public DoubleNegate(HighLevelOperand src, HighLevelOperand dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
