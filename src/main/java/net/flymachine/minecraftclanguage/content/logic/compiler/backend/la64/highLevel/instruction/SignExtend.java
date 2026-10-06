package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

/**
 * 进行符号拓展
 * <p>
 * 当源操作数宽度小于等于目标操作数宽度时，将源操作数的符号位扩展，随后存放到目标操作数
 * <p>
 * 当源操作数宽度大于目标操作数宽度时，将源操作数截断到目标操作数的宽度后，进行符号拓展，随后存放到目标操作数
 */
public class SignExtend implements HighLevelInstruction {
    public AsmType srcType;
    public AsmType dstType;
    public HighLevelOperand src;
    public HighLevelOperand dst;

    public SignExtend(AsmType srcType, AsmType dstType, HighLevelOperand src, HighLevelOperand dst) {
        this.srcType = srcType;
        this.dstType = dstType;
        this.src = src;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
