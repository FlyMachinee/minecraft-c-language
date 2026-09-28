package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class CallIndirect implements HighLevelInstruction {
    public HighLevelOperand funcPtr;

    public CallIndirect(HighLevelOperand funcPtr) {
        this.funcPtr = funcPtr;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
