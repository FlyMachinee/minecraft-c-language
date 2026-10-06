package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.io.PrintStream;
import java.util.Arrays;

public class TacCopyByteArrayToOffset implements TacInstruction {
    public byte[] data;
    public String dst;
    public long offset;

    public TacCopyByteArrayToOffset(byte[] data, String dst, long offset) {
        this.data = data;
        this.dst = dst;
        this.offset = offset;
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        stream.print("CopyByteArrayToOffset(data=");
        stream.print(Arrays.toString(data));
        stream.print(", dst=");
        stream.print(dst);
        stream.print(", offset=");
        stream.print(offset);
        stream.print(")");
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        // dst[offset:] = data
        stream.print(dst);
        stream.print("[");
        stream.print(offset);
        stream.print(":] = ");
        stream.print(Arrays.toString(data));
    }
}
