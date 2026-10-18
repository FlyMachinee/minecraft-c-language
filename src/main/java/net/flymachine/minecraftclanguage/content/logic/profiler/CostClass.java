package net.flymachine.minecraftclanguage.content.logic.profiler;

import java.util.Set;

/**
 * 指令代价类别
 */
public enum CostClass {

    /**
     * 普通算术 / 逻辑 / 移位指令
     */
    ALU,

    /**
     * 乘法
     */
    MUL,

    /**
     * 除法与取模
     */
    DIV,

    /**
     * 浮点运算
     */
    FLOAT,

    /**
     * 浮点除法
     */
    FLOAT_DIV,

    /**
     * 访存指令 (ld/ldx/st/stx/fld/fldx/fst/fstx)
     */
    MEMORY,

    /**
     * 条件/无条件分支
     */
    BRANCH,

    /**
     * 函数调用与间接跳转 (bl/jirl)
     */
    CALL;

    private static final Set<String> BRANCH_MNEMONICS = Set.of(
        "beq", "bne", "blt", "bge", "bltu", "bgeu",
        "beqz", "bnez", "bceqz", "bcnez", "b");

    private static final Set<String> CALL_MNEMONICS = Set.of("bl", "jirl");

    private static final Set<String> MUL_MNEMONICS = Set.of("mul.w", "mul.d");

    private static final Set<String> DIV_MNEMONICS = Set.of(
        "div.w", "mod.w", "div.wu", "mod.wu",
        "div.d", "mod.d", "div.du", "mod.du");

    private static final Set<String> FLOAT_DIV_MNEMONICS = Set.of("fdiv.d");

    private static final Set<String> FLOAT_MNEMONICS = Set.of(
        "fadd.d", "fsub.d", "fmul.d", "fneg.d", "fmov.d",
        "movgr2fr.d", "movfr2gr.d", "movcf2gr",
        "ftintrz.w.d", "ftintrz.l.d", "ffint.d.w", "ffint.d.l");

    private static final String[] MEMORY_PREFIXES = {
        "ld.", "ldx.", "st.", "stx.", "fld.", "fldx.", "fst.", "fstx."
    };

    /**
     * 浮点比较指令前缀
     */
    private static final String[] FLOAT_PREFIXES = {"fcmp."};

    /**
     * 根据助记符判断代价类别
     */
    public static CostClass classify(String mnemonic) {
        for (String prefix : MEMORY_PREFIXES) {
            if (mnemonic.startsWith(prefix)) {
                return MEMORY;
            }
        }
        for (String prefix : FLOAT_PREFIXES) {
            if (mnemonic.startsWith(prefix)) {
                return FLOAT;
            }
        }
        if (CALL_MNEMONICS.contains(mnemonic)) {
            return CALL;
        }
        if (BRANCH_MNEMONICS.contains(mnemonic)) {
            return BRANCH;
        }
        if (FLOAT_DIV_MNEMONICS.contains(mnemonic)) {
            return FLOAT_DIV;
        }
        if (FLOAT_MNEMONICS.contains(mnemonic)) {
            return FLOAT;
        }
        if (DIV_MNEMONICS.contains(mnemonic)) {
            return DIV;
        }
        if (MUL_MNEMONICS.contains(mnemonic)) {
            return MUL;
        }
        return ALU;
    }

    public boolean isBranchLike() {
        return this == BRANCH || this == CALL;
    }
}
