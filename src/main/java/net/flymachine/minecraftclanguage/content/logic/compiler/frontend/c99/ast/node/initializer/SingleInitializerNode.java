package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.initializer;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.AstNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.expression.ExpressionNode;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;

public final class SingleInitializerNode extends AstNode implements InitializerNode {
    public @NotNull ExpressionNode exp;

    public SingleInitializerNode(@NotNull ExpressionNode exp) {
        super(exp.wholeLoc);
        this.exp = exp;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return exp.accept(visitor);
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }

        stream.println("SingleInitializerNode(");

        indent(stream, indentLevel + 1);
        stream.print("exp=");
        exp.dump(stream, indentLevel + 1, false);
        stream.println(',');

        indent(stream, indentLevel);
        stream.print(')');
    }
}
