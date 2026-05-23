package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.io.PrintStream;

public class TacReturn implements TacInstruction {
    public TacValue value;

    public TacReturn(TacValue value) {
        this.value = value;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        stream.print("Return(");
        if (value != null) {
            value.dump(stream);
        }
        stream.print(")");
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
