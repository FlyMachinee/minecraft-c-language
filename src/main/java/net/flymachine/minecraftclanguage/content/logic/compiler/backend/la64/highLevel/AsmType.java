package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel;

import java.util.Arrays;
import java.util.EnumSet;

public final class AsmType {

    private enum Type {
        WORD, DWORD
    }

    public static final AsmType WORD = new AsmType(Type.WORD);
    public static final AsmType DWORD = new AsmType(Type.DWORD);
    public static final AsmType WORD_OR_DWORD = new AsmType(Type.WORD, Type.DWORD);

    private final EnumSet<Type> types;

    private AsmType(Type... types) {
        if (types.length == 0) {
            throw new IllegalArgumentException("At least one type must be specified");
        }
        this.types = EnumSet.copyOf(Arrays.asList(types));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AsmType that = (AsmType) o;
        return types.equals(that.types);
    }

    public int alignment() {
        if (types.size() != 1) {
            throw new IllegalArgumentException("Only one type allowed");
        }
        return switch (types.iterator().next()) {
            case WORD -> 4;
            case DWORD -> 8;
        };
    }

    public boolean isWord() {
        return types.contains(Type.WORD);
    }

    public boolean isDWord() {
        return types.contains(Type.DWORD);
    }

    public boolean isSpecific() {
        return types.size() == 1;
    }
}
