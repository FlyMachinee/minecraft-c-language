package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;

public class TacUnaryOperation implements TacInstruction {
    public UnaryOperator op;
    public TacValue src;
    public TacValue dst;

    public TacUnaryOperation(UnaryOperator op, TacValue src, TacValue dst) {
        this.op = op;
        this.src = src;
        this.dst = dst;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("Unary(").append(op).append(", src=");
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
