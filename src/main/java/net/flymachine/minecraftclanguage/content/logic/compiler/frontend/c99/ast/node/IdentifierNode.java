package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacValue;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class IdentifierNode extends ExpressionNode {
    public String id;

    public IdentifierNode(SourceLocation wholeLocation, String id) {
        super(wholeLocation);
        this.id = id;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("IdentifierNode(id=\"").append(id);
        if (expType != null) {
            stringBuilder.append("\", expType=").append(expType).append(")\n");
        } else {
            stringBuilder.append("\")\n");
        }
    }

    @Override
    public TacValue accept(ExpressionVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public ExpressionBoolVisitor.BoolGenResult accept(
        ExpressionBoolVisitor visitor, String jumpTarget, boolean inverse) {
        return visitor.visit(this, jumpTarget, inverse);
    }
}
