package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg;

import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.EntryNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.ExitNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.GraphNode;

import java.util.List;
import java.util.Map;

public final class ControlFlowGraph {

    private final List<GraphNode> nodes;
    private final Map<String, Integer> labelToId;

    public ControlFlowGraph(List<GraphNode> nodes, Map<String, Integer> labelToId) {
        this.nodes = nodes;
        this.labelToId = labelToId;
    }

    public EntryNode entry() {
        return (EntryNode) nodes.get(0);
    }

    public ExitNode exit() {
        return (ExitNode) nodes.get(1);
    }

    public List<GraphNode> nodes() {
        return nodes;
    }

    public GraphNode node(int id) {
        return nodes.get(id);
    }

    public BasicBlock blockByLabel(String label) {
        Integer id = labelToId.get(label);
        if (id == null) {
            throw new IllegalStateException("No block found for label: " + label);
        }
        return (BasicBlock) node(id);
    }
}
