package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.liveness;

import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.common.VariableInterner;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;

import java.util.BitSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 活跃变量分析的结果，按块 ID 索引
 */
public final class LivenessResult {
    private final BitSet[] in;
    private final BitSet[] out;
    private final BitSet staticVars;
    private final VariableInterner interner;

    public LivenessResult(BitSet[] in, BitSet[] out, BitSet staticVars, VariableInterner interner) {
        this.in = in;
        this.out = out;
        this.staticVars = staticVars;
        this.interner = interner;
    }

    /**
     * 函数内可见的静态变量集合。函数调用可能读写它们
     */
    public BitSet staticVars() {
        return staticVars;
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

    public VariableInterner interner() {
        return interner;
    }

    public Set<String> inNames(BasicBlock block) {
        return names(in[block.id()]);
    }

    public Set<String> outNames(BasicBlock block) {
        return names(out[block.id()]);
    }

    private Set<String> names(BitSet set) {
        Set<String> result = new LinkedHashSet<>();
        for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1)) {
            result.add(interner.nameOf(i));
        }
        return result;
    }
}
