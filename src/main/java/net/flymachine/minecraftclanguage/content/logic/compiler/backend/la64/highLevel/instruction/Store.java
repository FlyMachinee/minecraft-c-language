package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

/**
 * 如有需要，会将 ptr 加载至 T0，src 加载至 T1/FT1，offset 加载至 T2
 * <p>
 * 可能使用的临时寄存器
 * <li>整数操作：T0 T1 T2</li>
 * <li>浮点操作：FT1 T0 T1 T2</li>
 */
public class Store implements HighLevelInstruction {
    public AsmType srcAsmType;
    /**
     * 如有需要，会将 src 加载至 T0/FT0
     */
    public HighLevelOperand src;
    /**
     * 如有需要，会将 ptr 加载至 T1
     */
    public HighLevelOperand ptr;
    /**
     * 如有需要，会将 offset 加载至 T2
     */
    public HighLevelOperand offset;

    public Store(AsmType srcAsmType, HighLevelOperand src, HighLevelOperand ptr, HighLevelOperand offset) {
        this.srcAsmType = srcAsmType;
        this.src = src;
        this.ptr = ptr;
        this.offset = offset;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
