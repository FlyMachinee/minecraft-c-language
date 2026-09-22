package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;

public final class ArrayDesignatorNode extends AstNode implements DesignatorNode {
    public @NotNull ExpressionNode index; // 要求为整数常量表达式

    public ArrayDesignatorNode(SourceLocation wholeLocation, @NotNull ExpressionNode index) {
        super(wholeLocation);
        this.index = index;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return index.accept(visitor);
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        stream.print("[");
        index.dump(stream, indentLevel, false);
        stream.print("]");
    }
}
