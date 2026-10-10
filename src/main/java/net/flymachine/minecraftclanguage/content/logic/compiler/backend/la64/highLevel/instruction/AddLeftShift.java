package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

/**
 * 如有需要，会将 base 加载至 T0，index 加载至 T1，结果存放至 T0
 * <p>
 * 可能使用的临时寄存器：T0 T1
 */
public class AddLeftShift implements HighLevelInstruction {
    /**
     * 如有需要，会将 base 加载至 T0
     */
    public HighLevelOperand base;
    /**
     * 如有需要，会将 index 加载至 T1
     */
    public HighLevelOperand index;
    public int shiftAmount;
    /**
     * 如有需要，会将结果存放至 T0，然后再存放至 dst
     */
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
