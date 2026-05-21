package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;

import java.io.PrintStream;

public final class ForInitDeclarationNode extends AstNode implements ForInitNode {
    public DeclarationNode decl;

    public ForInitDeclarationNode(DeclarationNode decl) {
        super(decl.wholeLoc);
        this.decl = decl;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return decl.accept(visitor);
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        decl.dump(stream, indentLevel, indentFirstLine);
    }
}
