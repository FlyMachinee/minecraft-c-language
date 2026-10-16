package net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacValue;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;

public class TacReturn implements TacInstruction {
    public @Nullable TacValue value;

    public TacReturn(@Nullable TacValue value) {
        this.value = value;
    }

    public TacReturn() {
        this.value = null;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        stream.print("Return(");
        if (value != null) {
            value.dump(stream);
        }
        stream.print(")");
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        stream.print("return");
        if (value != null) {
            stream.print(" ");
            value.dumpPretty(stream);
        }
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
