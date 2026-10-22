package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.simple;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.common.FunctionContext;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.ControlFlowGraph;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;

import java.util.List;

public final class UselessJumpElimination implements SimpleTraversalPass {

    @Override
    public boolean runOn(FunctionContext ctx) {
        ControlFlowGraph cfg = ctx.cfg();
        List<BasicBlock> blocks = cfg.blocks();
        boolean changed = false;

        for (int i = 0; i < blocks.size() - 1; i++) {
            BasicBlock block = blocks.get(i);
            TacInstruction last = block.lastInstruction();
            if (!isJump(last)) {
                continue;
            }

            BasicBlock fallthroughSuccessor = blocks.get(i + 1);

            // 检查所有后继是否都是默认 fallthrough 后继
            boolean onlyDefault = true;
            for (int successorId : block.successors()) {
                if (successorId != fallthroughSuccessor.id()) {
                    onlyDefault = false;
                    break;
                }
            }
            if (!onlyDefault) {
                continue;
            }

            // 删除跳转指令
            block.instructions().remove(block.instructions().size() - 1);

            // 条件跳转的 target == fallthrough 时产生重边，需去重
            if (block.successors().size() > 1) {
                int defaultId = fallthroughSuccessor.id();
                block.successors().clear();
                block.successors().add(defaultId);
                List<Integer> predecessors = fallthroughSuccessor.predecessors();
                while (predecessors.remove(Integer.valueOf(block.id()))) {
                    // 移除对本块所有重复引用
                }
                predecessors.add(block.id());
            }
            changed = true;
        }

        return changed;
    }

    private boolean isJump(TacInstruction inst) {
        return inst instanceof TacJump
               || inst instanceof TacJumpIfZero
               || inst instanceof TacJumpIfNotZero
               || inst instanceof TacJumpIfComparison;
    }
}
