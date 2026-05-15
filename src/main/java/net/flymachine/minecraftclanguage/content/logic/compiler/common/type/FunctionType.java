package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record FunctionType(Type returnType, List<Type> parameterTypes) implements Type {

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
            if (!parameterTypes.get(i).isCompatible(o.parameterTypes.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isAssignableFrom(Type other) {
        return false;
    }

    @Override
    public boolean isComplete() {
        return true;
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
            if (paramType instanceof BasicType basicType) {
                return basicType.getKind() == BasicType.Kind.VOID;
            }
        }
        return false;
    }
}
