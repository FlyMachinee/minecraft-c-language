package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer;

import net.flymachine.minecraftclanguage.content.logic.compiler.CompilerOptions;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.topLevel.TacFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.topLevel.TacTopLevel;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.common.FunctionContext;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.simple.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.ControlFlowGraph;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.ControlFlowGraphDumper;

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

    public TacFunction optimize(TacFunction function, SymbolTable symbolTable, CompilerOptions options) {
        if (function.insts.isEmpty()) { return function; }

        ControlFlowGraph cfg = ControlFlowGraph.build(function);
        if (options.dumping().contains(CompilerOptions.DumpKind.CFG_RAW)) {
            System.out.println("=== Dumping raw CFG for function " + function.name + " ===");
            System.out.print(ControlFlowGraphDumper.toDotDetailed(cfg, function.name + " raw"));
            System.out.println("=== Dumping complete ===");
            System.out.println();
        }

        FunctionContext ctx = new FunctionContext(cfg, symbolTable);

        boolean changed;
        do {
            changed = new ConstantFolding().runOn(ctx);
            changed |= new UnreachableBlockElimination().runOn(ctx);
            changed |= new UselessLabelElimination().runOn(ctx);
            changed |= new CopyPropagation().runOn(ctx);
            changed |= new DeadStoreElimination().runOn(ctx);
            changed |= new BasicBlockMergence().runOn(ctx);
        } while (changed);

        if (options.dumping().contains(CompilerOptions.DumpKind.CFG_OPTIMIZED)) {
            System.out.println("=== Dumping optimized CFG for function " + function.name + " ===");
            System.out.print(ControlFlowGraphDumper.toDotDetailed(cfg, function.name + " optimized"));
            System.out.println("=== Dumping complete ===");
            System.out.println();
        }

        cfg.flattenInto(function);
        return function;
    }
}
