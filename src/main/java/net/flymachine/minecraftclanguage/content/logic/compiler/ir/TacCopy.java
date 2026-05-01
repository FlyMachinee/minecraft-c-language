package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

public class TacCopy implements TacInstruction {
    public TacValue src;
    public TacValue dst;

    public TacCopy(TacValue src, TacValue dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("Copy(src=");
        src.genFormattedString(stringBuilder);
        stringBuilder.append(", dst=");
        dst.genFormattedString(stringBuilder);
        stringBuilder.append(")");
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
