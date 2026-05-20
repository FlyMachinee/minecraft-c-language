package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.LA64Register;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;

public record Reg(LA64Register reg, AsmType asmType) implements HighLevelOperand {
    
    @Override
    public HighLevelOperand changeAsmType(AsmType asmType) {
        return new Reg(reg, asmType);
    }
}
