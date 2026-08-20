package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class Load implements HighLevelInstruction {
    public AsmType dstAsmType;
    public HighLevelOperand ptr;
    public HighLevelOperand dst;

    public Load(AsmType dstAsmType, HighLevelOperand ptr, HighLevelOperand dst) {
        this.dstAsmType = dstAsmType;
        this.ptr = ptr;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
