package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.record.StructInfo;
import org.jetbrains.annotations.Nullable;

public final class StructType extends RecordType<StructInfo> {

    public StructType(@Nullable String tag, boolean isConst) {
        super(new StructInfo(tag), isConst);
    }

    public StructType(StructInfo info, boolean isConst) {
        super(info, isConst);
    }

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
}
