package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;

public final class SubscriptNode extends ExpressionNode {
    public @NotNull ExpressionNode lhs;
    public @NotNull ExpressionNode rhs;
    public SourceLocation operatorLoc;

    public SubscriptNode(
        SourceLocation wholeLocation, @NotNull ExpressionNode lhs,
        @NotNull ExpressionNode rhs, SourceLocation operatorLoc) {

        super(wholeLocation);
        this.lhs = lhs;
        this.rhs = rhs;
        this.operatorLoc = operatorLoc;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public ExpressionBoolVisitor.BoolGenResult accept(ExpressionBoolVisitor visitor, String jumpTarget,
        boolean inverse) {
        return visitor.visit(this, jumpTarget, inverse);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        stream.println("SubscriptNode(");

        indent(stream, indentLevel + 1);
        stream.print("lhs=");
        lhs.dump(stream, indentLevel + 1, false);
        stream.println(',');

        indent(stream, indentLevel + 1);
        stream.print("rhs=");
        rhs.dump(stream, indentLevel + 1, false);
        stream.println(',');

        dumpExpType(stream, indentLevel + 1);

        indent(stream, indentLevel);
        stream.print(')');
    }
}
