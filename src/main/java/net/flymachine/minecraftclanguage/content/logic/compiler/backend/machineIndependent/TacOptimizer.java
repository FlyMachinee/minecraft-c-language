package net.flymachine.minecraftclanguage.content.logic.compiler.backend.machineIndependent;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacProgram;

public final class TacOptimizer {

    public TacOptimizer() { }

    public TacProgram optimize(TacProgram program) {
        return program;
    }

    private TacFunction optimize(TacFunction function) {
        return function;
    }
}
