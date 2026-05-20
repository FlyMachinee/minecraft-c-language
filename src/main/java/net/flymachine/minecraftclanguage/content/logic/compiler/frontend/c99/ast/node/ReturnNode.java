package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.StatementVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class ReturnNode extends StatementNode {
    public ExpressionNode exp;
    public SourceLocation returnLocation;

    public ReturnNode(SourceLocation returnLocation, ExpressionNode exp) {
        super(SourceLocation.concat(returnLocation, exp.wholeLoc));
        this.returnLocation = returnLocation;
        this.exp = exp;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("ReturnNode(\n");
        if (isLabeled()) {
            stringBuilder.append("  ".repeat(indentLevel + 1));
            genFormatedStringForLabels(stringBuilder);
            stringBuilder.append(",\n");
        }
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("exp=");
        exp.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel));
        stringBuilder.append(")\n");
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }
}

