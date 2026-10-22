package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.reachingCopies;

import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;

import java.util.BitSet;

public final class ReachingCopiesResult {

    private final BitSet[] in;
    private final BitSet[] out;
    private final CopyInterner interner;

    public ReachingCopiesResult(BitSet[] in, BitSet[] out, CopyInterner interner) {
        this.in = in;
        this.out = out;
        this.interner = interner;
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

    public CopyInterner interner() {
        return interner;
    }
}
