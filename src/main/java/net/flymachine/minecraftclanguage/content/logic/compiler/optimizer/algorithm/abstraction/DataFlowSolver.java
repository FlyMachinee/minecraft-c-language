package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.abstraction;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.ControlFlowGraph;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.EntryNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.ExitNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.GraphNode;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public final class DataFlowSolver {
    private DataFlowSolver() { }

    public static DataFlowResult solve(DataFlowProblem problem, ControlFlowGraph cfg, int n) {
        int slotCount = cfg.slotCount();
        BitSet[] befores = new BitSet[slotCount];
        BitSet[] afters = new BitSet[slotCount];
        for (int i = 0; i < slotCount; i++) {
            befores[i] = new BitSet(n);
            afters[i] = new BitSet(n);
        }

        problem.init(afters, n);

        List<BasicBlock> order = (switch (problem.direction()) {
            case FORWARD -> reversedPostorderRank(cfg);
            case BACKWARD -> postorderRank(cfg);
        }).int2IntEntrySet().stream()
          .sorted(Map.Entry.comparingByValue())
          .map(entry -> (BasicBlock) cfg.requireNode(entry.getIntKey()))
          .toList();

        int guard = 0;
        int guardLimit = Math.max(1, order.size()) * Math.max(64, n + 1);
        boolean changed = true;

        while (changed) {
            if (++guard > guardLimit) {
                throw new IllegalStateException("data flow analysis did not converge");
            }
            changed = false;

            for (BasicBlock block : order) {
                int id = block.id();
                List<Integer> toMeet = switch (problem.direction()) {
                    case FORWARD -> block.predecessors();
                    case BACKWARD -> block.successors();
                };

                BitSet before = befores[id];
                meet(problem, afters, toMeet, n, before);

                BitSet after = problem.transfer(id, before);
                if (!after.equals(afters[id])) {
                    afters[id].clear();
                    afters[id].or(after);
                    changed = true;
                }
            }
        }

        return new DataFlowResult(befores, afters);
    }

    private static void meet(DataFlowProblem problem, BitSet[] afters, List<Integer> toMeet, int n, BitSet before) {
        switch (problem.meet()) {
            case UNION -> before.clear();
            case INTERSECTION -> before.set(0, n);
        }
        for (int neighborId : toMeet) {
            switch (problem.meet()) {
                case UNION -> before.or(afters[neighborId]);
                case INTERSECTION -> before.and(afters[neighborId]);
            }
        }
    }

    private static Int2IntArrayMap postorderRank(ControlFlowGraph cfg) {
        Int2IntArrayMap postOrderRank = new Int2IntArrayMap(cfg.slotCount());
        IntArraySet visited = new IntArraySet(cfg.slotCount());
        Stack<Integer> stack = new Stack<>();
        stack.push(cfg.entry().id());
        int rank = 0;

        while (!stack.empty()) {
            int x = stack.pop();
            if (x < 0) {
                // 第二次遍历
                // 易见 -(-x - 1) - 1 = x
                int u = -x - 1;
                if (cfg.requireNode(u) instanceof BasicBlock) {
                    postOrderRank.put(u, rank++);
                }
                continue;
            }
            if (visited.contains(x)) {
                continue;
            }

            // 第一次遍历
            visited.add(x);

            GraphNode u = cfg.requireNode(x);
            // 再次压栈，等所有后继都访问后，回过来访问该节点
            stack.push(-x - 1);

            List<Integer> successors = u.successors();
            for (int i = successors.size() - 1; i >= 0; i--) {
                int vId = successors.get(i);
                GraphNode v = cfg.requireNode(vId);
                if (v instanceof ExitNode) {
                    continue;
                }
                if (v instanceof EntryNode) {
                    throw new IllegalStateException("Malformed CFG: Entry node has predecessors");
                }
                if (!visited.contains(vId)) {
                    stack.push(vId);
                }
            }
        }
        return postOrderRank;
    }

    private static Int2IntArrayMap reversedPostorderRank(ControlFlowGraph cfg) {
        Int2IntArrayMap postOrderRank = postorderRank(cfg);
        int size = postOrderRank.size();
        for (Int2IntMap.Entry entry : postOrderRank.int2IntEntrySet()) {
            postOrderRank.put(entry.getIntKey(), size - 1 - entry.getIntKey());
        }
        return postOrderRank;
    }
}
