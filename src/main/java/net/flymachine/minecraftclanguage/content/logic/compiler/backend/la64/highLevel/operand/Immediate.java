package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand;

import java.io.PrintStream;
public record Immediate(long value) implements HighLevelOperand {
    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) stream.print("  ".repeat(indentLevel));
        stream.print(value);
    }
}
