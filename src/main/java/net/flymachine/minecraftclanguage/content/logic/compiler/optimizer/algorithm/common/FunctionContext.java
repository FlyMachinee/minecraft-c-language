package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.common;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.alias.AddressTakenAnalysis;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.ControlFlowGraph;

import java.util.BitSet;

public final class FunctionContext {
    private final ControlFlowGraph cfg;
    private final SymbolTable symbolTable;
    private final VariableInterner interner = new VariableInterner();
    private final UseDefCollector collector = new UseDefCollector(interner);
    private BitSet memEscVars = null;
    private BitSet staticVars = null;

    public FunctionContext(ControlFlowGraph cfg, SymbolTable symbolTable) {
        this.cfg = cfg;
        this.symbolTable = symbolTable;
    }

    public ControlFlowGraph cfg() {
        return cfg;
    }

    public SymbolTable symbolTable() {
        return symbolTable;
    }

    public VariableInterner interner() {
        return interner;
    }

    public UseDefCollector collector() {
        return collector;
    }

    public BitSet memoryEscapedVars() {
        if (memEscVars == null) {
            memEscVars = AddressTakenAnalysis.analyze(this);
            memEscVars.or(staticVars());
        }
        return memEscVars;
    }

    public BitSet staticVars() {
        if (staticVars == null) {
            staticVars = new BitSet();
            for (SymbolTable.Entry entry : symbolTable.getEntries()) {
                if (entry.attr instanceof SymbolTable.Entry.StaticAttr) {
                    staticVars.set(interner.intern(entry.id.name));
                }
            }
        }
        return staticVars;
    }
}
