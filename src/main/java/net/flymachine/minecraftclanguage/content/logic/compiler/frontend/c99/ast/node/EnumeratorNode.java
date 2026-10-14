package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;

public final class EnumeratorNode extends AstNode {
    public final @NotNull IdentifierNode name;
    public final @Nullable ExpressionNode value;

    public EnumeratorNode(@NotNull IdentifierNode name, @Nullable ExpressionNode value) {
        super(value == null ? name.wholeLoc : SourceLocation.concat(name.wholeLoc, value.wholeLoc));
        this.name = name;
        this.value = value;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) { }
}
