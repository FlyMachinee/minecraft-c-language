package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.simple;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.ControlFlowGraph;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.util.TacUtil;

import java.util.HashSet;
import java.util.Set;

public final class UselessLabelElimination implements SimpleTraversalPass {

    @Override
    public boolean runOn(ControlFlowGraph cfg, SymbolTable symbolTable) {
        Set<String> labels = new HashSet<>(cfg.labelToIdMap().keySet());

        for (BasicBlock block : cfg.blocks()) {
            String usedLabel = TacUtil.getLabel(block.lastInstruction());
            if (usedLabel != null) {
                labels.remove(usedLabel);
            }
        }

        if (!labels.isEmpty()) {
            for (String label : labels) {
                cfg.removeLabel(label);
            }
            return true;
        }

        return false;
    }
}
