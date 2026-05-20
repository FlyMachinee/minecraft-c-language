package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacValue;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class AssignmentNode extends ExpressionNode {
    public AssignmentOperatorNode op;
    public ExpressionNode lhs;
    public ExpressionNode rhs;

    public AssignmentNode(AssignmentOperatorNode op, ExpressionNode lhs, ExpressionNode rhs) {
        super(SourceLocation.concat(lhs.wholeLoc, rhs.wholeLoc));
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("AssignmentNode(\n").append("  ".repeat(indentLevel + 1)).append("op=");
        op.genFormattedString(stringBuilder);
        stringBuilder.append("\n").append("  ".repeat(indentLevel + 1)).append("lhs=");
        lhs.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("rhs=");
        rhs.genFormattedString(stringBuilder, indentLevel + 1, false);
        if (expType != null) {
            stringBuilder.append("  ".repeat(indentLevel + 1)).append("expType=").append(expType).append("\n");
        }
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
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
