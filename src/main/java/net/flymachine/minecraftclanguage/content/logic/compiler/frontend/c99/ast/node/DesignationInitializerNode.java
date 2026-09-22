package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.List;

public final class DesignationInitializerNode extends AstNode {
    public @NotNull List<DesignatorNode> designators;
    public @NotNull InitializerNode initializer;

    public DesignationInitializerNode(SourceLocation wholeLocation, @NotNull List<DesignatorNode> designators,
        @NotNull InitializerNode initializer) {
        super(wholeLocation);
        this.designators = designators;
        this.initializer = initializer;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        designators.forEach(node -> node.accept(visitor));
        initializer.accept(visitor);
        return null;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }

        for (DesignatorNode designator : designators) {
            designator.dump(stream, indentLevel, false);
        }
        if (!designators.isEmpty()) {
            stream.print('=');
        }
        initializer.dump(stream, indentLevel, false);
    }
}
