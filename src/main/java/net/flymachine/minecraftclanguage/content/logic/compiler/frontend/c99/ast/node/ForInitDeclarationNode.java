package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;

public final class ForInitDeclarationNode extends AstNode implements ForInitNode {
    public DeclarationNode declaration;

    public ForInitDeclarationNode(DeclarationNode declaration) {
        super(declaration.wholeLocation);
        this.declaration = declaration;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return declaration.accept(visitor);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        declaration.genFormattedString(stringBuilder, indentLevel, indentFirstLine);
    }
}
