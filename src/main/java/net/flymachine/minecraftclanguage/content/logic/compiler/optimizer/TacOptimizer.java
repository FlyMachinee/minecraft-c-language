package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer;

import net.flymachine.minecraftclanguage.content.logic.compiler.CompilerOptions;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.topLevel.TacFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.topLevel.TacTopLevel;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.simple.*;
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
        if (options.dumping().contains(CompilerOptions.DumpKind.TAC_OPTIMIZED)) {
            System.out.println("=== Dumping TAC after optimization ===");
            program.dumpPretty(System.out);
            System.out.println("=== Dumping complete ===");
            System.out.println();
        }
    }

    private TacFunction optimize(TacFunction function, SymbolTable symbolTable, CompilerOptions options) {
        if (function.insts.isEmpty()) { return function; }

        ControlFlowGraph cfg = ControlFlowGraph.build(function);

        boolean changed;
        do {
            changed = new ConstantFolding().runOn(cfg, symbolTable);
            changed |= new UnreachableBlockElimination().runOn(cfg, symbolTable);
            changed |= new UselessLabelElimination().runOn(cfg, symbolTable);
            changed |= new DeadStoreElimination().runOn(cfg, symbolTable);
            changed |= new BasicBlockMergence().runOn(cfg, symbolTable);
        } while (changed);

        cfg.flattenInto(function);
        return function;
    }
}
