package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.io.PrintStream;

/**
 * 将无符号整数转换为双精度浮点数
 * 源操作数可为 unsigned char, unsigned int 或 unsigned long，目标操作数为 double
 */
public class TacUnsignedIntToDouble implements TacInstruction {
    public TacValue src;
    public TacValue dst;

    public TacUnsignedIntToDouble(TacValue src, TacValue dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void dump(java.io.PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        stream.print("UnsignedIntToDouble(src=");
        src.dump(stream);
        stream.print(", dst=");
        dst.dump(stream);
        stream.print(")");
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        // dst = (ui2d) src
        dst.dumpPretty(stream);
        stream.print(" = (ui2d) ");
        src.dumpPretty(stream);
    }
}
