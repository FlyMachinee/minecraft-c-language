package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

/**
 * 如有需要，会将 lhs 加载至 T0，rhs 加载至 T1，结果存放在 T0
 * <p>
 * 可能使用的临时寄存器：T0 T1
 */
public class BitwiseShift implements HighLevelInstruction {
    public final boolean isLeftShift;
    public final AsmType asmType;
    public final boolean isUnsigned;
    /**
     * 如有需要，会将 lhs 加载至 T0
     */
    public HighLevelOperand lhs;
    /**
     * 如有需要，会将 rhs 加载至 T1
     */
    public HighLevelOperand rhs;
    /**
     * 如有需要，会将结果存放在 T0，然后再存放到 dst
     */
    public HighLevelOperand dst;

    public BitwiseShift(
        boolean isLeftShift, AsmType asmType, boolean isUnsigned,
        HighLevelOperand lhs, HighLevelOperand rhs, HighLevelOperand dst) {

        if (asmType == AsmType.DOUBLE) {
            throw new IllegalArgumentException("BitwiseShift does not support DOUBLE type");
        }
        this.isLeftShift = isLeftShift;
        this.asmType = asmType;
        this.isUnsigned = isUnsigned;
        this.lhs = lhs;
        this.rhs = rhs;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
