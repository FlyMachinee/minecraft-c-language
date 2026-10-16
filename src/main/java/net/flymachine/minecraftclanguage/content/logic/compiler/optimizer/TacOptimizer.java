package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.topLevel.TacFunction;

public final class TacOptimizer {

    public TacOptimizer() { }

    public TacProgram optimize(TacProgram program) {
        return program;
    }

    private TacFunction optimize(TacFunction function) {
        return function;
    }
}
