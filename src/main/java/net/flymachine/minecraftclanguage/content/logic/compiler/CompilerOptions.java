package net.flymachine.minecraftclanguage.content.logic.compiler;

import java.util.EnumSet;
import java.util.Objects;

public record CompilerOptions(OptimizationOptions optimization, DumpOptions dumping) {

    public static final CompilerOptions DEFAULT = new CompilerOptions(OptimizationOptions.DEFAULT, DumpOptions.DEFAULT);

    public CompilerOptions {
        Objects.requireNonNull(optimization, "optimization cannot be null");
        Objects.requireNonNull(dumping, "dumping cannot be null");
    }

    public record OptimizationOptions(OptimizationLevel level) {
        public static final OptimizationOptions DEFAULT = new OptimizationOptions(OptimizationLevel.O1);
    }

    public enum OptimizationLevel {O0, O1}

    public final static class DumpOptions {
        public static final DumpOptions DEFAULT = new DumpOptions(EnumSet.noneOf(DumpKind.class));
        public static final DumpOptions ALL = new DumpOptions(EnumSet.allOf(DumpKind.class));

        private final EnumSet<DumpKind> kinds;

        private DumpOptions(EnumSet<DumpKind> kinds) {
            this.kinds = kinds;
        }

        public boolean contains(DumpKind kind) {
            return kinds.contains(kind);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private final EnumSet<DumpKind> kinds = EnumSet.noneOf(DumpKind.class);

            public Builder add(DumpKind kind) {
                kinds.add(kind);
                return this;
            }

            public Builder remove(DumpKind kind) {
                kinds.remove(kind);
                return this;
            }

            public Builder addAll() {
                this.kinds.addAll(EnumSet.allOf(DumpKind.class));
                return this;
            }

            public DumpOptions build() {
                return new DumpOptions(kinds.clone());
            }
        }
    }

    public enum DumpKind {
        AST_RAW,
        AST_ANNOTATED,
        TAC_RAW,
        TAC_RAW_SYMTAB,
        TAC_OPTIMIZED,
        CFG_RAW,
        CFG_OPTIMIZED,
        HL_ASM_RAW,
        HL_ASM_RAW_SYMTAB,
        HL_ASM_PSEUDO,
        ASM
    }
}
