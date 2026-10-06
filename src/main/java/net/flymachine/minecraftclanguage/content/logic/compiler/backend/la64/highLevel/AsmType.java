package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel;

public sealed interface AsmType permits AsmType.ByteArray, AsmType.PrimitiveType {

    /**
     * 合理的有符号 8 位数，有符号数为符号拓展
     */
    AsmType BYTE = new PrimitiveType(Primitive.BYTE);

    /**
     * 合理的无符号 8 位数，无符号数为零拓展
     */
    AsmType UBYTE = new PrimitiveType(Primitive.UBYTE);

    /**
     * 仅内部使用
     */
    AsmType HALF = new PrimitiveType(Primitive.HALF);

    /**
     * 仅内部使用
     */
    AsmType UHALF = new PrimitiveType(Primitive.UHALF);

    /**
     * 合理的 32 位数，无论是有符号数，还是无符号数，存在于寄存器中时，均为符号拓展的形式
     */
    AsmType WORD = new PrimitiveType(Primitive.WORD);

    /**
     * 仅内部使用
     */
    AsmType UWORD = new PrimitiveType(Primitive.UWORD);

    /**
     * 合理的 64 位数
     */
    AsmType DWORD = new PrimitiveType(Primitive.DWORD);

    /**
     * 合理的 64 位浮点数
     */
    AsmType DOUBLE = new PrimitiveType(Primitive.DOUBLE);

    record PrimitiveType(Primitive p) implements AsmType {
        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (!(o instanceof PrimitiveType that)) { return false; }
            return p == that.p;
        }

        @Override
        public int hashCode() {
            return p.hashCode();
        }

        @Override
        public long size() {
            return p.size;
        }

        @Override
        public long alignment() {
            return size();
        }
    }

    enum Primitive {
        BYTE(1, "b"),
        UBYTE(1, "bu"),
        HALF(2, "h"),
        UHALF(2, "hu"),
        WORD(4, "w"),
        UWORD(4, "wu"),
        DWORD(8, "d"),
        DOUBLE(8, "d");

        public final long size;
        public final String suffix;

        Primitive(long size, String suffix) {
            this.size = size;
            this.suffix = suffix;
        }
    }

    record ByteArray(long size, long alignment) implements AsmType { }

    long size();

    long alignment();

}
