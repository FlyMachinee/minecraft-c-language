package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.TacInstruction;

import java.util.List;

public record EntryNode(List<Integer> successors) implements GraphNode {

    @Override
    public int id() {
        return 0;
    }

    @Override
    public NodeType type() {
        return NodeType.ENTRY;
    }

    @Override
    public List<Integer> predecessors() {
        return List.of();
    }

    @Override
    public List<String> labels() {
        return List.of();
    }

    @Override
    public List<TacInstruction> instructions() {
        return List.of();
    }
}
