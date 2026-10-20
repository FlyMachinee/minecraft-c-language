package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer;

import net.flymachine.minecraftclanguage.content.logic.compiler.CompilerOptions;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.topLevel.TacFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.topLevel.TacTopLevel;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.simple.BasicBlockMergence;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.simple.UnreachableBlockElimination;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.simple.UselessLabelElimination;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.ControlFlowGraph;

public final class TacOptimizer {

    public TacOptimizer() { }

    public void optimize(TacProgram program, SymbolTable symbolTable, CompilerOptions options) {
        if (options.optimization().level() == CompilerOptions.OptimizationLevel.O0) { return; }

        int size = program.topLevels.size();
        for (int i = 0; i < size; i++) {
            TacTopLevel topLevel = program.topLevels.get(i);
            if (topLevel instanceof TacFunction function) {
                program.topLevels.set(i, optimize(function, symbolTable, options));
            }
        }
    }

    private TacFunction optimize(TacFunction function, SymbolTable symbolTable, CompilerOptions options) {
        ControlFlowGraph cfg = ControlFlowGraph.build(function);

        new UnreachableBlockElimination().runOn(cfg);
        new UselessLabelElimination().runOn(cfg);
        new BasicBlockMergence().runOn(cfg);

        cfg.flattenInto(function);
        return function;
    }
}
