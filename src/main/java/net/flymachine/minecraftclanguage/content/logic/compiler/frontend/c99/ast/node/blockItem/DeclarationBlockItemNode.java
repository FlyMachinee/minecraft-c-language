package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.blockItem;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.AstNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.DeclarationNode;

import java.io.PrintStream;

public final class DeclarationBlockItemNode extends AstNode implements BlockItemNode {
    public DeclarationNode decl;

    public DeclarationBlockItemNode(DeclarationNode decl) {
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
