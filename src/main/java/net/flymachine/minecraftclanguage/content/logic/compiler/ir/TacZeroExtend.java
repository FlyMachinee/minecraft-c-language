package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

/**
 * 将左操作数视为 unsigned int 值，右操作数视为 unsigned long 值，将左操作数的值零扩展为 unsigned long，并将结果存储在右操作数中
 */

public class TacZeroExtend implements TacInstruction {
    public TacValue src;
    public TacValue dst;

    public TacZeroExtend(TacValue src, TacValue dst) {
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
        stringBuilder.append("ZeroExtend(src=");
        src.genFormattedString(stringBuilder);
        stringBuilder.append(", dst=");
        dst.genFormattedString(stringBuilder);
        stringBuilder.append(")");
    }

}
