package net.flymachine.minecraftclanguage.content.logic.architecture.la64.register;

import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64AsmOperand;

import java.util.List;

public interface LA64Register extends LA64AsmOperand {
    /**
     * @return 寄存器类型
     */
    RegType getType();

    /**
     * @return 寄存器编号
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
        GPR, FPR, CFR
    }
}

