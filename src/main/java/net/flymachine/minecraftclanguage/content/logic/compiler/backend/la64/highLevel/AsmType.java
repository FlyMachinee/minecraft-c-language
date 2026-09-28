package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel;

public sealed interface AsmType permits AsmType.Word, AsmType.DWord, AsmType.Double, AsmType.ByteArray {

    /**
     * 合理的 32 位数，无论是有符号数，还是无符号数，存在于寄存器中时，均为符号拓展的形式
     */
    AsmType WORD = new Word();

    /**
     * 合理的 64 位数
     */
    AsmType DWORD = new DWord();

    /**
     * 合理的 64 位浮点数
     */
    AsmType DOUBLE = new Double();

    record Word() implements AsmType { }

    record DWord() implements AsmType { }

    record Double() implements AsmType { }

    record ByteArray(long size, long alignment) implements AsmType { }

    default long size() {
        if (this instanceof Word) { return 4; }
        if (this instanceof DWord || this instanceof Double) { return 8; }
        if (this instanceof ByteArray ba) { return ba.size(); }
        throw new AssertionError("unknown AsmType: " + this);
    }

    default long alignment() {
        if (this instanceof Word) { return 4; }
        if (this instanceof DWord || this instanceof Double) { return 8; }
        if (this instanceof ByteArray ba) { return ba.alignment(); }
        throw new AssertionError("unknown AsmType: " + this);
    }

}
