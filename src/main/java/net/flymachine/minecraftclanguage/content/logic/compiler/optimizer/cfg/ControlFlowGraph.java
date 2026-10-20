package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.TacInstruction;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.TacLabel;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.topLevel.TacFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.EntryNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.ExitNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.GraphNode;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ControlFlowGraph {

    private final List<GraphNode> slots;
    private final Map<String, Integer> labelToId;
    private List<GraphNode> liveNodes;
    private List<BasicBlock> liveBlocks;

    public ControlFlowGraph(List<GraphNode> nodes, Map<String, Integer> labelToId) {
        this.slots = nodes;
        this.labelToId = labelToId;
    }

    public EntryNode entry() {
        return (EntryNode) slots.get(0);
    }

    public ExitNode exit() {
        return (ExitNode) slots.get(1);
    }

    /**
     * 返回按照节点 ID 排序的所有节点的不可变列表视图，已经排除了被删除的节点
     */
    public List<GraphNode> nodes() {
        if (liveNodes == null) {
            List<GraphNode> live = new ArrayList<>(slots.size());
            for (GraphNode node : slots) {
                if (node != null) {
                    live.add(node);
                }
            }
            liveNodes = Collections.unmodifiableList(live);
        }
        return liveNodes;
    }

    /**
     * 返回按照节点 ID 排序的所有基本块的不可变列表视图，已经排除了被删除的节点
     */
    public List<BasicBlock> blocks() {
        if (liveBlocks == null) {
            List<BasicBlock> live = new ArrayList<>();
            for (GraphNode node : nodes()) {
                if (node instanceof BasicBlock block) {
                    live.add(block);
                }
            }
            liveBlocks = Collections.unmodifiableList(live);
        }
        return liveBlocks;
    }

    private void invalidateView() {
        liveNodes = null;
        liveBlocks = null;
    }

    /**
     * 返回所有节点的列表，包括被删除的节点（为 null），按照节点 ID 排序排列
     */
    public List<GraphNode> slots() {
        return slots;
    }

    public int slotCount() {
        return slots.size();
    }

    public @Nullable GraphNode node(int id) {
        return id >= 0 && id < slots.size() ? slots.get(id) : null;
    }

    public @NotNull GraphNode requireNode(int id) {
        GraphNode node = node(id);
        if (node == null) {
            throw new IllegalArgumentException("Node with id " + id + " does not exist");
        }
        return node;
    }

    /**
     * 删除 ID 为 id 的节点，并移除其入边和出边
     */
    public void deleteNode(int id) {
        if (id == 0 || id == 1) {
            throw new IllegalArgumentException("Cannot delete entry or exit node");
        }

        if (id < 0 || id >= slots.size() || slots.get(id) == null) {
            throw new IllegalArgumentException("Node with id " + id + " does not exist");
        }

        BasicBlock nodeToDelete = (BasicBlock) slots.get(id);
        for (int predecessorId : nodeToDelete.predecessors()) {
            if (!slots.get(predecessorId).successors().remove(Integer.valueOf(nodeToDelete.id()))) {
                throw new IllegalStateException("Inconsistent graph");
            }
        }
        for (int successorId : nodeToDelete.successors()) {
            if (!slots.get(successorId).predecessors().remove(Integer.valueOf(nodeToDelete.id()))) {
                throw new IllegalStateException("Inconsistent graph");
            }
        }

        slots.set(id, null);
        invalidateView();

        for (String label : nodeToDelete.labels()) {
            labelToId.remove(label);
        }
    }

    public BasicBlock blockByLabel(String label) {
        Integer id = labelToId.get(label);
        if (id == null) {
            throw new IllegalStateException("No block found for label: " + label);
        }
        return (BasicBlock) requireNode(id);
    }

    public static ControlFlowGraph build(TacFunction function) {
        ControlFlowGraphBuilder builder = new ControlFlowGraphBuilder(function);
        return builder.build();
    }

    public List<TacInstruction> flatten() {
        List<TacInstruction> flattened = new ArrayList<>();
        for (BasicBlock block : blocks()) {
            for (String label : block.labels()) {
                flattened.add(new TacLabel(label));
            }
            flattened.addAll(block.instructions());
        }
        return flattened;
    }

    public void flattenInto(TacFunction function) {
        function.insts.clear();
        for (BasicBlock block : blocks()) {
            for (String label : block.labels()) {
                function.insts.add(new TacLabel(label));
            }
            function.insts.addAll(block.instructions());
        }
    }
}
