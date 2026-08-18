package net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction;

public enum LA64FloatCompareCondition {

    /**
     * 无，QNaN 不报例外
     */
    CAF(0x0),

    /**
     * 无，QNaN 报例外
     */
    SAF(0x1),

    /**
     * 小于，QNaN 不报例外
     */
    CLT(0x2),

    /**
     * 小于，QNaN 报例外
     */
    SLT(0x3),

    /**
     * 相等，QNaN 不报例外
     */
    CEQ(0x4),

    /**
     * 相等，QNaN 报例外
     */
    SEQ(0x5),

    /**
     * 小于等于，QNaN 不报例外
     */
    CLE(0x6),

    /**
     * 小于等于，QNaN 报例外
     */
    SLE(0x7),

    /**
     * 无法比较，QNaN 不报例外
     */
    CUN(0x8),

    /**
     * 不是大于小于或等于，QNaN 报例外
     */
    SUN(0x9),

    /**
     * 小于或无法比较，QNaN 不报例外
     */
    CULT(0xA),

    /**
     * 不是大于或等于，QNaN 报例外
     */
    SULT(0xB),

    /**
     * 相等或无法比较，QNaN 不报例外
     */
    CUEQ(0xC),

    /**
     * 不是大于或小于，QNaN 报例外
     */
    SUEQ(0xD),

    /**
     * 小于等于或无法比较，QNaN 不报例外
     */
    CULE(0xE),

    /**
     * 不是大于，QNaN 报例外
     */
    SULE(0xF),

    /**
     * 不等，QNaN 不报例外
     */
    CNE(0x10),

    /**
     * 不等，QNaN 报例外
     */
    SNE(0x11),

    /**
     * 有序，QNaN 不报例外
     */
    COR(0x14),

    /**
     * 有序，QNaN 报例外
     */
    SOR(0x15),

    /**
     * 无法比较或不等，QNaN 不报例外
     */
    CUNE(0x18),

    /**
     * 无法比较或不等，QNaN 报例外
     */
    SUNE(0x19);

    private final int code;

    LA64FloatCompareCondition(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public String mnemonic() {
        return name().toLowerCase();
    }

    static LA64FloatCompareCondition fromCode(int code) {
        for (LA64FloatCompareCondition condition : values()) {
            if (condition.code == code) {
                return condition;
            }
        }
        throw new IllegalArgumentException("Invalid LA64FloatCompareCondition code: " + code);
    }
}
