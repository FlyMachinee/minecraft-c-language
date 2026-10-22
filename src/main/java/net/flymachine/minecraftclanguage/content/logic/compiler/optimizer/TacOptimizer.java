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

import java.util.List;

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
        if (options.debug().isDebug()) {
            cfgValidation(cfg, "initial");
        }

        boolean changed;
        do {
            changed = new ConstantFolding().runOn(ctx);
            if (options.debug().isDebug()) {
                cfgValidation(cfg, "ConstantFolding");
            }

            changed |= new UnreachableBlockElimination().runOn(ctx);
            if (options.debug().isDebug()) {
                cfgValidation(cfg, "UnreachableBlockElimination");
            }

            changed |= new UselessJumpElimination().runOn(ctx);
            if (options.debug().isDebug()) {
                cfgValidation(cfg, "UselessJumpElimination");
            }

            changed |= new UselessLabelElimination().runOn(ctx);
            if (options.debug().isDebug()) {
                cfgValidation(cfg, "UselessLabelElimination");
            }

            changed |= new CopyPropagation().runOn(ctx);
            if (options.debug().isDebug()) {
                cfgValidation(cfg, "CopyPropagation");
            }

            changed |= new DeadStoreElimination().runOn(ctx);
            if (options.debug().isDebug()) {
                cfgValidation(cfg, "DeadStoreElimination");
            }

            changed |= new EmptyBlockElimination().runOn(ctx);
            if (options.debug().isDebug()) {
                cfgValidation(cfg, "EmptyBlockElimination");
            }

            changed |= new BasicBlockMergence().runOn(ctx);
            if (options.debug().isDebug()) {
                cfgValidation(cfg, "BasicBlockMergence");
            }
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

    private void cfgValidation(ControlFlowGraph cfg, String lastPassName) {
        List<String> result = cfg.validate();
        if (!result.isEmpty()) {
            System.err.println("CFG validation error after pass: " + lastPassName);
            for (String s : result) {
                System.err.println(s);
            }
            throw new IllegalStateException("CFG validation failed after pass: " + lastPassName);
        }
    }
}
