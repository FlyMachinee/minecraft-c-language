package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ConstantEvalVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public final class PointerMemberAccessNode extends ExpressionNode {
    public ExpressionNode pointer;
    public IdentifierNode member;
    public SourceLocation arrowLocation;

    public PointerMemberAccessNode(ExpressionNode pointer, SourceLocation arrowLocation, IdentifierNode member) {
        super(SourceLocation.concat(pointer.wholeLoc, member.wholeLoc));
        this.pointer = pointer;
        this.arrowLocation = arrowLocation;
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
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }

        stream.println("PointerMemberAccessNode(");

        indent(stream, indentLevel + 1);
        stream.print("pointer=");
        pointer.dump(stream, indentLevel + 1, false);
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
