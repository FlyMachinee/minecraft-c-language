package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.simple;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.common.FunctionContext;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.liveness.LivenessAnalysis;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.liveness.LivenessResult;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;

import java.util.BitSet;

public final class DeadStoreElimination implements SimpleTraversalPass {

    @Override
    public boolean runOn(FunctionContext ctx) {
        LivenessResult live = LivenessAnalysis.analyze(ctx);
        BitSet use = new BitSet();
        BitSet def = new BitSet();
        BitSet tmpDef = new BitSet();
        boolean changed = false;

        for (BasicBlock block : ctx.cfg().blocks()) {
            // 块出口处的活跃变量集合
            BitSet state = (BitSet) live.out(block).clone();

            for (int i = block.instructions().size() - 1; i >= 0; i--) {
                TacInstruction inst = block.instructions().get(i);

                // 该条指令的 def/use 集合
                use.clear();
                def.clear();
                LivenessAnalysis.collectUseDef(ctx, inst, use, def);
                tmpDef.clear();
                tmpDef.or(def);
                if (inst instanceof TacCopyToOffset copy) {
                    tmpDef.set(ctx.interner().intern(copy.dst));
                } else if (inst instanceof TacCopyByteArrayToOffset copy) {
                    tmpDef.set(ctx.interner().intern(copy.dst));
                }

                // 如果该条指令无副作用，且其 def 的结果在后续不活跃，则可删除
                if (isPure(inst) && !tmpDef.isEmpty() && !tmpDef.intersects(state)) {
                    block.instructions().remove(i);
                    changed = true;
                    // 删除后，def/use 不需要更新
                    continue;
                }

                // 回推上一条指令结尾处的状态
                state.andNot(def);
                state.or(use);
            }
        }

        return changed;
    }

    private boolean isPure(TacInstruction inst) {
        return inst instanceof TacCopy
               || inst instanceof TacBinaryOperation
               || inst instanceof TacUnaryOperation
               || inst instanceof TacSignExtend
               || inst instanceof TacTruncate
               || inst instanceof TacZeroExtend
               || inst instanceof TacDoubleToInt
               || inst instanceof TacDoubleToUnsignedInt
               || inst instanceof TacIntToDouble
               || inst instanceof TacUnsignedIntToDouble
               || inst instanceof TacGetAddress
               || inst instanceof TacAddPointer
               || inst instanceof TacCopyToOffset
               || inst instanceof TacCopyByteArrayToOffset
               || inst instanceof TacCopyFromOffset;
    }
}
