package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;

public final class InitDeclaratorNode extends AstNode {
    public final TypeNode finalType;
    public IdentifierNode id;
    public @Nullable ExpressionNode init;

    public InitDeclaratorNode(
        SourceLocation wholeLocation, TypeNode finalType, IdentifierNode id, @Nullable ExpressionNode init) {
        super(wholeLocation);
        this.finalType = finalType;
        this.id = id;
        this.init = init;
    }

    public InitDeclaratorNode(
        SourceLocation wholeLocation, TypeNode finalType, IdentifierNode id) {
        super(wholeLocation);
        this.finalType = finalType;
        this.id = id;
        this.init = null;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) { }
}
