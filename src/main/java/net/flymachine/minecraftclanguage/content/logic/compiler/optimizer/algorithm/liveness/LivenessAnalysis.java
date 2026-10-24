package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.liveness;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.abstraction.DataFlowProblem;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.abstraction.DataFlowResult;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.abstraction.DataFlowSolver;
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
    private BitSet[] useB;
    private BitSet[] defB;

    private LivenessAnalysis() { }

    public static LivenessResult analyze(FunctionContext ctx) {
        return new LivenessAnalysis().run(ctx);
    }

    private LivenessResult run(FunctionContext ctx) {
        ControlFlowGraph cfg = ctx.cfg();
        int slotCount = cfg.slotCount();
        this.useB = new BitSet[slotCount];
        this.defB = new BitSet[slotCount];
        for (int i = 0; i < slotCount; i++) {
            useB[i] = new BitSet();
            defB[i] = new BitSet();
        }

        computeBlockUseDef(ctx, useB, defB);
        ctx.staticVars();
        int n = ctx.interner().size();

        DataFlowProblem problem = new LivenessProblem(ctx);
        DataFlowResult result = DataFlowSolver.solve(problem, cfg, n);
        return new LivenessResult(result.afters(), result.befores());
    }

    private final class LivenessProblem implements DataFlowProblem {
        private final FunctionContext ctx;

        LivenessProblem(FunctionContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public Direction direction() { return Direction.BACKWARD; }

        @Override
        public Meet meet() { return Meet.UNION; }

        @Override
        public BitSet transfer(int blockId, BitSet before) {
            // after = use ∪ (before − def)
            BitSet tmp = (BitSet) before.clone();
            tmp.andNot(defB[blockId]);
            tmp.or(useB[blockId]);
            return tmp;
        }

        @Override
        public void init(BitSet[] afters, int n) {
            // 函数出口处静态变量活跃
            afters[ctx.cfg().exit().id()].or(ctx.staticVars());
        }
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
