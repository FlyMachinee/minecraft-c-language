package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

import java.io.PrintStream;
import java.util.Arrays;

/**
 * 可能会使用的寄存器：T1 T2
 */
public class CopyByteArray implements HighLevelInstruction {
    public final byte[] data;
    public HighLevelOperand dst;

    public CopyByteArray(byte[] data, HighLevelOperand dst) {
        this.data = data;
        this.dst = dst;
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        dst.dumpPretty(stream);
        stream.print(" = ");
        stream.print(Arrays.toString(data));
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
