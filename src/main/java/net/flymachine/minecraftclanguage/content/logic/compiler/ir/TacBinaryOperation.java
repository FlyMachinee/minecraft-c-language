package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;

import java.io.PrintStream;

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
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        stream.append("Binary(").append(String.valueOf(op)).append(", lhs=");
        lhs.dump(stream);
        stream.print(", rhs=");
        rhs.dump(stream);
        stream.print(", dst=");
        dst.dump(stream);
        stream.print(")");
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
