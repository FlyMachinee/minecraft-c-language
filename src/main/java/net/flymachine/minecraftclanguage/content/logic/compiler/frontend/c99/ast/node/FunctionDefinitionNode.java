package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public class FunctionDefinitionNode extends AstNode {
    public IdentifierNode identifier;
    public CompoundStatementNode body;

    public FunctionDefinitionNode(IdentifierNode identifier, CompoundStatementNode body) {
        super(SourceLocation.concat(identifier.wholeLocation, body.wholeLocation));
        this.identifier = identifier;
        this.body = body;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("FunctionDefinitionNode(\n");
        stringBuilder.append("  ".repeat(indentLevel + 1));
        stringBuilder.append("name=\"").append(identifier.id).append("\",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("body=");
        body.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
