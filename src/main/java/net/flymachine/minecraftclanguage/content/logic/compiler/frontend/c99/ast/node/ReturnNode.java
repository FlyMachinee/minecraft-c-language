package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public class ReturnNode extends StatementNode {
    public ExpressionNode expression;
    public SourceLocation returnLocation;

    public ReturnNode(SourceLocation returnLocation, ExpressionNode expression) {
        super(SourceLocation.concat(returnLocation, expression.wholeLocation));
        this.returnLocation = returnLocation;
        this.expression = expression;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("ReturnNode(\n");
        expression.genFormattedString(stringBuilder, indentLevel + 1, true);
        stringBuilder.append("  ".repeat(indentLevel));
        stringBuilder.append(")\n");
    }
}

