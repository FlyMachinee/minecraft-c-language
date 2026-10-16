package net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacValue;

import java.io.PrintStream;

public class TacCopyToOffset implements TacInstruction {
    public TacValue src;
    public String dst;
    public long offset;

    public TacCopyToOffset(TacValue src, String dst, long offset) {
        this.src = src;
        this.dst = dst;
        this.offset = offset;
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void dump(java.io.PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        stream.print("CopyToOffset(src=");
        src.dump(stream);
        stream.print(", dst=");
        stream.print(dst);
        stream.print(", offset=");
        stream.print(offset);
        stream.print(")");
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        // dst[offset:] = src
        stream.print(dst);
        stream.print("[");
        stream.print(offset);
        stream.print(":] = ");
        src.dumpPretty(stream);
    }
}
