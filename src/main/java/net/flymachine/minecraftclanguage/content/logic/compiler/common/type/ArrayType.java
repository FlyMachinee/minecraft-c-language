package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantUnsignedLong;
import org.jetbrains.annotations.NotNull;

public final class ArrayType extends Type {
    private final @NotNull Type elementType;
    private final @NotNull ConstantUnsignedLong size;

    public ArrayType(@NotNull Type elementType, @NotNull ConstantUnsignedLong size) {
        super(false);
        this.elementType = elementType;
        this.size = size;
    }

    public Type elementType() {
        return this.elementType;
    }

    public ConstantUnsignedLong size() {
        return this.size;
    }

    public ArrayType withSize(ConstantUnsignedLong newSize) {
        return new ArrayType(elementType, newSize);
    }

    @Override
    TypeKind kind() {
        return TypeKind.ARRAY;
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
    public Type merge(Type other) {
        if (!this.isCompatible(other)) {
            return ErrorType.INSTANCE;
        }

        ArrayType o = (ArrayType) other;

        Type mergedElementType = elementType.merge(o.elementType);
        ConstantUnsignedLong mergedSize = new ConstantUnsignedLong(Math.max(size.value(), o.size.value()));
        return new ArrayType(mergedElementType, mergedSize);
    }

    @Override
    public boolean isComplete() {
        return size.value() != 0;
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
        return new AsmType.ByteArray(sizeof(), elementType.sizeof());
    }
}
