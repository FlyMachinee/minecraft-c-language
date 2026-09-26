package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.io.PrintStream;

public class TacStore implements TacInstruction {
    public TacValue src;
    public TacAddressDescriptor dstAddr;

    public TacStore(TacValue src, TacAddressDescriptor dstAddr) {
        this.src = src;
        this.dstAddr = dstAddr;
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
        stream.print("Store(src=");
        src.dump(stream);
        stream.print(", dstAddr=");
        dstAddr.dump(stream);
        stream.print(")");
    }
}
