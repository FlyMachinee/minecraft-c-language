package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;

public interface HighLevelOperand {
    AsmType asmType();

    HighLevelOperand changeAsmType(AsmType asmType);
}
