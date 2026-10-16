package net.flymachine.minecraftclanguage.content.logic.compiler;

import java.util.Objects;

public record CompilerOptions(OptimizationOptions optimization) {

    public static final CompilerOptions DEFAULT = new CompilerOptions(OptimizationOptions.DEFAULT);

    public CompilerOptions {
        Objects.requireNonNull(optimization, "optimization cannot be null");
    }

    public record OptimizationOptions(OptimizationLevel level) {
        public static final OptimizationOptions DEFAULT = new OptimizationOptions(OptimizationLevel.O1);
    }

    public enum OptimizationLevel {O0, O1}
}
