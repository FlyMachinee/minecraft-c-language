package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.record.Field;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.record.RecordInfo;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public sealed abstract class RecordType<I extends RecordInfo> extends Type permits StructType, UnionType {
    protected final I info;

    protected RecordType(I info, boolean isConst) {
        super(isConst);
        this.info = info;
    }

    public final I info() {
        return info;
    }

    public final @Nullable String tag() {
        return info.tag();
    }

    public final int id() {
        return info.id();
    }

    public final void populate(List<Field> fields) {
        info.populate(fields);
    }

    public final boolean hasField(String name) {
        return info.hasField(name);
    }

    public final Optional<Field> getField(String name) {
        Optional<Field> field = info.getField(name);
        if (field.isEmpty()) {
            return Optional.empty();
        }
        Field f = field.get();
        if (isConst()) {
            return Optional.of(new Field(f.name, f.type.setConst(true), f.offset));
        } else {
            return Optional.of(f);
        }
    }

    public final List<Field> fields() {
        List<Field> fs = info.fields();
        if (isConst()) {
            return fs.stream().map(f -> new Field(f.name, f.type.setConst(true), f.offset)).toList();
        } else {
            return fs;
        }
    }

    public final boolean modifiable() {
        if (!info.isComplete()) {
            throw new IllegalStateException("struct/union is not complete yet");
        }
        if (isConst()) {
            return false;
        }
        for (Field field : info.fields()) {
            if (field.type instanceof RecordType<?> nested) {
                if (!nested.modifiable()) {
                    return false;
                }
            } else if (field.type.isConst()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final boolean isCompatible(Type other) {
        other = other.unwrap();
        if (!(other instanceof RecordType<?> otherRecord)) {
            return false;
        }
        return this.id() == otherRecord.id() && this.isConst == otherRecord.isConst;
    }

    @Override
    public final Type merge(Type other) {
        if (!this.isCompatible(other)) {
            return ErrorType.INSTANCE;
        }
        return this;
    }

    @Override
    public final boolean isComplete() {
        return info.isComplete();
    }

    @Override
    public final long sizeof() {
        return info.sizeof();
    }

    @Override
    public final long alignof() {
        return info.alignof();
    }

    @Override
    public final AsmType toAsmType() {
        return isComplete() ? new AsmType.ByteArray(sizeof(), alignof()) : AsmType.DUMMY;
    }
}
