package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantUnsignedLong;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.ArrayType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;

public final class ArrayTypeNode extends TypeNode {
    public @NotNull TypeNode elementType;
    public @Nullable ExpressionNode size; // 要求为整数常量表达式
    public @Nullable SourceLocation constLoc; // const，仅在函数参数中使用

    public ArrayTypeNode(SourceLocation wholeLocation, @NotNull TypeNode elementType, @Nullable ExpressionNode size) {
        super(wholeLocation);
        this.elementType = elementType;
        this.size = size;
        this.constLoc = null;
    }

    public ArrayTypeNode(SourceLocation wholeLocation, @NotNull TypeNode elementType, @Nullable ExpressionNode size,
        @Nullable SourceLocation constLoc) {

        super(wholeLocation);
        this.elementType = elementType;
        this.size = size;
        this.constLoc = constLoc;
    }

    public boolean containsConst() {
        return constLoc != null;
    }

    @Override
    public ArrayType getType() {
        Type pointeeType = elementType.getType();
        if (constQualifier != null) {
            pointeeType = pointeeType.addConst();
        }
        return new ArrayType(
            pointeeType,
            size == null ? ConstantUnsignedLong.ZERO : ((ConstantNode) size).value.toUnsignedLong());
    }

    @Override
    protected String format(String declarator) {
        long sizeValue = size == null ? 0 : ((ConstantNode) size).value.toUnsignedLong().value();
        String newDecl = Type.wrapIfPointer(declarator) + "[" + (constLoc == null ? "" : "const ") +
                         (sizeValue > 0 ? sizeValue : "") + "]";
        return elementType.format(newDecl);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        stream.print(typename());
    }
}
