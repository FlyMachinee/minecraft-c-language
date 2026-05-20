package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;

/**
 * 伪寄存器，最终将会被替换为物理寄存器或栈上内存
 *
 * @param name
 */
public record Pseudo(String name, AsmType asmType) implements HighLevelOperand {

    @Override
    public HighLevelOperand changeAsmType(AsmType asmType) {
        return new Pseudo(name, asmType);
    }
}
