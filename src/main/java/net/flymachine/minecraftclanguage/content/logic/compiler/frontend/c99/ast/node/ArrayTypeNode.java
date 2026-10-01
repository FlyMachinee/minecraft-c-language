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

    /**
     * 检查数组类型维度中是否包含 const 限定符，仅在函数形参类型衰减时使用
     * <p>
     * 注意与顶层 const 的区别
     */
    public boolean containsConst() {
        return constLoc != null;
    }

    @Override
    public ArrayType getType() {
        Type pointeeType = elementType.getType();
        if (constQualifier != null) {
            pointeeType = pointeeType.addConst();
        }
        ConstantUnsignedLong size =
            this.size == null ||
            !(this.size instanceof ConstantNode) ? ConstantUnsignedLong.ZERO : ((ConstantNode) this.size).value.toUnsignedLong();
        return new ArrayType(pointeeType, size);
    }

    @Override
    protected String format(String declarator) {
        String newDecl;
        if (size == null) {
            newDecl = Type.wrapIfPointer(declarator) + "[" + (constLoc == null ? "" : "const") + "]";
        } else if (size instanceof ConstantNode constSize) {
            newDecl = Type.wrapIfPointer(declarator) + "[" + (constLoc == null ? "" : "const ") +
                      constSize.value.toUnsignedLong().value() + "]";
        } else {
            newDecl = Type.wrapIfPointer(declarator) + "[" + (constLoc == null ? "" : "const ") + "...]";
        }
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
