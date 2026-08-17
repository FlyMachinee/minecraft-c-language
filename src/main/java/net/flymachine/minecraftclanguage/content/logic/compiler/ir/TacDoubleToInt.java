package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.io.PrintStream;

/**
 * 将双精度浮点数转换为有符号整数
 * 源操作数为 double，目标操作数可为 int 或 long
 */
public class TacDoubleToInt implements TacInstruction {
    public TacValue src;
    public TacValue dst;

    public TacDoubleToInt(TacValue src, TacValue dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        stream.print("DoubleToInt(src=");
        src.dump(stream);
        stream.print(", dst=");
        dst.dump(stream);
        stream.print(")");
    }
}
