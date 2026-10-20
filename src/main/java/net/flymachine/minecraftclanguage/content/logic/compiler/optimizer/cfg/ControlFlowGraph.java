package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.*;
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
     * 返回按照节点 ID 排序的所有节点的不可变列表快照，已经排除了被删除的节点
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
     * 返回按照节点 ID 排序的所有基本块的不可变列表快照，已经排除了被删除的节点
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

    public Map<String, Integer> labelToIdMap() {
        return Collections.unmodifiableMap(labelToId);
    }

    public BasicBlock blockByLabel(String label) {
        Integer id = labelToId.get(label);
        if (id == null) {
            throw new IllegalStateException("No block found for label: " + label);
        }
        return (BasicBlock) requireNode(id);
    }

    public boolean hasLabel(String label) {
        return labelToId.containsKey(label);
    }

    /**
     * 移除指定标签（基本块以及映射），返回是否成功移除
     * <p>
     * 需保证标签已不被语句使用
     */
    public boolean removeLabel(String label) {
        Integer id = labelToId.remove(label);
        if (id == null) {
            return false;
        }
        BasicBlock block = (BasicBlock) requireNode(id);
        block.labels().remove(label);
        return true;
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

    /**
     * 检查控制流图的一致性，返回发现的问题列表，空列表表示通过
     */
    public List<String> validate() {
        List<String> problems = new ArrayList<>();
        checkSlots(problems);
        checkEdges(problems);
        checkSuccessorCounts(problems);
        checkLabels(problems);
        checkInstructions(problems);
        return problems;
    }

    private void checkSlots(List<String> problems) {
        if (slots.size() < 2) {
            problems.add("graph must have at least the entry and exit slots, found " + slots.size());
            return;
        }
        if (!(slots.get(0) instanceof EntryNode)) {
            problems.add("slot 0 must be an entry node, found " + describe(slots.get(0)));
        }
        if (!(slots.get(1) instanceof ExitNode)) {
            problems.add("slot 1 must be an exit node, found " + describe(slots.get(1)));
        }
        for (int id = 0; id < slots.size(); id++) {
            GraphNode node = slots.get(id);
            if (node == null) {
                continue;
            }
            if (node.id() != id) {
                problems.add("node in slot " + id + " reports id " + node.id());
            }
            if (id >= 2 && node.type() != GraphNode.NodeType.BASIC_BLOCK) {
                problems.add("slot " + id + " holds a " + node.type() + " node, expected a basic block");
            }
        }
    }

    private void checkEdges(List<String> problems) {
        for (GraphNode node : nodes()) {
            for (int successorId : node.successors()) {
                GraphNode successor = node(successorId);
                if (successor == null) {
                    problems.add("node " + node.id() + " has dangling successor " + successorId);
                } else if (!successor.predecessors().contains(node.id())) {
                    problems.add("edge " + node.id() + " -> " + successorId
                                 + " has no matching entry in the successor's predecessors");
                }
            }
            for (int predecessorId : node.predecessors()) {
                GraphNode predecessor = node(predecessorId);
                if (predecessor == null) {
                    problems.add("node " + node.id() + " has dangling predecessor " + predecessorId);
                } else if (!predecessor.successors().contains(node.id())) {
                    problems.add("edge " + predecessorId + " -> " + node.id()
                                 + " has no matching entry in the predecessor's successors");
                }
            }
        }
    }

    private void checkSuccessorCounts(List<String> problems) {
        for (GraphNode node : nodes()) {
            int expected = expectedSuccessorCount(node);
            if (expected < 0) {
                continue;
            }
            int actual = node.successors().size();
            if (actual != expected) {
                problems.add("node " + node.id() + " (" + node.type() + ") has " + actual
                             + " successors, expected " + expected);
            }
        }
    }

    private int expectedSuccessorCount(GraphNode node) {
        if (node.isExit()) {
            return 0;
        }
        if (node.isEntry()) {
            return 1;
        }
        if (!(node instanceof BasicBlock block)) {
            return -1;
        }
        TacInstruction last = block.lastInstruction();
        if (last == null || last instanceof TacReturn || last instanceof TacJump) {
            return 1;
        }
        if (last instanceof TacJumpIfZero
            || last instanceof TacJumpIfNotZero
            || last instanceof TacJumpIfComparison) {
            return 2;
        }
        return 1;
    }

    private void checkLabels(List<String> problems) {
        for (BasicBlock block : blocks()) {
            for (String label : block.labels()) {
                Integer mapped = labelToId.get(label);
                if (mapped == null) {
                    problems.add("label '" + label + "' owned by node " + block.id()
                                 + " is missing from the label map");
                } else if (mapped != block.id()) {
                    problems.add("label '" + label + "' maps to node " + mapped
                                 + " but is owned by node " + block.id());
                }
            }
        }
        for (Map.Entry<String, Integer> entry : labelToId.entrySet()) {
            if (node(entry.getValue()) == null) {
                problems.add("label '" + entry.getKey() + "' maps to missing node " + entry.getValue());
            }
        }
    }

    private void checkInstructions(List<String> problems) {
        for (BasicBlock block : blocks()) {
            for (TacInstruction instruction : block.instructions()) {
                if (instruction instanceof TacLabel) {
                    problems.add("node " + block.id() + " holds a label inside its instruction list");
                }
            }
            checkJumpTarget(block, problems);
        }
    }

    private void checkJumpTarget(BasicBlock block, List<String> problems) {
        TacInstruction last = block.lastInstruction();
        String target = null;
        if (last instanceof TacJump jump) {
            target = jump.target;
        } else if (last instanceof TacJumpIfZero jump) {
            target = jump.target;
        } else if (last instanceof TacJumpIfNotZero jump) {
            target = jump.target;
        } else if (last instanceof TacJumpIfComparison jump) {
            target = jump.target;
        }
        if (target == null) {
            return;
        }

        Integer targetId = labelToId.get(target);
        if (targetId == null) {
            problems.add("node " + block.id() + " jumps to unknown label '" + target + "'");
        } else if (!block.successors().contains(targetId)) {
            problems.add("node " + block.id() + " jumps to '" + target + "' (node " + targetId
                         + ") which is not among its successors");
        }
    }

    private static String describe(GraphNode node) {
        return node == null ? "null" : node.type().toString();
    }
}
