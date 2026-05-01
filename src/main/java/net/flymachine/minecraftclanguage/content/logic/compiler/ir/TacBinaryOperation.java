package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;

public class TacBinaryOperation implements TacInstruction {
    public BinaryOperator op;
    public TacValue lhs;
    public TacValue rhs;
    public TacValue dst;

    public TacBinaryOperation(BinaryOperator op, TacValue lhs, TacValue rhs, TacValue dst) {
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
        this.dst = dst;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("Binary(").append(op).append(", lhs=");
        lhs.genFormattedString(stringBuilder);
        stringBuilder.append(", rhs=");
        rhs.genFormattedString(stringBuilder);
        stringBuilder.append(", dst=");
        dst.genFormattedString(stringBuilder);
        stringBuilder.append(")");
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
