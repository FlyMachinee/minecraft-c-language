package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.simple;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.ControlFlowGraph;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.GraphNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Queue;

public final class UnreachableBlockElimination implements SimpleTraversalPass {

    @Override
    public boolean runOn(ControlFlowGraph cfg, SymbolTable symbolTable) {
        IntSet allBlocks = new IntArraySet();
        for (BasicBlock block : cfg.blocks()) {
            allBlocks.add(block.id());
        }

        IntSet reachableNodes = bfs(cfg);

        allBlocks.removeAll(reachableNodes);
        if (!allBlocks.isEmpty()) {
            for (int unreachableId : allBlocks) {
                cfg.deleteNode(unreachableId);
            }
            return true;
        } else {
            return false;
        }
    }

    private static @NotNull IntSet bfs(ControlFlowGraph cfg) {
        IntSet reachableNodes = new IntArraySet();
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(cfg.entry().id());
        reachableNodes.add(cfg.entry().id());
        while (!queue.isEmpty()) {
            int currentId = queue.poll();
            GraphNode currentNode = cfg.requireNode(currentId);
            for (int successorId : currentNode.successors()) {
                if (reachableNodes.add(successorId)) {
                    queue.add(successorId);
                }
            }
        }
        return reachableNodes;
    }
}
