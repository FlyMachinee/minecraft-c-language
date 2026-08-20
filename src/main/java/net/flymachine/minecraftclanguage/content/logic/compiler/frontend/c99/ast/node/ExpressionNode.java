package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public abstract class ExpressionNode extends AstNode {
    public Type expType = null;

    protected ExpressionNode(SourceLocation wholeLocation) {
        super(wholeLocation);
    }

    public abstract ExpressionVisitor.ExpEvalResult accept(ExpressionVisitor visitor);

    public abstract ExpressionBoolVisitor.BoolGenResult accept(
        ExpressionBoolVisitor visitor, String jumpTarget, boolean inverse);

    /**
     * 会在结尾加换行
     */
    protected void dumpExpType(PrintStream stream, int indentLevel) {
        if (expType != null) {
            indent(stream, indentLevel);
            stream.append("expType=").append(expType.toString()).append(',');
            stream.println();
        }
    }
}

