package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;

public record Immediate(long value, AsmType asmType) implements HighLevelOperand {

    @Override
    public Immediate changeAsmType(AsmType asmType) {
        return new Immediate(value, asmType);
    }
}
