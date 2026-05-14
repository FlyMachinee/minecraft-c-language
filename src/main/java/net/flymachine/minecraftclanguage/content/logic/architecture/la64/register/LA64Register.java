package net.flymachine.minecraftclanguage.content.logic.architecture.la64.register;

import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64AsmOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

import java.util.List;

public interface LA64Register extends HighLevelOperand, LA64AsmOperand {
    /**
     * @return 寄存器类型（通用或浮点）
     */
    RegType getType();

    /**
     * @return 寄存器编号 0-31
     */
    int getNumber();

    /**
     * @return 寄存器名称（如 "a0", "sp"）
     */
    String getPrimaryName();

    /**
     * @return 所有别名（包括名称）
     */
    List<String> getNames();

    /**
     * 寄存器类型枚举
     */
    enum RegType {
        GPR, FPR
    }
}

