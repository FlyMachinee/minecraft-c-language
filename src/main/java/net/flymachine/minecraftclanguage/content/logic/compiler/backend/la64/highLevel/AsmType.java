package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel;

public enum AsmType {

    /**
     * 合理的 32 位数，无论是有符号数，还是无符号数，存在于寄存器中时，均为符号拓展的形式
     */
    WORD,

    /**
     * 合理的 64 位数
     */
    DWORD;

    public int alignment() {
        return switch (this) {
            case WORD -> 4;
            case DWORD -> 8;
        };
    }
}
