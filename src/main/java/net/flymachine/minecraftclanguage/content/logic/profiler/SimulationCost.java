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
 * @param callOps           函数调用 / 间接跳转指令数
 * @param branchesTaken     发生跳转的分支数
 * @param branchesNotTaken  落空的条件分支数
 * @param fetchTranslations 取指地址翻译次数
 * @param loadTranslations  读地址翻译次数
 * @param storeTranslations 写地址翻译次数
 * @param loads             读访问明细
 * @param stores            写访问明细
 * @param ioLoads           设备读次数
 * @param ioStores          设备写次数
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
    long ioStores
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
     * 内存读总数
     */
    public long totalLoads() {
        return loads.total();
    }

    /**
     * 内存写总数
     */
    public long totalStores() {
        return stores.total();
    }

    /**
     * 地址翻译总数 (不含取指)
     */
    public long totalDataTranslations() {
        return loadTranslations + storeTranslations;
    }

    /**
     * 按给定权重计算加权代价
     */
    public double weightedCost(CostModel model) {
        return model.alu() * aluOps
               + model.mul() * mulOps
               + model.div() * divOps
               + model.floatOp() * floatOps
               + model.floatDiv() * floatDivOps
               + model.memoryAccess() * (totalLoads() + totalStores())
               + model.mmuTranslation() * totalDataTranslations()
               + model.branchTaken() * branchesTaken
               + model.branchNotTaken() * branchesNotTaken
               + model.call() * callOps
               + model.ioAccess() * (ioLoads + ioStores);
    }
}
