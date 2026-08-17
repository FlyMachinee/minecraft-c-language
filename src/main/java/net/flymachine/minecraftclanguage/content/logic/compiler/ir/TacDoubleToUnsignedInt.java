package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

/**
 * 将双精度浮点数转换为无符号整数
 * 源操作数为 double，目标操作数可为 unsigned int 或 unsigned long
 */
public class TacDoubleToUnsignedInt implements TacInstruction {
    public TacValue src;
    public TacValue dst;

    public TacDoubleToUnsignedInt(TacValue src, TacValue dst) {
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
        stream.print("DoubleToUnsignedInt(src=");
        src.dump(stream);
        stream.print(", dst=");
        dst.dump(stream);
        stream.print(")");
    }
}
