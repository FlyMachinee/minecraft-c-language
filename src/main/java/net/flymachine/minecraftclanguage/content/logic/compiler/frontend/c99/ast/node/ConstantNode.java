package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantInt;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantLong;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacValue;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public final class ConstantNode extends ExpressionNode {
    public Constant value;

    public ConstantNode(SourceLocation wholeLocation, Constant value) {
        super(wholeLocation);
        this.value = value;
    }

    public ConstantNode(SourceLocation wholeLocation, int intValue) {
        super(wholeLocation);
        this.value = new ConstantInt(intValue);
    }

    public ConstantNode(SourceLocation wholeLocation, long longValue) {
        super(wholeLocation);
        this.value = new ConstantLong(longValue);
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

        stream.append("ConstantNode(value=").append(value.toString());
        if (expType != null) {
            stream.append(", expType=").append(expType.toString()).append(')');
        } else {
            stream.print(')');
        }
    }

    @Override
    public String toString() {
        return String.valueOf(value);
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

