package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

/**
 * 如有需要，会将 ptr 加载至 T0，offset 加载至 T1，结果存放在 T0/FT0
 * <p>
 * 可能使用的临时寄存器
 * <li>整数操作：T0 T1</li>
 * <li>浮点操作：FT0 FT1 T0</li>
 */
public class Load implements HighLevelInstruction {
    public AsmType dstAsmType;
    /**
     * 如有需要，会将 ptr 加载至 T0
     */
    public HighLevelOperand ptr;
    /**
     * 如有需要，会将 offset 加载至 T1
     */
    public HighLevelOperand offset;
    /**
     * 如有需要，会将结果存放在 T0/FT0，然后再存放至 dst
     */
    public HighLevelOperand dst;

    public Load(AsmType dstAsmType, HighLevelOperand ptr, HighLevelOperand offset, HighLevelOperand dst) {
        this.dstAsmType = dstAsmType;
        this.ptr = ptr;
        this.offset = offset;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
