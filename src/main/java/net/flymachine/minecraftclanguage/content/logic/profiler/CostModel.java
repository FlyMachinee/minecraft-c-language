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
     * @param base              分支指令自身的执行成本，与 ALU 同量级
     * @param takenRedirect     预测 taken 成功时的取指重定向
     * @param rasRedirect       RAS 命中时的取指重定向 (返回地址在调用时已压栈，几乎为零)
     * @param missPenalty       BTB 无有效条目时等待目标解析
     * @param mispredictPenalty 预测失败时冲刷流水线
     * @param indirectPenalty   每次间接跳转的额外目标解析代价 (与预测成败无关)
     */
    public record BranchPredictionCosts(
        double base,
        double takenRedirect,
        double rasRedirect,
        double missPenalty,
        double mispredictPenalty,
        double indirectPenalty
    ) {

        public static final BranchPredictionCosts ZERO =
            new BranchPredictionCosts(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

        public static final BranchPredictionCosts LA64_DEFAULT =
            new BranchPredictionCosts(
                1.0,
                0.5,
                0.2,
                3.0,
                15.0,
                2.0
            );

        public BranchPredictionCosts {
            requireNonNegative(base, "branchPrediction.base");
            requireNonNegative(takenRedirect, "branchPrediction.takenRedirect");
            requireNonNegative(rasRedirect, "branchPrediction.rasRedirect");
            requireNonNegative(missPenalty, "branchPrediction.missPenalty");
            requireNonNegative(mispredictPenalty, "branchPrediction.mispredictPenalty");
            requireNonNegative(indirectPenalty, "branchPrediction.indirectPenalty");
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
