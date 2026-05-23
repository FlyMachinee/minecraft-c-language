package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;

import java.io.PrintStream;

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
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        stream.append("Unary(").append(String.valueOf(op)).append(", src=");
        src.dump(stream);
        stream.print(", dst=");
        dst.dump(stream);
        stream.print(")");
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
