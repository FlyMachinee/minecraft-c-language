package net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.IdentifierNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.expression.VariableNode;

import java.io.PrintStream;

public class TacVariable implements TacValue {
    public String name;

    public TacVariable(String name) {
        this.name = name;
    }

    public TacVariable(IdentifierNode id) {
        this.name = id.name;
    }

    public TacVariable(VariableNode var) {
        this.name = var.id.name;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        stream.append("Var(").append(name).append(")");
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        stream.append(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (!(obj instanceof TacVariable o)) { return false; }
        return name.equals(o.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
