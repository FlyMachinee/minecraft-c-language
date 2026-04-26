package net.flymachine.minecraftclanguage.content.logic.architecture.la64.register;

import java.util.List;

public interface LA64Register {
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

