package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.reachingCopies;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.TacCopy;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.TacInstruction;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacValue;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacVariable;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.common.FunctionContext;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.liveness.LivenessAnalysis;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.ControlFlowGraph;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;

import java.util.BitSet;

/**
 * 到达复制分析
 * <p>
 * in[B]  = ∩ out[P] (P ∈ predecessors(B))
 * out[B] = gen[B] ∪ (in[B] − kill[B])
 */
public final class ReachingCopiesAnalysis {

    private ReachingCopiesAnalysis() { }

    public static ReachingCopiesResult analyze(FunctionContext ctx) {
        return new ReachingCopiesAnalysis().run(ctx);
    }

    private ReachingCopiesResult run(FunctionContext ctx) {
        CopyInterner interner = makeCopyInterner(ctx);
        int n = interner.size();

        ControlFlowGraph cfg = ctx.cfg();
        int slotCount = cfg.slotCount();
        BitSet[] genB = new BitSet[slotCount];
        BitSet[] killB = new BitSet[slotCount];
        BitSet[] inB = new BitSet[slotCount];
        BitSet[] outB = new BitSet[slotCount];
        for (int i = 0; i < slotCount; i++) {
            genB[i] = new BitSet(n);
            killB[i] = new BitSet(n);
            inB[i] = new BitSet(n);
            outB[i] = new BitSet(n);
        }

        computeBlockGenKill(ctx, interner, genB, killB);

        for (BasicBlock block : ctx.cfg().blocks()) {
            outB[block.id()].set(0, n);
        }

        int guardLimit = Math.max(1, cfg.blocks().size()) * (n + 1);
        int guard = 0;
        boolean changed = true;
        while (changed) {
            if (++guard > guardLimit) {
                throw new IllegalStateException("reaching copies analysis did not converge");
            }
            changed = false;

            for (BasicBlock block : ctx.cfg().blocks()) {
                int id = block.id();

                // in[B] = ∩ out[P]
                BitSet in = inB[id];
                in.clear();
                in.set(0, n);
                for (int predId : block.predecessors()) {
                    in.and(outB[predId]);
                }

                // out[B] = gen ∪ (in[B] − kill)
                BitSet out = outB[id];
                BitSet next = (BitSet) in.clone();
                next.andNot(killB[id]);
                next.or(genB[id]);

                if (!next.equals(out)) {
                    out.clear();
                    out.or(next);
                    changed = true;
                }
            }
        }

        return new ReachingCopiesResult(inB, outB, interner);

    }

    private static CopyInterner makeCopyInterner(FunctionContext ctx) {
        CopyInterner interner = new CopyInterner();
        for (BasicBlock block : ctx.cfg().blocks()) {
            for (TacInstruction inst : block.instructions()) {
                if (inst instanceof TacCopy copy) {
                    TacVariable dst = (TacVariable) copy.dst;
                    if (isSelfCopy(dst.name, copy.src)) {
                        // 跳过 x = x
                        continue;
                    }
                    interner.intern(dst.name, copy.src);
                }
            }
        }
        return interner;
    }

    public static boolean isSelfCopy(TacInstruction inst) {
        return inst instanceof TacCopy copy && isSelfCopy(((TacVariable) copy.dst).name, copy.src);
    }

    public static boolean isSelfCopy(String dst, TacValue src) {
        return src instanceof TacVariable srcVar && srcVar.name.equals(dst);
    }

    private void computeBlockGenKill(FunctionContext ctx, CopyInterner interner, BitSet[] genB, BitSet[] killB) {
        int n = interner.size();
        BitSet state = new BitSet(n);
        BitSet scratchUse = new BitSet();
        BitSet scratchDef = new BitSet();
        BitSet blockDef = new BitSet();

        for (BasicBlock block : ctx.cfg().blocks()) {
            BitSet gen = genB[block.id()];
            BitSet kill = killB[block.id()];

            // gen[B]
            state.clear();
            for (TacInstruction inst : block.instructions()) {
                transferOne(ctx, interner, inst, state, scratchUse, scratchDef);
            }
            gen.or(state);

            // kill[B]
            blockDef.clear();
            for (TacInstruction inst : block.instructions()) {
                if (isSelfCopy(inst)) {
                    continue;
                }
                scratchUse.clear();
                scratchDef.clear();
                LivenessAnalysis.collectUseDef(ctx, inst, scratchUse, scratchDef);
                blockDef.or(scratchDef);
            }

            // 使用其 def 计算 kill，kill 所有与 def 相关的复制指令
            for (int i = blockDef.nextSetBit(0); i >= 0; i = blockDef.nextSetBit(i + 1)) {
                kill.or(interner.relatedCopies(ctx.interner().nameOf(i)));
                if (i == Integer.MAX_VALUE) {
                    break;
                }
            }
        }

    }

    public static void transferOne(FunctionContext ctx, CopyInterner interner,
        TacInstruction inst, BitSet state, BitSet scratchUse, BitSet scratchDef) {

        if (inst instanceof TacCopy copy) {
            TacVariable dst = (TacVariable) copy.dst;
            if (isSelfCopy(dst.name, copy.src)) {
                return;
            }
            // kill 所有 dst 相关的复制指令
            state.andNot(interner.relatedCopies(dst.name));
            // gen 自己
            state.set(interner.indexOf(dst.name, copy.src));
            return;
        }

        scratchUse.clear();
        scratchDef.clear();
        LivenessAnalysis.collectUseDef(ctx, inst, scratchUse, scratchDef);
        // 使用其 def 计算 kill，kill 所有与 def 相关的复制指令
        for (int i = scratchDef.nextSetBit(0); i >= 0; i = scratchDef.nextSetBit(i + 1)) {
            state.andNot(interner.relatedCopies(ctx.interner().nameOf(i)));
            if (i == Integer.MAX_VALUE) {
                break;
            }
        }
    }
}
