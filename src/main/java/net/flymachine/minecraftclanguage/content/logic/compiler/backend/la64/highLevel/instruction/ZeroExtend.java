package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

/**
 * 进行零拓展
 * <p>
 * 当源操作数宽度小于等于目标操作数宽度时，将源操作数的高位用零填充，随后存放到目标操作数
 * <p>
 * 当源操作数宽度大于目标操作数宽度时，将源操作数截断到目标操作数的宽度后，进行零拓展，随后存放到目标操作数
 * <p>
 * 如有需要，会将 src 加载至 T0，结果存放在 T0
 * <p>
 * 可能使用的临时寄存器：T0 T1
 */
public class ZeroExtend implements HighLevelInstruction {
    public AsmType srcType;
    public AsmType dstType;
    /**
     * 如有需要，会将 src 加载至 T0
     */
    public HighLevelOperand src;
    /**
     * 如有需要，会将结果存放在 T0，然后再存放到 dst
     */
    public HighLevelOperand dst;

    public ZeroExtend(AsmType srcType, AsmType dstType, HighLevelOperand src, HighLevelOperand dst) {
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
