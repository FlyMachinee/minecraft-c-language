package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;

public final class MemberDesignatorNode extends AstNode implements DesignatorNode {
    public final @NotNull IdentifierNode member;

    public MemberDesignatorNode(SourceLocation wholeLocation, @NotNull IdentifierNode member) {
        super(wholeLocation);
        this.member = member;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        stream.append('.').append(member.name);
    }
}
