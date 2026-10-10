package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

/**
 * 如有需要，会将 cond 加载至 T0
 * <p>
 * 可能使用的临时寄存器：T0
 */
public class BranchIfZero implements HighLevelInstruction {
    public final AsmType asmType;
    /**
     * 如有需要，会将 cond 加载至 T0
     */
    public HighLevelOperand cond;
    public final String target;

    public BranchIfZero(AsmType asmType, HighLevelOperand cond, String target) {
        if (asmType == AsmType.DOUBLE) {
            throw new IllegalArgumentException("BranchIfZero does not support DOUBLE type");
        }
        this.asmType = asmType;
        this.cond = cond;
        this.target = target;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
