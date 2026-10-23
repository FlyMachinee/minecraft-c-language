package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.liveness;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.common.FunctionContext;
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

    private final BitSet scratchUse = new BitSet();
    private final BitSet scratchDef = new BitSet();

    private LivenessAnalysis() { }

    public static LivenessResult analyze(FunctionContext ctx) {
        return new LivenessAnalysis().run(ctx);
    }

    private LivenessResult run(FunctionContext ctx) {
        ControlFlowGraph cfg = ctx.cfg();
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

        // 函数出口处静态变量仍然活跃：调用者之后可能读取它们
        inB[cfg.exit().id()] = ctx.staticVars();

        computeBlockUseDef(ctx, useB, defB);

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

        return new LivenessResult(inB, outB);
    }

    private void computeBlockUseDef(FunctionContext ctx, BitSet[] useB, BitSet[] defB) {
        for (BasicBlock block : ctx.cfg().blocks()) {
            // 活跃变量分析中
            // use[B] 是块内在所有定义前被使用的变量集合
            // def[B] 是块内在所有使用前被定义的变量集合
            // 以下实现稍有不同，但分析结果等价
            BitSet use = useB[block.id()];
            BitSet def = defB[block.id()];

            for (TacInstruction inst : block.instructions()) {
                scratchUse.clear();
                scratchDef.clear();
                LivenessAnalysis.collectUseDef(ctx, inst, scratchUse, scratchDef);
                scratchUse.andNot(def);

                use.or(scratchUse);
                def.or(scratchDef);
            }
        }
    }

    public static void collectUseDef(FunctionContext ctx, TacInstruction inst, BitSet useSet, BitSet defSet) {
        ctx.collector().collect(inst, useSet, defSet);
        if (inst instanceof TacDirectCall || inst instanceof TacIndirectCall) {
            // 函数调用可能会读静态变量、通过指针访问等
            useSet.or(ctx.memoryEscapedVars());
        } else if (inst instanceof TacLoad) {
            // 读取内存可能会读静态变量、通过指针访问等
            useSet.or(ctx.memoryEscapedVars());
        } else if (inst instanceof TacCopyFromOffset aggregateCopy) {
            // 读取聚合类型的一部分，即使用了整个聚合体
            useSet.set(ctx.interner().intern(aggregateCopy.src));
        }
    }

}
