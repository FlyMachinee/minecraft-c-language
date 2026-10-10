package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;

/**
 * 如有需要，会将 lhs 加载至 T0，rhs 加载至 T1
 * <p>
 * 可能使用的临时寄存器：T0, T1
 */
public class BranchIfComparison implements HighLevelInstruction {
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
    public final String target;

    public BranchIfComparison(
        Comparison cond, boolean isUnsigned, AsmType asmType,
        HighLevelOperand lhs, HighLevelOperand rhs, String target) {

        if (asmType == AsmType.DOUBLE) {
            throw new IllegalArgumentException("BranchIfComparison does not support DOUBLE type");
        }
        this.cond = cond;
        this.isUnsigned = isUnsigned;
        this.asmType = asmType;
        this.lhs = lhs;
        this.rhs = rhs;
        this.target = target;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
