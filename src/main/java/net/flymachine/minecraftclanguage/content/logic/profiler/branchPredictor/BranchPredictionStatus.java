package net.flymachine.minecraftclanguage.content.logic.profiler.branchPredictor;

/**
 * 分支预测情况
 */
public enum BranchPredictionStatus {

    /**
     * BTB 命中，预测 taken 且实际 taken、目标一致
     */
    BTB_HIT_TAKEN,

    /**
     * BTB 命中，预测 not-taken 且实际 not-taken
     */
    BTB_HIT_NOT_TAKEN,

    /**
     * RAS 命中，返回地址正确
     */
    RAS_HIT,

    /**
     * BTB 无有效条目 (首次执行或索引冲突)，没有可用预测
     */
    BTB_MISS,

    /**
     * BTB 有目标，但方向 (是否跳转) 预测错误
     */
    DIRECTION_MISMATCH,

    /**
     * 方向预测正确，但目标地址与预测不符
     */
    TARGET_MISMATCH,

    /**
     * 返回地址预测错误，或 RAS 下溢 (没有可用的返回地址)
     */
    RAS_MISPREDICT;

    public boolean isCorrect() {
        return this == BTB_HIT_TAKEN || this == BTB_HIT_NOT_TAKEN || this == RAS_HIT;
    }

    public boolean isMispredict() {
        return this == DIRECTION_MISMATCH || this == TARGET_MISMATCH || this == RAS_MISPREDICT;
    }
}
