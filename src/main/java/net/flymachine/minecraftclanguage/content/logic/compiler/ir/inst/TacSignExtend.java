package net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacValue;

import java.io.PrintStream;

/**
 * 将左操作数的值符号扩展，并将结果存储在右操作数中
 */
public class TacSignExtend implements TacInstruction {
    public TacValue src;
    public TacValue dst;

    public TacSignExtend(TacValue src, TacValue dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        stream.print("SignExtend(src=");
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
        // dst = (sign ext) src
        dst.dumpPretty(stream);
        stream.print(" = (sign ext) ");
        src.dumpPretty(stream);
    }
}
