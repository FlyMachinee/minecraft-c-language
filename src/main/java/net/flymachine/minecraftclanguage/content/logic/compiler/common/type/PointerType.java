package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import org.jetbrains.annotations.NotNull;

public final class PointerType extends Type {

    private final @NotNull Type referencedType;

    public PointerType(@NotNull Type referencedType) {
        super(false);
        this.referencedType = referencedType;
    }

    public PointerType(@NotNull Type referencedType, boolean isConst) {
        super(isConst);
        this.referencedType = referencedType;
    }

    public @NotNull Type referencedType() {
        return this.referencedType;
    }

    @Override
    TypeKind kind() {
        return TypeKind.POINTER;
    }

    @Override
    public String format(String declarator) {
        String newDeclarator = "*" + (isConst() ? " const" : "") + declarator;
        return referencedType.format(newDeclarator);
    }

    @Override
    public PointerType setConst(boolean isConst) {
        if (this.isConst == isConst) {
            return this;
        } else {
            return new PointerType(referencedType, isConst);
        }
    }

    @Override
    public boolean isCompatible(Type other) {
        if (this.isConst() != other.isConst()) {
            return false;
        }
        if (!(other instanceof PointerType o)) {
            return false;
        }
        return referencedType.isCompatible(o.referencedType);
    }

    @Override
    public Type merge(Type other) {
        if (!this.isCompatible(other)) {
            return ErrorType.INSTANCE;
        }
        return new PointerType(referencedType.merge(((PointerType) other).referencedType), this.isConst());
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public long sizeof() {
        return 8;
    }

    @Override
    public long alignof() {
        return 8;
    }

    @Override
    public AsmType toAsmType() {
        return AsmType.DWORD;
    }
}
