package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;

/**
 * 数据标识符，标识着 data 段或 bss 段上的某个符号
 *
 * @param name 符号名
 */
public record Data(String name, AsmType asmType) implements HighLevelOperand {

    @Override
    public Data changeAsmType(AsmType asmType) {
        return new Data(name, asmType);
    }
}
