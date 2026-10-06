package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.io.PrintStream;

public class TacAddPointer implements TacInstruction {
    public TacValue ptr;
    public TacValue index;
    public long scale;
    public TacValue dst;

    public TacAddPointer(TacValue ptr, TacValue index, long scale, TacValue dst) {
        this.ptr = ptr;
        this.index = index;
        this.scale = scale;
        this.dst = dst;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        stream.print("AddPointer(ptr=");
        ptr.dump(stream);
        stream.print(", index=");
        index.dump(stream);
        stream.print(", scale=");
        stream.print(scale);
        stream.print(", dst=");
        dst.dump(stream);
        stream.print(")");
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        // dst = ptr + index * scale
        dst.dumpPretty(stream);
        stream.print(" = ");
        ptr.dumpPretty(stream);
        stream.print(" + ");
        index.dumpPretty(stream);
        stream.print(" * ");
        stream.print(scale);
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
