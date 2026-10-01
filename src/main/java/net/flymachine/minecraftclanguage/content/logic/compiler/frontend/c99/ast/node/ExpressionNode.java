package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ConstantEvalVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public abstract class ExpressionNode extends AstNode {
    public Type expType = null;

    protected ExpressionNode(SourceLocation wholeLocation) {
        super(wholeLocation);
    }

    public abstract <T> T accept(ExpressionVisitor<T> visitor);

    public abstract ExpressionBoolVisitor.BoolGenResult accept(
        ExpressionBoolVisitor visitor, String jumpTarget, boolean inverse);

    public abstract Either<Constant, SourceLocation> accept(
        ConstantEvalVisitor visitor, ConstantEvalVisitor.ConstantCategory category);

    /**
     * 会在结尾加换行
     */
    protected void dumpExpType(PrintStream stream, int indentLevel) {
        if (expType != null) {
            indent(stream, indentLevel);
            stream.append("expType=").append(expType.typename()).append(',');
            stream.println();
        }
    }
}

