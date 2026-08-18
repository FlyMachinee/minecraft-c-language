package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import org.jetbrains.annotations.NotNull;

public record PointerType(@NotNull Type referencedType) implements Type {
    @Override
    public boolean isCompatible(Type other) {
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
        return referencedType.toString() + " *";
    }
}
