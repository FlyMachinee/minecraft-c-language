package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

/**
 * 如有需要，会将结果存放在 T0 中
 * <p>
 * 可能使用的临时寄存器：T0 T1
 */
public class LoadAddress implements HighLevelInstruction {
    public HighLevelOperand obj;
    /**
     * 如有需要，会将结果存放在 T0 中，然后再存放到 dst
     */
    public HighLevelOperand dst;

    public LoadAddress(HighLevelOperand obj, HighLevelOperand dst) {
        this.obj = obj;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
