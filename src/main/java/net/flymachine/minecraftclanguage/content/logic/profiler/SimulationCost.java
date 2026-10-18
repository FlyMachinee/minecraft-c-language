package net.flymachine.minecraftclanguage.content.logic.profiler;

/**
 * 一次模拟后得到的统计结果
 *
 * @param instructions      执行的指令总数
 * @param aluOps            ALU 类指令数
 * @param mulOps            乘法指令数
 * @param divOps            除法与取模指令数
 * @param floatOps          浮点运算指令数
 * @param floatDivOps       浮点除法指令数
 * @param memoryOps         访存指令数（指令条数，不含跨页放大）
 * @param callOps           控制转移类指令数（{@code bl} 与全部 {@code jirl}）
 * @param branchesTaken     发生跳转的分支数
 * @param branchesNotTaken  未跳转的条件分支数
 * @param fetchTranslations 取指地址翻译次数
 * @param loadTranslations  读地址翻译次数
 * @param storeTranslations 写地址翻译次数
 * @param loads             读访问明细
 * @param stores            写访问明细
 * @param ioLoads           设备读次数
 * @param ioStores          设备写次数
 * @param branchPrediction  分支预测器明细，控制转移代价的唯一来源
 */
public record SimulationCost(
    long instructions,
    long aluOps,
    long mulOps,
    long divOps,
    long floatOps,
    long floatDivOps,
    long memoryOps,
    long callOps,
    long branchesTaken,
    long branchesNotTaken,
    long fetchTranslations,
    long loadTranslations,
    long storeTranslations,
    AccessCounts loads,
    AccessCounts stores,
    long ioLoads,
    long ioStores,
    BranchPredictionStats branchPrediction
) {

    /**
     * 按位宽细分的内存访问次数
     */
    public record AccessCounts(long bytes, long halfWords, long words, long doubleWords) {
        public static final AccessCounts ZERO = new AccessCounts(0, 0, 0, 0);

        public long total() {
            return bytes + halfWords + words + doubleWords;
        }
    }

    /**
     * 分支预测器明细
     *
     * @param btbHitTaken         BTB 命中，预测 taken 且实际 taken、目标一致
     * @param btbHitNotTaken      BTB 命中，预测 not-taken 且实际 not-taken
     * @param rasHits             RAS 命中且返回地址正确
     * @param btbMisses           BTB 无有效条目（首次执行或索引冲突）
     * @param directionMispredict 方向预测错误
     * @param targetMispredict    方向正确但目标错误（多目标间接跳转的典型表现）
     * @param rasMispredict       RAS 预测错误或下溢
     * @param indirectJumpCount   间接跳转次数（{@code jirl} 且目标不确定），函数返回不计入
     */
    public record BranchPredictionStats(
        long btbHitTaken,
        long btbHitNotTaken,
        long rasHits,
        long btbMisses,
        long directionMispredict,
        long targetMispredict,
        long rasMispredict,
        long indirectJumpCount
    ) {

        public static final BranchPredictionStats ZERO =
            new BranchPredictionStats(0, 0, 0, 0, 0, 0, 0, 0);

        public long totalBranches() {
            return btbHitTaken + btbHitNotTaken + rasHits + btbMisses
                   + directionMispredict + targetMispredict + rasMispredict;
        }

        public long totalCorrect() {
            return btbHitTaken + btbHitNotTaken + rasHits;
        }

        public long totalMispredicts() {
            return directionMispredict + targetMispredict + rasMispredict;
        }

        public double accuracy() {
            long total = totalBranches();
            return total == 0 ? 0.0 : (double) totalCorrect() / total;
        }
    }

    public long totalLoads() {
        return loads.total();
    }

    public long totalStores() {
        return stores.total();
    }

    public long totalDataTranslations() {
        return loadTranslations + storeTranslations;
    }

    /**
     * 按给定权重计算加权代价
     */
    public double weightedCost(CostModel model) {
        CostModel.BranchPredictionCosts prediction = model.branchPrediction();
        BranchPredictionStats branches = branchPrediction;

        return model.alu() * aluOps
               + model.mul() * mulOps
               + model.div() * divOps
               + model.floatOp() * floatOps
               + model.floatDiv() * floatDivOps
               + model.memoryAccess() * (totalLoads() + totalStores())
               + model.mmuTranslation() * totalDataTranslations()
               + model.ioAccess() * (ioLoads + ioStores)
               + prediction.btbHitTaken() * branches.btbHitTaken()
               + prediction.btbHitNotTaken() * branches.btbHitNotTaken()
               + prediction.rasHit() * branches.rasHits()
               + prediction.miss() * branches.btbMisses()
               + prediction.mispredict() * branches.totalMispredicts()
               + prediction.indirect() * branches.indirectJumpCount();
    }
}
