package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.IdentifierNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.VariableNode;

import java.io.PrintStream;

public class TacVariable implements TacValue {
    public String name;

    public TacVariable(String name) {
        this.name = name;
    }

    public TacVariable(IdentifierNode id) {
        this.name = id.id;
    }

    public TacVariable(VariableNode var) {
        this.name = var.id.id;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        stream.append("Var(").append(name).append(")");
    }
}
