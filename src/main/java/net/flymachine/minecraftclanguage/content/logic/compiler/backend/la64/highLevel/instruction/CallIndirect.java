package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

/**
 * 如有需要，会将 funcPtr 加载至 T0
 * <p>
 * 可能使用的临时寄存器：T0
 */
public class CallIndirect implements HighLevelInstruction {
    /**
     * 如有需要，会将 funcPtr 加载至 T0
     */
    public HighLevelOperand funcPtr;

    public CallIndirect(HighLevelOperand funcPtr) {
        this.funcPtr = funcPtr;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
