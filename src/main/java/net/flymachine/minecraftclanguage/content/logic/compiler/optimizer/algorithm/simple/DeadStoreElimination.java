package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.simple;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.common.UseDefCollector;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.liveness.LivenessAnalysis;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.liveness.LivenessResult;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.ControlFlowGraph;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;

import java.util.BitSet;

public final class DeadStoreElimination implements SimpleTraversalPass {

    @Override
    public boolean runOn(ControlFlowGraph cfg, SymbolTable symbolTable) {
        LivenessResult live = LivenessAnalysis.analyze(cfg, symbolTable);
        UseDefCollector collector = new UseDefCollector(live.interner());
        BitSet staticVars = live.staticVars();
        BitSet use = new BitSet();
        BitSet def = new BitSet();
        boolean changed = false;

        for (BasicBlock block : cfg.blocks()) {
            // 块出口处的活跃变量集合
            BitSet state = (BitSet) live.out(block).clone();

            for (int i = block.instructions().size() - 1; i >= 0; i--) {
                TacInstruction inst = block.instructions().get(i);

                // 该条指令的 def/use 集合
                use.clear();
                def.clear();
                collector.collect(inst, use, def);
                if (inst instanceof TacDirectCall || inst instanceof TacIndirectCall) {
                    // 函数调用可能会读写静态变量
                    use.or(staticVars);
                    def.or(staticVars);
                }

                // 如果该条指令无副作用，且其 def 的结果在后续不活跃，则可删除
                if (isPure(inst) && !def.isEmpty() && !def.intersects(state)) {
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
               || inst instanceof TacAddPointer;
    }
}
