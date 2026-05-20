package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

/**
 * 将左操作数视为 long 值，右操作数视为 int 值，将左操作数的值截断为 int，并将结果存储在右操作数中
 */
public class TacTruncate implements TacInstruction {
    public TacValue src;
    public TacValue dst;

    public TacTruncate(TacValue src, TacValue dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("Truncate(src=");
        src.genFormattedString(stringBuilder);
        stringBuilder.append(", dst=");
        dst.genFormattedString(stringBuilder);
        stringBuilder.append(")");
    }
}
