package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.io.PrintStream;

public class TacLoad implements TacInstruction {
    public TacValue srcPtr;
    public TacValue dst;

    public TacLoad(TacValue srcPtr, TacValue dst) {
        this.srcPtr = srcPtr;
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
        stream.print("Load(srcPtr=");
        srcPtr.dump(stream);
        stream.print(", dst=");
        dst.dump(stream);
        stream.print(")");
    }
}
