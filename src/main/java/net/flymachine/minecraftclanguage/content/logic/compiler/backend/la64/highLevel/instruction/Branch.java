package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import java.io.PrintStream;

public class Branch implements HighLevelInstruction {
    public final String target;

    public Branch(String target) {
        this.target = target;
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        stream.print("goto ");
        stream.print(target);
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
