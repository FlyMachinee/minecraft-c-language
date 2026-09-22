package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantUnsignedLong;
import org.jetbrains.annotations.NotNull;

public final class ArrayType extends Type {
    public final @NotNull Type elementType;
    public final @NotNull ConstantUnsignedLong size;

    public ArrayType(@NotNull Type elementType, @NotNull ConstantUnsignedLong size) {
        super(false);
        this.elementType = elementType;
        this.size = size;
    }

    @Override
    public String format(String declarator) {
        String newDecl = Type.wrapIfPointer(declarator) + "[" + (size.value() > 0 ? size.value() : "") + "]";
        return elementType.format(newDecl);
    }

    @Override
    public Type setConst(boolean isConst) {
        if (isConst) {
            return new ArrayType(elementType.addConst(), size);
        } else {
            return this;
        }
    }

    @Override
    public boolean isCompatible(Type other) {
        if (!(other instanceof ArrayType o)) {
            return false;
        }
        // 其元素类型兼容
        if (!elementType.isCompatible(o.elementType)) {
            return false;
        }
        // 若都拥有常量大小，则大小相同
        // 未知边界数组与任何兼容元素类型的数组兼容
        return size.value() == 0 || o.size.value() == 0 || size.value() == o.size.value();
    }

    @Override
    public boolean isComplete() {
        return size.value() != 0;
    }

    @Override
    public boolean isArithmetic() {
        return false;
    }

    @Override
    public boolean isScalar() {
        return false;
    }

    @Override
    public boolean isInteger() {
        return false;
    }

    @Override
    public boolean isReal() {
        return false;
    }

    @Override
    public boolean isAggregate() {
        return true;
    }

    @Override
    public long sizeof() {
        if (size.value() == 0) {
            throw new UnsupportedOperationException("array size is unknown");
        }
        return elementType.sizeof() * size.value();
    }

    @Override
    public AsmType toAsmType() {
        return null;
    }

    @Override
    public <R> R accept(TypeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
