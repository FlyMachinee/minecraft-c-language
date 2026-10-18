package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.TacInstruction;

import java.util.List;

public interface GraphNode {
    enum NodeType {
        ENTRY, EXIT, BASIC_BLOCK
    }

    int id();

    NodeType type();

    List<Integer> predecessors();

    List<Integer> successors();

    List<String> labels();

    List<TacInstruction> instructions();

    default boolean isEntry() {
        return type() == NodeType.ENTRY;
    }

    default boolean isExit() {
        return type() == NodeType.EXIT;
    }
}
