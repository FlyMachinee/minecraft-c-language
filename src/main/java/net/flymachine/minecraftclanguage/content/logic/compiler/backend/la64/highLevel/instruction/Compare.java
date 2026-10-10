package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;

/**
 * 如有需要，会将 lhs 加载至 T0，rhs 加载至 T1，结果存放在 T0
 * <p>
 * 可能使用的临时寄存器：T0, T1
 */
public class Compare implements HighLevelInstruction {
    public final Comparison cond;
    public final boolean isUnsigned;
    public final AsmType asmType;
    /**
     * 如有需要，会将 lhs 加载至 T0
     */
    public HighLevelOperand lhs;
    /**
     * 如有需要，会将 rhs 加载至 T1
     */
    public HighLevelOperand rhs;
    /**
     * 如有需要，会将结果存放在 T0，然后再存放至 dst
     */
    public HighLevelOperand dst;

    public Compare(
        Comparison cond, boolean isUnsigned, AsmType asmType,
        HighLevelOperand lhs, HighLevelOperand rhs, HighLevelOperand dst) {

        if (asmType == AsmType.DOUBLE) {
            throw new IllegalArgumentException("Compare does not support DOUBLE type");
        }
        this.cond = cond;
        this.isUnsigned = isUnsigned;
        this.asmType = asmType;
        this.lhs = lhs;
        this.rhs = rhs;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
