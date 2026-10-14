package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;

import javax.annotation.Nullable;

public final class EnumType extends Type {
    private final EnumInfo info;
    private static final BasicType UNDERLYING_TYPE = BasicType.INT;

    public EnumType(@Nullable String tag, boolean isConst) {
        super(isConst);
        this.info = new EnumInfo(tag);
    }

    public EnumType(EnumInfo info, boolean isConst) {
        super(isConst);
        this.info = info;
    }

    public EnumInfo info() { return info; }

    public @Nullable String tag() { return info.tag(); }

    public int id() { return info.id(); }

    public void populate() { info.populate(); }

    @Override
    TypeKind kind() { return TypeKind.ENUM; }

    @Override
    public String format(String declarator) {
        String name = isConst ? "const enum " : "enum ";
        name += info.tag() != null ? info.tag() : "<anonymous>";
        if (declarator.isEmpty()) { return name; }
        if (declarator.startsWith("[")) { return name + declarator; }
        return name + " " + declarator;
    }

    @Override
    public Type setConst(boolean isConst) {
        return this.isConst == isConst ? this : new EnumType(info, isConst);
    }

    @Override
    public boolean isCompatible(Type other) {
        if (this.isConst() != other.isConst()) { return false; }
        // 枚举与相同枚举类型兼容
        if (other instanceof EnumType otherEnum) {
            return this.info.id() == otherEnum.info.id();
        }
        // 与底层整数类型兼容
        return UNDERLYING_TYPE.isCompatible(other.removeQualifiers());
    }

    @Override
    public Type underlyingType() {
        return UNDERLYING_TYPE.setConst(this.isConst());
    }

    @Override
    public Type merge(Type other) {
        return this.isCompatible(other) ? this : ErrorType.INSTANCE;
    }

    @Override
    public boolean isComplete() { return info.isComplete(); }

    @Override
    public long sizeof() { return UNDERLYING_TYPE.sizeof(); }

    @Override
    public long alignof() { return UNDERLYING_TYPE.alignof(); }

    @Override
    public AsmType toAsmType() { return UNDERLYING_TYPE.toAsmType(); }
}
