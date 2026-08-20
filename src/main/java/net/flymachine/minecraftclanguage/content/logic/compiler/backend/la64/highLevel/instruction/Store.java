package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

public class Store implements HighLevelInstruction {
    public AsmType srcAsmType;
    public HighLevelOperand src;
    public HighLevelOperand ptr;

    public Store(AsmType srcAsmType, HighLevelOperand src, HighLevelOperand ptr) {
        this.srcAsmType = srcAsmType;
        this.src = src;
        this.ptr = ptr;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
