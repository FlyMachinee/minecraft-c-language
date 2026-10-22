package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.liveness;

import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;

import java.util.BitSet;

/**
 * 活跃变量分析的结果，按块 ID 索引
 */
public final class LivenessResult {
    private final BitSet[] in;
    private final BitSet[] out;

    public LivenessResult(BitSet[] in, BitSet[] out) {
        this.in = in;
        this.out = out;
    }

    public BitSet in(BasicBlock block) {
        return in[block.id()];
    }

    public BitSet in(int blockId) {
        return in[blockId];
    }

    public BitSet out(BasicBlock block) {
        return out[block.id()];
    }

    public BitSet out(int blockId) {
        return out[blockId];
    }
}
