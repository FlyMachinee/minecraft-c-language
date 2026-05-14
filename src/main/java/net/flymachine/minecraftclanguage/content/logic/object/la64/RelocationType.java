package net.flymachine.minecraftclanguage.content.logic.object.la64;

public enum RelocationType {

    /**
     * 18 位相对 PC 跳转
     */
    R_LARCH_B16,

    /**
     * 23 位相对 PC 跳转
     */
    R_LARCH_B21,

    /**
     * 28 位相对 PC 跳转
     */
    R_LARCH_B26,

    /**
     * 32/64 位绝对地址的 [31:12] 位
     */
    R_LARCH_ABS_HI20,

    /**
     * 32/64 位绝对地址的 [11:0] 位
     */
    R_LARCH_ABS_LO12,

    /**
     * 64 位绝对地址的 [51:32] 位
     */
    R_LARCH_ABS64_LO20,
    
    /**
     * 64 位绝对地址的 [63:52] 位
     */
    R_LARCH_ABS64_HI12,

}
