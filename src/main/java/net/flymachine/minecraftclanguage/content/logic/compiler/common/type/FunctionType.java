package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class FunctionType extends Type {
    private final @NotNull Type returnType;
    private final @NotNull List<Type> parameterTypes;

    public FunctionType(@NotNull Type returnType, @NotNull List<Type> parameterTypes) {
        super(false);
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
    }

    @Override
    public boolean isConst() {
        return false;
    }

    public @NotNull Type returnType() {
        return this.returnType;
    }

    public @NotNull List<Type> parameterTypes() {
        return this.parameterTypes;
    }

    @Override
    public FunctionType setConst(boolean isConst) {
        return this;
    }

    @Override
    public boolean isCompatible(Type other) {
        if (!(other instanceof FunctionType o)) {
            return false;
        }
        if (!returnType.isCompatible(o.returnType)) {
            return false;
        }
        if (hasNoParameters() && o.hasNoParameters()) {
            return true;
        }
        if (parameterTypes.size() != o.parameterTypes.size()) {
            return false;
        }
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (!parameterTypes.get(i).removeQualifiers().isCompatible(o.parameterTypes.get(i).removeQualifiers())) {
                return false;
            }
        }
        return true;
    }

    public Type parameterType(int i) {
        return parameterTypes.get(i);
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
    public long sizeof() {
        return -1;
    }

    @Override
    public AsmType toAsmType() {
        throw new UnsupportedOperationException("toAsmType(function) is not defined");
    }

    @Override
    public <R> R accept(TypeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public @NotNull String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(returnType.toString()).append("(");
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(parameterTypes.get(i).toString());
        }
        sb.append(")");
        return sb.toString();
    }

    public boolean hasNoParameters() {
        if (parameterTypes.isEmpty()) {
            return true;
        }
        if (parameterTypes.size() == 1) {
            Type paramType = parameterTypes.get(0);
            return paramType.isVoid();
        }
        return false;
    }

    public int parameterCount() {
        if (parameterTypes.size() == 1) {
            Type paramType = parameterTypes.get(0);
            if (paramType.isVoid()) {
                return 0;
            }
        }
        return parameterTypes.size();
    }
}
