package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

/**
 * 仅表示移动相同二进制表示的值
 * <p>
 * 如有需要，会将 src 加载至 T0/FT0
 * <p>
 * 可能使用的临时寄存器：T0 T1 T2 FT0
 */
public class Move implements HighLevelInstruction {
    public final AsmType asmType;

    /**
     * 如有需要，会将 src 加载至 T0/FT0
     */
    public HighLevelOperand src;
    public HighLevelOperand dst;

    public Move(AsmType asmType, HighLevelOperand src, HighLevelOperand dst) {
        this.asmType = asmType;
        this.src = src;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
