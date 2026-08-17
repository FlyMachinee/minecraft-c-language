package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.ConditionFlagRegister;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class GetCC implements HighLevelInstruction {
    public ConditionFlagRegister cc;
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
