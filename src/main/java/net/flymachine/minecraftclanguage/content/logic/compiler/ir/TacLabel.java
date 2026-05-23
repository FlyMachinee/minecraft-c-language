package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.io.PrintStream;

public class TacLabel implements TacInstruction {
    public String name;

    public TacLabel(String name) {
        this.name = name;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        stream.append("Label(").append(name).append(")");
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
