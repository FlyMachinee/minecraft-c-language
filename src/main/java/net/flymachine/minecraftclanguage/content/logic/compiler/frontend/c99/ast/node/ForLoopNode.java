package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

public final class ForLoopNode extends StatementNode {
    public @Nullable ForInitNode init;
    public @Nullable ExpressionNode cond;
    public @Nullable ExpressionNode step;
    public StatementNode body;
    public String loopLabel;

    public ForLoopNode(
        SourceLocation wholeLocation, @Nullable ForInitNode init,
        @Nullable ExpressionNode cond, @Nullable ExpressionNode step, StatementNode body) {
        super(wholeLocation);
        this.init = init;
        this.cond = cond;
        this.step = step;
        this.body = body;
    }

    @Override
    public boolean containsActiveLabel() {
        if (super.containsActiveLabel()) {
            return true;
        }
        return body.containsActiveLabel();
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("ForLoopNode(\n");
        if (loopLabel != null) {
            stringBuilder.append("  ".repeat(indentLevel + 1)).append("loop=").append(loopLabel).append(",\n");
        }
        if (init != null) {
            stringBuilder.append("  ".repeat(indentLevel + 1)).append("init=");
            if (init instanceof DeclarationNode decl) {
                decl.genFormattedString(stringBuilder, indentLevel + 1, false);
            } else if (init instanceof ExpressionNode expr) {
                expr.genFormattedString(stringBuilder, indentLevel + 1, false);
            } else {
                throw new RuntimeException("Unknown init type: " + init.getClass().getName());
            }
        }
        if (cond != null) {
            stringBuilder.append("  ".repeat(indentLevel + 1)).append("cond=");
            cond.genFormattedString(stringBuilder, indentLevel + 1, false);
        }
        if (step != null) {
            stringBuilder.append("  ".repeat(indentLevel + 1)).append("step=");
            step.genFormattedString(stringBuilder, indentLevel + 1, false);
        }
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("body=");
        body.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
