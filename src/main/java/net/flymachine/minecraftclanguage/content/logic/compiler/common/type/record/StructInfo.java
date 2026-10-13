package net.flymachine.minecraftclanguage.content.logic.compiler.common.type.record;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class StructInfo extends RecordInfo {

    public StructInfo(@Nullable String tag) {
        super(tag);
    }

    @Override
    protected void computeLayout(List<Field> fields) {
        long structSize = 0;
        long structAlignment = 1;
        for (Field field : fields) {
            long fieldAlignment = field.type.alignof();
            long fieldOffset = (structSize + fieldAlignment - 1) / fieldAlignment * fieldAlignment;
            field.offset = fieldOffset;
            structSize = fieldOffset + field.type.sizeof();
            structAlignment = Math.max(structAlignment, fieldAlignment);
        }
        structSize = (structSize + structAlignment - 1) / structAlignment * structAlignment;
        this.size = structSize;
        this.alignment = structAlignment;
    }
}
