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
    public boolean isComplete() {
        return true;
    }

    @Override
    public boolean isArithmetic() {
        return false;
    }

    @Override
    public boolean isScalar() {
        return true;
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
    public long sizeof() {
        return 8;
    }

    @Override
    public AsmType toAsmType() {
        return AsmType.DWORD;
    }

    @Override
    public <R> R accept(TypeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public @NotNull String toString() {
        return referencedType + " *" + (isConst ? " const" : "");
    }
}
