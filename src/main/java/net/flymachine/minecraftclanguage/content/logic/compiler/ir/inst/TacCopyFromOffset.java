package net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacValue;

import java.io.PrintStream;

public class TacCopyFromOffset implements TacInstruction {
    public String src;
    public long offset;
    public TacValue dst;

    public TacCopyFromOffset(String src, long offset, TacValue dst) {
        this.src = src;
        this.offset = offset;
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
        stream.print("CopyFromOffset(src=");
        stream.print(src);
        stream.print(", offset=");
        stream.print(offset);
        stream.print(", dst=");
        dst.dump(stream);
        stream.print(")");
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        // dst = src[offset:]
        dst.dumpPretty(stream);
        stream.print(" = ");
        stream.print(src);
        stream.print("[");
        stream.print(offset);
        stream.print(":]");
    }
}
