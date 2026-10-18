package net.flymachine.minecraftclanguage.content.logic.profiler;

/**
 * 各类指令的消耗权重
 *
 * @param alu              ALU 类指令
 * @param mul              乘法
 * @param div              除法与取模
 * @param floatOp          浮点运算
 * @param floatDiv         浮点除法
 * @param memoryAccess     内存访问次数
 * @param mmuTranslation   地址翻译次数 (仅统计读/写)
 * @param ioAccess         设备 I/O 访问
 * @param branchPrediction 控制转移权重，全部按预测器结果计价
 */
public record CostModel(
    double alu,
    double mul,
    double div,
    double floatOp,
    double floatDiv,
    double memoryAccess,
    double mmuTranslation,
    double ioAccess,
    BranchPredictionCosts branchPrediction
) {

    public static final CostModel ZERO = new CostModel(
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        BranchPredictionCosts.ZERO);

    public static final CostModel LA64_DEFAULT = new CostModel(
        1.0,
        4.0,
        5.0,
        5.0,
        8.0,
        1.0,
        3.0,
        50.0,
        BranchPredictionCosts.LA64_DEFAULT
    );

    public CostModel {
        requireNonNegative(alu, "alu");
        requireNonNegative(mul, "mul");
        requireNonNegative(div, "div");
        requireNonNegative(floatOp, "floatOp");
        requireNonNegative(floatDiv, "floatDiv");
        requireNonNegative(memoryAccess, "memoryAccess");
        requireNonNegative(mmuTranslation, "mmuTranslation");
        requireNonNegative(ioAccess, "ioAccess");
        if (branchPrediction == null) {
            throw new IllegalArgumentException("branchPrediction must not be null");
        }
    }

    /**
     * 控制转移的权重
     *
     * @param btbHitTaken    BTB 命中且预测 taken 正确
     * @param btbHitNotTaken BTB 命中且预测 not-taken 正确
     * @param rasHit         RAS 命中且返回地址正确
     * @param miss           BTB 无有效条目 (首次执行或索引冲突)，没有可用预测
     * @param mispredict     预测失败，需要冲刷流水线
     * @param indirect       每次间接跳转 ({@code jirl} 且目标不确定) 的额外代价，目标从寄存器来
     */
    public record BranchPredictionCosts(
        double btbHitTaken,
        double btbHitNotTaken,
        double rasHit,
        double miss,
        double mispredict,
        double indirect
    ) {

        public static final BranchPredictionCosts ZERO =
            new BranchPredictionCosts(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

        /**
         * rasHit < btbHitNotTaken < btbHitTaken < miss << mispredict
         */
        public static final BranchPredictionCosts LA64_DEFAULT =
            new BranchPredictionCosts(
                1.5,
                0.5,
                0.2,
                3.0,
                15.0,
                2.0
            );

        public BranchPredictionCosts {
            requireNonNegative(btbHitTaken, "branchPrediction.btbHitTaken");
            requireNonNegative(btbHitNotTaken, "branchPrediction.btbHitNotTaken");
            requireNonNegative(rasHit, "branchPrediction.rasHit");
            requireNonNegative(miss, "branchPrediction.miss");
            requireNonNegative(mispredict, "branchPrediction.mispredict");
            requireNonNegative(indirect, "branchPrediction.indirect");
        }
    }

    private static void requireNonNegative(double value, String name) {
        if (value < 0.0) {
            throw new IllegalArgumentException(name + " must not be negative, but was " + value);
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be finite, but was " + value);
        }
    }
}
