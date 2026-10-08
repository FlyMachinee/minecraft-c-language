package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ConstantEvalVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public final class CommaExpressionNode extends ExpressionNode {
    public SourceLocation commaLocation;
    public ExpressionNode lhs;
    public ExpressionNode rhs;

    public CommaExpressionNode(SourceLocation commaLocation, ExpressionNode lhs, ExpressionNode rhs) {
        super(SourceLocation.concat(lhs.wholeLoc, rhs.wholeLoc));
        this.commaLocation = commaLocation;
        this.lhs = lhs;
        this.rhs = rhs;
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
    public Either<Constant, SourceLocation> accept(ConstantEvalVisitor visitor,
        ConstantEvalVisitor.ConstantCategory category) {
        return Either.right(wholeLoc);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {

    }
}
