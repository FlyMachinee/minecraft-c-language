package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.simple;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.TacInstruction;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.TacJump;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.TacReturn;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.common.FunctionContext;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.ControlFlowGraph;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.GraphNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.util.TacUtil;

import java.util.List;

public final class BasicBlockMergence implements SimpleTraversalPass {

    @Override
    public boolean runOn(FunctionContext ctx) {
        ControlFlowGraph cfg = ctx.cfg();
        // 当基本块A的唯一后继是基本块B，并且基本块B的唯一前驱是基本块A时，将基本块A和基本块B合并为一个基本块

        boolean changed = false;
        List<BasicBlock> blocks = cfg.blocks();

        int i = 0;
        while (i < blocks.size()) {
            BasicBlock block = blocks.get(i);

            // 虽然通过 blocks 得到的块一定存在，但在合并过程中可能会进行删除，从而需要判空
            if (cfg.node(block.id()) == null) {
                i++;
                continue;
            }

            // 检查是否有唯一后继
            int successorId = allSame(block.successors());
            if (successorId == -1) {
                i++;
                continue;
            }

            // 有唯一后继
            GraphNode successorNode = cfg.requireNode(successorId);
            // 排除 Exit
            if (!(successorNode instanceof BasicBlock successorBlock)) {
                i++;
                continue;
            }

            // 检查其后继的前驱是否唯一
            int thisId = allSame(successorBlock.predecessors());
            if (thisId == -1) {
                i++;
                continue;
            }

            // 后继有唯一前驱
            if (thisId != block.id()) {
                throw new IllegalStateException("Inconsistent graph");
            }

            // ... -> A -> B -> ...
            // 合并后 A 的末尾会直接接上 B 的指令
            // 若 B 可能以 fallthrough 结束，则语义将变为 A 可能以 fallthrough 结束
            // 但 A 合并后的 fallthrough 不一定和 B 的 fallthrough 相同
            // 当且仅当 A B 相邻时，才可以保证 fallthrough 的语义不变
            if (hasFallThrough(successorBlock) && successorBlock.id() != nextLiveId(cfg, block.id())) {
                i++;
                continue;
            }

            // 合并基本块
            changed = true;

            String label = TacUtil.getLabel(block.lastInstruction());
            if (label != null) {
                // 无条件跳转/条件跳转，但其目标与 fallthrough 结果相同
                if (!successorBlock.labels().contains(label)) {
                    throw new IllegalStateException("Inconsistent graph");
                }
                // 移除该跳转
                block.instructions().remove(block.instructions().size() - 1);
            }
            // 否则为自然 fallthrough
            block.instructions().addAll(successorBlock.instructions());

            // 更新 CFG
            // T* -> U -> V -> W <- X*
            // =>
            // T* -> U+V -> W <- X*
            block.successors().clear();
            block.successors().addAll(successorBlock.successors());
            for (int succId : successorBlock.successors()) {
                cfg.requireNode(succId).predecessors().remove(Integer.valueOf(successorId));
                cfg.requireNode(succId).predecessors().add(block.id());
            }
            successorBlock.successors().clear();
            successorBlock.predecessors().clear();
            cfg.deleteNode(successorBlock.id());
        }
        return changed;
    }

    private int allSame(List<Integer> list) {
        if (list.isEmpty()) {
            return -1;
        }
        int first = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i) != first) {
                return -1;
            }
        }
        return first;
    }

    private boolean hasFallThrough(BasicBlock block) {
        TacInstruction last = block.lastInstruction();
        return !(last instanceof TacReturn || last instanceof TacJump);
    }

    private int nextLiveId(ControlFlowGraph cfg, int id) {
        for (int next = id + 1; next < cfg.slotCount(); next++) {
            if (cfg.node(next) != null) {
                return next;
            }
        }
        return -1;
    }
}
