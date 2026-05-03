package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.util.List;

public class FunctionDefinitionNode extends AstNode {
    public IdentifierNode identifier;
    public List<BlockItemNode> body;

    public FunctionDefinitionNode(IdentifierNode identifier, List<BlockItemNode> body) {
        super(body.isEmpty() ? identifier.wholeLocation
                  : SourceLocation.concat(identifier.wholeLocation, body.get(body.size() - 1).wholeLocation));
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
        stringBuilder.append("  ".repeat(indentLevel + 1));
        stringBuilder.append("body=[\n");
        for (BlockItemNode item : body) {
            item.genFormattedString(stringBuilder, indentLevel + 2, true);
            stringBuilder.append("  ".repeat(indentLevel + 2)).append(",\n");
        }
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("]\n");
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
