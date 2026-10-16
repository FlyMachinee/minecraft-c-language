package net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacAddressDescriptor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacValue;

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
            indent(stream, indentLevel);
        }
        stream.print("Load(srcAddr=");
        srcAddr.dump(stream);
        stream.print(", dst=");
        dst.dump(stream);
        stream.print(")");
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        // dst = *srcAddr
        dst.dumpPretty(stream);
        stream.print(" = *");
        srcAddr.dumpPretty(stream);
    }
}
