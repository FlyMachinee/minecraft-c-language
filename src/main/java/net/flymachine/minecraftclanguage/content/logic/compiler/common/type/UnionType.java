package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.record.UnionInfo;
import org.jetbrains.annotations.Nullable;

public final class UnionType extends RecordType<UnionInfo> {

    public UnionType(@Nullable String tag, boolean isConst) {
        super(new UnionInfo(tag), isConst);
    }

    public UnionType(UnionInfo info, boolean isConst) {
        super(info, isConst);
    }

    @Override
    TypeKind kind() {
        return TypeKind.UNION;
    }

    @Override
    public String format(String declarator) {
        String name = isConst ? "const union " : "union ";
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
        return this.isConst == isConst ? this : new UnionType(info, isConst);
    }
}
