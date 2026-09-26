package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.io.PrintStream;

public class TacLoad implements TacInstruction {
    public TacAddressDescriptor srcAddr;
    public TacValue dst;

    public TacLoad(TacAddressDescriptor srcAddr, TacValue dst) {
        this.srcAddr = srcAddr;
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
        stream.print("Load(srcAddr=");
        srcAddr.dump(stream);
        stream.print(", dst=");
        dst.dump(stream);
        stream.print(")");
    }
}
