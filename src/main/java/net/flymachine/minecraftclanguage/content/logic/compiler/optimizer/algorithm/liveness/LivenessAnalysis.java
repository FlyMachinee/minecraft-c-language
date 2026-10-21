package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.liveness;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.TacDirectCall;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.TacIndirectCall;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.TacInstruction;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.common.UseDefCollector;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.common.VariableInterner;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.ControlFlowGraph;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;

import java.util.BitSet;

/**
 * 活跃变量分析
 * <p>
 * in[B]  = use[B] ∪ (out[B] − def[B])
 * out[B] = ∪ in[S] (S ∈ successors(B))
 * <p>
 * 其中 use[B] 是块内"先使用后定义"的变量集合，def[B] 是块内被定义的变量集合
 */
public final class LivenessAnalysis {

    private final VariableInterner interner;
    private final UseDefCollector collector;
    private final BitSet scratchUse = new BitSet();
    private final BitSet scratchDef = new BitSet();
    private final BitSet allStaticVars = new BitSet();

    private LivenessAnalysis(VariableInterner interner) {
        this.interner = interner;
        this.collector = new UseDefCollector(interner);
    }

    public static LivenessResult analyze(ControlFlowGraph cfg, SymbolTable symbolTable) {
        return new LivenessAnalysis(new VariableInterner()).run(cfg, symbolTable);
    }

    private LivenessResult run(ControlFlowGraph cfg, SymbolTable symbolTable) {
        int slotCount = cfg.slotCount();
        BitSet[] useB = new BitSet[slotCount];
        BitSet[] defB = new BitSet[slotCount];
        BitSet[] inB = new BitSet[slotCount];
        BitSet[] outB = new BitSet[slotCount];
        for (int i = 0; i < slotCount; i++) {
            useB[i] = new BitSet();
            defB[i] = new BitSet();
            inB[i] = new BitSet();
            outB[i] = new BitSet();
        }

        for (SymbolTable.Entry entry : symbolTable.getEntries()) {
            if (entry.attr instanceof SymbolTable.Entry.StaticAttr) {
                allStaticVars.set(interner.intern(entry.id.name));
            }
        }

        // 函数出口处静态变量仍然活跃：调用者之后可能读取它们
        inB[cfg.exit().id()] = allStaticVars;

        computeBlockUseDef(cfg, useB, defB);

        int guardLimit = Math.max(1, cfg.blocks().size()) * 64;
        int guard = 0;
        boolean changed = true;
        while (changed) {
            if (++guard > guardLimit) {
                throw new IllegalStateException("liveness analysis did not converge");
            }
            changed = false;

            for (int i = cfg.blocks().size() - 1; i >= 0; i--) {
                BasicBlock block = cfg.blocks().get(i);
                int id = block.id();

                // out[B] = ∪ in[S] (S ∈ successors(B))
                BitSet out = outB[id];
                out.clear();
                for (int successorId : block.successors()) {
                    out.or(inB[successorId]);
                }

                // in[B] = use[B] ∪ (out[B] − def[B])
                BitSet in = inB[id];
                BitSet killed = (BitSet) out.clone();
                killed.andNot(defB[id]);
                BitSet next = (BitSet) useB[id].clone();
                next.or(killed);

                if (!next.equals(in)) {
                    in.clear();
                    in.or(next);
                    changed = true;
                }
            }
        }

        return new LivenessResult(inB, outB, allStaticVars, interner);
    }

    private void computeBlockUseDef(ControlFlowGraph cfg, BitSet[] useB, BitSet[] defB) {
        for (BasicBlock block : cfg.blocks()) {
            // 活跃变量分析中
            // use[B] 是块内在所有定义前被使用的变量集合
            // def[B] 是块内在所有使用前被定义的变量集合
            // 以下实现稍有不同，但分析结果等价
            BitSet use = useB[block.id()];
            BitSet def = defB[block.id()];

            for (TacInstruction inst : block.instructions()) {
                scratchUse.clear();
                scratchDef.clear();
                collector.collect(inst, scratchUse, scratchDef);

                if (inst instanceof TacDirectCall || inst instanceof TacIndirectCall) {
                    // 函数调用可能会读写静态变量
                    scratchUse.or(allStaticVars);
                    scratchDef.or(allStaticVars);
                }
                scratchUse.andNot(def);

                use.or(scratchUse);
                def.or(scratchDef);
            }
        }
    }

}
