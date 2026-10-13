package net.flymachine.minecraftclanguage.content.logic.compiler.common.type.record;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class UnionInfo extends RecordInfo {

    public UnionInfo(@Nullable String tag) {
        super(tag);
    }

    @Override
    protected void computeLayout(List<Field> fields) {
        long unionSize = 0;
        long unionAlignment = 1;
        for (Field field : fields) {
            // 联合体中所有字段偏移均为 0
            field.offset = 0;
            unionSize = Math.max(unionSize, field.type.sizeof());
            unionAlignment = Math.max(unionAlignment, field.type.alignof());
        }
        // 大小向上对齐到对齐要求
        unionSize = (unionSize + unionAlignment - 1) / unionAlignment * unionAlignment;
        this.size = unionSize;
        this.alignment = unionAlignment;
    }
}
