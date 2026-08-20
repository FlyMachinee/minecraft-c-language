package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.io.PrintStream;

public class TacGetAddress implements TacInstruction {
    public TacValue src;
    public TacValue dst;

    public TacGetAddress(TacValue src, TacValue dst) {
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
            stream.print("  ".repeat(indentLevel));
        }
        stream.print("GetAddress(src=");
        src.dump(stream);
        stream.print(", dst=");
        dst.dump(stream);
        stream.print(")");
    }
}
