package net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacValue;

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
            indent(stream, indentLevel);
        }
        stream.append("Unary(").append(String.valueOf(op)).append(", src=");
        src.dump(stream);
        stream.print(", dst=");
        dst.dump(stream);
        stream.print(")");
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        // dst = op src
        dst.dumpPretty(stream);
        stream.print(" = ");
        stream.print(op.getSymbol());
        src.dumpPretty(stream);
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
