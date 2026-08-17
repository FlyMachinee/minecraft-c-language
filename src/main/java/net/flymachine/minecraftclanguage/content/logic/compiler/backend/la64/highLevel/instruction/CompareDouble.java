package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction.LA64FloatCompareCondition;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.ConditionFlagRegister;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class CompareDouble implements HighLevelInstruction {
    public final LA64FloatCompareCondition cond;
    public HighLevelOperand lhs;
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
