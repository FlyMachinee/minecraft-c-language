package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.ConditionFlagRegister;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

/**
 * 如有需要，会将结果存放至 T0 中
 * <p>
 * 可能使用的临时寄存器：T0 T1
 */
public class GetCC implements HighLevelInstruction {
    public ConditionFlagRegister cc;
    /**
     * 如有需要，会将结果存放至 T0 中，然后再存放至 dst
     */
    public HighLevelOperand dst;

    public GetCC(ConditionFlagRegister cc, HighLevelOperand dst) {
        this.cc = cc;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
