package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.topLevel.TacFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.EntryNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.ExitNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.GraphNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ControlFlowGraphBuilder {
    private final TacFunction function;
    private final Map<String, Integer> labelToId = new HashMap<>();
    private int nextId = 2;

    public ControlFlowGraphBuilder(TacFunction function) {
        this.function = function;
    }

    public ControlFlowGraph build() {
        List<BasicBlock> blocks = partition(function.insts);

        if (blocks.isEmpty()) {
            throw new IllegalStateException("Function has no basic blocks");
        }

        ControlFlowGraph cfg = buildGraph(blocks);

        return cfg;
    }

    private List<BasicBlock> partition(List<TacInstruction> insts) {
        List<BasicBlock> finished = new ArrayList<>();
        BasicBlock current = emptyBlock();

        for (TacInstruction inst : insts) {
            if (inst instanceof TacLabel label) {
                if (!current.instructions().isEmpty()) {
                    finished.add(current);
                    current = emptyBlock();
                }
                current.labels().add(label.name);
                labelToId.put(label.name, current.id());
            } else if (isJump(inst)) {
                current.instructions().add(inst);
                finished.add(current);
                current = emptyBlock();
            } else {
                current.instructions().add(inst);
            }
        }

        if (!current.instructions().isEmpty() || !current.labels().isEmpty()) {
            finished.add(current);
        }

        return finished;
    }

    private BasicBlock emptyBlock() {
        return new BasicBlock(nextId++, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    private boolean isJump(TacInstruction inst) {
        return inst instanceof TacJump || inst instanceof TacJumpIfComparison || inst instanceof TacJumpIfZero ||
               inst instanceof TacJumpIfNotZero || inst instanceof TacReturn;
    }

    private ControlFlowGraph buildGraph(List<BasicBlock> blocks) {
        List<GraphNode> nodes = new ArrayList<>();
        nodes.add(new EntryNode(new ArrayList<>()));
        nodes.add(new ExitNode(new ArrayList<>()));
        nodes.addAll(blocks);

        ControlFlowGraph cfg = new ControlFlowGraph(nodes, labelToId);

        addEdge(cfg.entry(), blocks.get(0));

        for (BasicBlock block : blocks) {
            TacInstruction last = block.lastInstruction();
            if (last instanceof TacReturn) {
                addEdge(block, cfg.exit());
            } else if (last instanceof TacJump jump) {
                int targetId = labelToId.get(jump.target);
                addEdge(block, cfg.node(targetId));
            } else if (last instanceof TacJumpIfComparison conditional) {
                processConditionalJump(cfg, block, conditional.target);
            } else if (last instanceof TacJumpIfZero conditional) {
                processConditionalJump(cfg, block, conditional.target);
            } else if (last instanceof TacJumpIfNotZero conditional) {
                processConditionalJump(cfg, block, conditional.target);
            } else {
                addEdge(block, fallthroughNode(cfg, block));
            }
        }

        return cfg;
    }

    private void processConditionalJump(ControlFlowGraph cfg, BasicBlock block, String target) {
        int targetId = labelToId.get(target);
        addEdge(block, cfg.node(targetId));
        addEdge(block, fallthroughNode(cfg, block));
    }

    private GraphNode fallthroughNode(ControlFlowGraph cfg, BasicBlock block) {
        int id = block.id();
        return id == cfg.nodes().size() - 1 ? cfg.exit() : cfg.node(id + 1);
    }

    private void addEdge(GraphNode from, GraphNode to) {
        from.successors().add(to.id());
        to.predecessors().add(from.id());
    }
}
