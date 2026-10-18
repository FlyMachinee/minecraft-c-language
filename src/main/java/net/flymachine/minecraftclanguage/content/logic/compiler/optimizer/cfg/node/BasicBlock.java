package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.TacInstruction;

import java.util.List;

public record BasicBlock(
    int id, List<Integer> successors, List<Integer> predecessors,
    List<TacInstruction> instructions, List<String> labels) implements GraphNode {

    @Override
    public NodeType type() {
        return NodeType.BASIC_BLOCK;
    }

    public TacInstruction lastInstruction() {
        if (instructions.isEmpty()) {
            return null;
        }
        return instructions.get(instructions.size() - 1);
    }
}
