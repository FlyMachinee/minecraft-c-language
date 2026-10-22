package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.simple;

import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.common.FunctionContext;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.ControlFlowGraph;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.GraphNode;

import java.util.ArrayList;
import java.util.List;

public final class EmptyBlockElimination implements SimpleTraversalPass {

    @Override
    public boolean runOn(FunctionContext ctx) {
        ControlFlowGraph cfg = ctx.cfg();
        boolean changed = false;

        List<BasicBlock> blocks = cfg.blocks();
        for (BasicBlock block : blocks) {
            if (!block.instructions().isEmpty()) {
                continue;
            }
            if (block.successors().size() != 1) {
                throw new IllegalStateException("Inconsistent CFG: empty block must have exactly one successor");
            }

            int successorId = block.successors().get(0);
            GraphNode successorNode = cfg.requireNode(successorId);
            if (!(successorNode instanceof BasicBlock successorBlock)) {
                continue;
            }

            // 转移 label 到后继
            for (String label : new ArrayList<>(block.labels())) {
                cfg.removeLabel(label);
                cfg.addLabelToBlock(label, successorBlock.id());
            }

            // 让前驱指向本块的边改为指向本块的后继
            for (int predId : block.predecessors()) {
                GraphNode pred = cfg.requireNode(predId);
                pred.successors().add(successorId);
                successorBlock.predecessors().add(predId);
            }

            // 清理被删块的边后删除
            cfg.deleteNode(block.id());

            changed = true;
        }
        return changed;
    }
}
