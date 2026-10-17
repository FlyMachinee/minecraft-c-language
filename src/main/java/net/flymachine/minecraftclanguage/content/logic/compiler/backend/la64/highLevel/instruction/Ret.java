package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import java.io.PrintStream;

public class Ret implements HighLevelInstruction {
    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        stream.print("ret");
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
