package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.ConditionFlagRegister;

public class BranchIfCCZero implements HighLevelInstruction {

    public ConditionFlagRegister cc;
    public String target;

    public BranchIfCCZero(ConditionFlagRegister cc, String target) {
        this.cc = cc;
        this.target = target;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
