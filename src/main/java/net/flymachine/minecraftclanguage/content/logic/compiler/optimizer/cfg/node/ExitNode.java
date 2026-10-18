package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.TacInstruction;

import java.util.List;

public record ExitNode(List<Integer> predecessors) implements GraphNode {

    @Override
    public int id() {
        return 1;
    }

    @Override
    public NodeType type() {
        return NodeType.EXIT;
    }

    @Override
    public List<Integer> successors() {
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
