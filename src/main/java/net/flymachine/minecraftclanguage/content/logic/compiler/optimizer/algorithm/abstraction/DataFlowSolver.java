package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.abstraction;

import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.ControlFlowGraph;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

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

        List<BasicBlock> order = switch (problem.direction()) {
            case FORWARD -> cfg.blocks();
            case BACKWARD -> {
                List<BasicBlock> blocks = new ArrayList<>(cfg.blocks());
                Collections.reverse(blocks);
                yield blocks;
            }
        };

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
}
