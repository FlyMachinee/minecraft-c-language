package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction.LA64FloatCompareCondition;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.ConditionFlagRegister;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

/**
 * 如有需要，会将 lhs 加载至 FT0，rhs 加载至 FT1
 * <p>
 * 可能使用的临时寄存器：FT0 FT1 T0
 */
public class CompareDouble implements HighLevelInstruction {
    public final LA64FloatCompareCondition cond;
    /**
     * 如有需要，会将 lhs 加载至 FT0
     */
    public HighLevelOperand lhs;
    /**
     * 如有需要，会将 rhs 加载至 FT1
     */
    public HighLevelOperand rhs;
    public ConditionFlagRegister cc;

    public CompareDouble(LA64FloatCompareCondition cond, HighLevelOperand lhs, HighLevelOperand rhs,
        ConditionFlagRegister cc) {
        this.cond = cond;
        this.lhs = lhs;
        this.rhs = rhs;
        this.cc = cc;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
