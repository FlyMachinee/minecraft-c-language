package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.simple;

import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.ControlFlowGraph;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.GraphNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.util.TacUtil;

import java.util.List;

public final class BasicBlockMergence implements SimpleTraversalPass {

    @Override
    public boolean runOn(ControlFlowGraph cfg) {
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
}
