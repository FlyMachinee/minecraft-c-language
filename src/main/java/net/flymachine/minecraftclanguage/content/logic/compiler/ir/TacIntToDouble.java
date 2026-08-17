package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

/**
 * 将有符号整数转换为双精度浮点数
 * 源操作数可为 int 或 long，目标操作数为 double
 */
public class TacIntToDouble implements TacInstruction {
    public TacValue src;
    public TacValue dst;

    public TacIntToDouble(TacValue src, TacValue dst) {
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
        stream.print("IntToDouble(src=");
        src.dump(stream);
        stream.print(", dst=");
        dst.dump(stream);
        stream.print(")");
    }
}
