package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;

public final class TypedefType extends Type {
    public final String typename;
    public final Type underlyingType;

    public TypedefType(String typename, Type underlyingType) {
        this(typename, underlyingType, false);
    }

    public TypedefType(String typename, Type underlyingType, boolean isConst) {
        super(isConst);
        this.typename = typename;
        this.underlyingType = underlyingType.unwrap();
    }

    @Override
    TypeKind kind() {
        return TypeKind.TYPEDEF;
    }

    @Override
    public Type unwrap() {
        return isConst ? underlyingType.addConst() : underlyingType;
    }

    @Override
    public boolean containsAlias() {
        return true;
    }

    @Override
    public String format(String declarator) {
        String name = isConst ? "const " + typename : typename;
        if (declarator.isEmpty()) { return name; }
        if (declarator.startsWith("[")) { return name + declarator; }
        return name + " " + declarator;
    }

    @Override
    public Type setConst(boolean isConst) {
        return this.isConst == isConst ? this : new TypedefType(typename, underlyingType, isConst);
    }

    @Override
    public boolean isCompatible(Type other) {
        // 不能直接比 const，不同类型的 const 语义不同
        return this.unwrap().isCompatible(other.unwrap());
    }

    @Override
    public Type merge(Type other) {
        return this.unwrap().merge(other.unwrap());
    }

    @Override
    public boolean isComplete() {
        return underlyingType.isComplete();
    }

    @Override
    public long sizeof() {
        return underlyingType.sizeof();
    }

    @Override
    public long alignof() {
        return underlyingType.alignof();
    }

    @Override
    public AsmType toAsmType() {
        return underlyingType.toAsmType();
    }

    @Override
    public boolean isBasic() {
        return underlyingType.isBasic();
    }

    @Override
    public boolean isVoid() {
        return underlyingType.isVoid();
    }

    @Override
    public boolean isFunction() {
        return underlyingType.isFunction();
    }

    @Override
    public boolean isPointer() {
        return underlyingType.isPointer();
    }

    @Override
    public boolean isArray() {
        return underlyingType.isArray();
    }

    @Override
    public boolean isStruct() {
        return underlyingType.isStruct();
    }

    @Override
    public boolean isUnion() {
        return underlyingType.isUnion();
    }

    @Override
    public boolean isEnum() {
        return underlyingType.isEnum();
    }
}
