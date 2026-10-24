package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.abstraction;

import java.util.BitSet;

/**
 * 数据流问题的抽象接口
 */
public interface DataFlowProblem {
    enum Direction {
        FORWARD, BACKWARD
    }

    enum Meet {
        UNION, INTERSECTION
    }

    Direction direction();

    Meet meet();

    /**
     * after[B] = transfer(B, before[B])
     * <p>
     * 创造新的 BitSet 作为返回值
     */
    BitSet transfer(int blockId, BitSet before);

    void init(BitSet[] afters, int n);
}
