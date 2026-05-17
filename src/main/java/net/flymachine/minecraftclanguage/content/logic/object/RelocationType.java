package net.flymachine.minecraftclanguage.content.logic.object;

public enum RelocationType {

    /**
     * 18 位相对 PC 跳转
     * <p>
     * (*(uint32_t *) PC) [25:10] = (S+A-PC) [17:2]
     * <p>
     * 带 18 位有符号数溢出和 4 字节对齐检测功能
     */
    R_LARCH_B16,

    /**
     * 23 位相对 PC 跳转
     * <p>
     * (*(uint32_t *) PC) [4:0] = (S+A-PC) [22:18]
     * <p>
     * (*(uint32_t *) PC) [25:10] = (S+A-PC) [17:2]
     * <p>
     * 带 23 位有符号数溢出和 4 字节对齐检测功能
     */
    R_LARCH_B21,

    /**
     * 28 位相对 PC 跳转
     * <p>
     * (*(uint32_t *) PC) [9:0] = (S+A-PC) [27:18]
     * <p>
     * (*(uint32_t *) PC) [25:10] = (S+A-PC) [17:2]
     * <p>
     * 带 28 位有符号数溢出和 4 字节对齐检测功能
     */
    R_LARCH_B26,

    /**
     * 32/64 位绝对地址的 [31:12] 位
     * <p>
     * <p>
     * (*(uint32_t *) PC) [24:5] = (S+A) [31:12]
     */
    R_LARCH_ABS_HI20,

    /**
     * 32/64 位绝对地址的 [11:0] 位
     * <p>
     * (*(uint32_t *) PC) [21:10] = (S+A) [11:0]
     */
    R_LARCH_ABS_LO12,

    /**
     * 64 位绝对地址的 [51:32] 位
     * <p>
     * (*(uint32_t *) PC) [24:5] = (S+A) [51:32]
     */
    R_LARCH_ABS64_LO20,

    /**
     * 64 位绝对地址的 [63:52] 位
     * <p>
     * (*(uint32_t *) PC) [21:10] = (S+A) [63:52]
     */
    R_LARCH_ABS64_HI12,

    /**
     * 32/64 位相对PC偏移的 [31:12] 位
     * <p>
     * (*(uint32_t *) PC) [24:5] = (((S+A) & ~0xfff) - (PC & ~0xfff)) [31:12]
     * <p>
     * 注意：所有相对 PC 偏移计算都不包含低 12 位
     */
    R_LARCH_PCALA_HI20,

    /**
     * 32/64 位相对PC偏移的 [11:0] 位
     * <p>
     * (*(uint32_t *) PC) [21:10] = (S+A) [11:0]
     */
    R_LARCH_PCALA_LO12,

    /**
     * 64 位相对PC偏移的 [51:32] 位
     * <p>
     * (*(uint32_t *) PC) [24:5] = (S+A - (PC & ~0xffffffff)) [51:32]
     */
    R_LARCH_PCALA64_LO20,

    /**
     * 64 位相对PC偏移的 [63:52] 位
     * <p>
     * (*(uint32_t *) PC) [21:10] = (S+A - (PC & ~0xffffffff)) [63:52]
     */
    R_LARCH_PCALA64_HI12,

}
