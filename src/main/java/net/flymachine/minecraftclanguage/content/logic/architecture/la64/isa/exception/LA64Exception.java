package net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.exception;

public enum LA64Exception {
    /**
     * 中断
     */
    INT(0x0, 0),

    /**
     * load操作页无效例外
     */
    PIL(0x1, 0),

    /**
     * store操作页无效例外
     */
    PIS(0x2, 0),

    /**
     * 取指操作页无效例外
     */
    PIF(0x3, 0),

    /**
     * 页修改例外
     */
    PME(0x4, 0),

    /**
     * 页不可读例外
     */
    PNR(0x5, 0),

    /**
     * 页不可执行例外
     */
    PNX(0x6, 0),

    /**
     * 页特权等级不合规例外
     */
    PPI(0x7, 0),

    /**
     * 取指地址错例外
     */
    ADEF(0x8, 0),

    /**
     * 访存指令地址错例外
     */
    ADEM(0x8, 1),

    /**
     * 地址非对齐例外
     */
    ALE(0x9, 0),

    /**
     * 边界检查错例外
     */
    BCE(0xA, 0),

    /**
     * 系统调用例外
     */
    SYS(0xB, 0),

    /**
     * 断点例外
     */
    BRK(0xC, 0),

    /**
     * 指令不存在例外
     */
    INE(0xD, 0),

    /**
     * 指令特权等级错例外
     */
    IPE(0xE, 0),

    /**
     * 浮点指令未使能例外
     */
    FPD(0xF, 0),

    /**
     * 基础浮点指令例外
     */
    FPE(0x12, 0);

    LA64Exception(int eCode, int eSubCode) {
        this.eCode = eCode;
        this.eSubCode = eSubCode;
    }

    private final int eCode;
    private final int eSubCode;

    public int getECode() {
        return eCode;
    }

    public int getESubCode() {
        return eSubCode;
    }
}
