package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.List;

public final class CompoundInitializerNode extends AstNode implements InitializerNode {
    public @NotNull List<DesignationInitializerNode> inits;

    public CompoundInitializerNode(SourceLocation wholeLocation, @NotNull List<DesignationInitializerNode> inits) {
        super(wholeLocation);
        this.inits = inits;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        inits.forEach(initializer -> initializer.accept(visitor));
        return null;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }

        stream.println("CompoundInitializerNode(");
        indent(stream, indentLevel + 1);
        stream.println("list=[");

        for (DesignationInitializerNode initializer : inits) {
            initializer.dump(stream, indentLevel + 2, true);
            stream.println(',');
        }
        indent(stream, indentLevel + 1);
        stream.println(']');

        indent(stream, indentLevel);
        stream.print(')');
    }
}
