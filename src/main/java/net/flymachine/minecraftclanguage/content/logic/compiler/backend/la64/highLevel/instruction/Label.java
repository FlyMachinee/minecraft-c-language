package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import java.io.PrintStream;
public class Label implements HighLevelInstruction {
    public final String name;

    public Label(String name) {
        this.name = name;
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        stream.print(name);
        stream.print(":");
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
