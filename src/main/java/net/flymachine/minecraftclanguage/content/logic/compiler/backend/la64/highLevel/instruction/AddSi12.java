package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

/**
 * 如有需要，会将 src 加载至 T0，结果存放在 T0
 * <p>
 * 可能使用的临时寄存器：T0 T1
 */
public class AddSi12 implements HighLevelInstruction {
    public final AsmType asmType;
    /**
     * 如有需要，会将 src 加载至 T0
     */
    public HighLevelOperand src;
    public int si12;
    /**
     * 如有需要，会将结果存放在 T0，然后再存放至 dst
     */
    public HighLevelOperand dst;

    public AddSi12(AsmType asmType, HighLevelOperand src, int si12, HighLevelOperand dst) {
        if (asmType == AsmType.DOUBLE) {
            throw new IllegalArgumentException("AddSi12 does not support DOUBLE type");
        }
        this.asmType = asmType;
        this.src = src;
        this.si12 = si12;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
