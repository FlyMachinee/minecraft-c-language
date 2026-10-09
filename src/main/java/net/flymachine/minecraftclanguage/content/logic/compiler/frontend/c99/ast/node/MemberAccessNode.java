package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ConstantEvalVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public final class MemberAccessNode extends ExpressionNode {
    public ExpressionNode base;
    public IdentifierNode member;
    public SourceLocation dotLocation;

    public MemberAccessNode(ExpressionNode base, SourceLocation dotLocation, IdentifierNode member) {
        super(SourceLocation.concat(base.wholeLoc, member.wholeLoc));
        this.base = base;
        this.dotLocation = dotLocation;
        this.member = member;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public ExpressionBoolVisitor.BoolGenResult accept(
        ExpressionBoolVisitor visitor, String jumpTarget, boolean inverse) {
        return visitor.visit(this, jumpTarget, inverse);
    }

    @Override
    public Either<Constant, SourceLocation> accept(
        ConstantEvalVisitor visitor, ConstantEvalVisitor.ConstantCategory category) {
        return Either.right(wholeLoc);
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

        stream.println("MemberAccessNode(");

        indent(stream, indentLevel + 1);
        stream.print("base=");
        base.dump(stream, indentLevel + 1, false);
        stream.println(',');

        indent(stream, indentLevel + 1);
        stream.print("member=");
        member.dump(stream, indentLevel + 1, false);
        stream.println(',');

        dumpExpType(stream, indentLevel + 1);

        indent(stream, indentLevel);
        stream.print(')');
    }
}
