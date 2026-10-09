package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import org.jetbrains.annotations.Nullable;

public final class StructType extends Type {

    private final StructInfo info;

    public StructType(@Nullable String tag, boolean isConst) {
        super(isConst);
        this.info = new StructInfo(tag);
    }

    public StructType(StructInfo info, boolean isConst) {
        super(isConst);
        this.info = info;
    }

    public StructInfo info() { return info; }

    public @Nullable String tag() { return info.tag(); }

    public int id() { return info.id(); }

    @Override
    TypeKind kind() {
        return TypeKind.STRUCT;
    }

    @Override
    public String format(String declarator) {
        String name = isConst ? "const struct " : "struct ";
        if (info.tag() != null) {
            name += info.tag();
        } else {
            name += " <anonymous>";
        }

        if (declarator.isEmpty()) {
            return name;
        }
        if (declarator.startsWith("[")) {
            return name + declarator;
        }
        return name + " " + declarator;
    }

    @Override
    public Type setConst(boolean isConst) {
        return this.isConst == isConst ? this : new StructType(info, isConst);
    }

    @Override
    public boolean isCompatible(Type other) {
        if (this.isConst() != other.isConst()) {
            return false;
        }
        return (other instanceof StructType otherStruct) && this.info.id() == otherStruct.info.id();
    }

    @Override
    public Type merge(Type other) {
        if (!this.isCompatible(other)) {
            return ErrorType.INSTANCE;
        }
        return this;
    }

    @Override
    public boolean isComplete() {
        return info.isComplete();
    }

    @Override
    public long sizeof() {
        return info.sizeof();
    }

    @Override
    public long alignof() {
        return info.alignof();
    }

    @Override
    public AsmType toAsmType() {
        return null;
    }
}
