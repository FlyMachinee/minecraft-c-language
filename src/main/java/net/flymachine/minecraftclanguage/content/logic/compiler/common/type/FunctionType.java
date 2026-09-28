package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class FunctionType extends Type {
    private final @NotNull Type returnType;
    private final @NotNull List<Type> parameterTypes;

    public FunctionType(@NotNull Type returnType, @NotNull List<Type> parameterTypes) {
        super(false);
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
    }

    @Override
    TypeKind kind() {
        return TypeKind.FUNCTION;
    }

    @Override
    public String format(String declarator) {
        String params = parameterTypes.stream()
                                      .map(t -> t.format(""))
                                      .collect(Collectors.joining(", "));
        String newDecl = wrapIfPointer(declarator) + "(" + params + ")";
        return returnType.format(newDecl);
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
        // 其返回类型兼容
        if (!returnType.isCompatible(o.returnType)) {
            return false;
        }
        // 它们都使用形参列表，形参数量（包括省略号的使用）相同
        if (hasNoParameters() && o.hasNoParameters()) {
            return true;
        }
        if (parameterTypes.size() != o.parameterTypes.size()) {
            return false;
        }
        // 且其对应形参，在应用数组到指针和函数到指针类型调整，及剥除顶层限定符后，拥有相同类型
        // 类型衰减应在类型检查中完成
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (!parameterTypes.get(i).removeQualifiers().isCompatible(o.parameterTypes.get(i).removeQualifiers())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Type merge(Type other) {
        if (!this.isCompatible(other)) {
            return ErrorType.INSTANCE;
        }

        FunctionType o = (FunctionType) other;

        Type mergedReturnType = returnType.merge(o.returnType);

        if (hasNoParameters() && o.hasNoParameters()) {
            return new FunctionType(mergedReturnType, List.of());
        }

        List<Type> mergedParameterTypes = new ArrayList<>();
        for (int i = 0; i < parameterTypes.size(); i++) {
            mergedParameterTypes.add(
                parameterTypes.get(i).removeQualifiers().merge(o.parameterTypes.get(i).removeQualifiers())
                              .setConst(this.isConst()));
        }

        return new FunctionType(mergedReturnType, mergedParameterTypes);
    }

    public Type parameterType(int i) {
        return parameterTypes.get(i);
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public long sizeof() {
        throw new UnsupportedOperationException("sizeof(function) is not defined");
    }

    @Override
    public long alignof() {
        throw new UnsupportedOperationException("alignof(function) is not defined");
    }

    @Override
    public AsmType toAsmType() {
        throw new UnsupportedOperationException("toAsmType(function) is not defined");
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
