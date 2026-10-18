package net.flymachine.minecraftclanguage.content.logic.profiler;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 收集一次模拟过程中消耗的架构资源
 */
public final class LA64EmulatorProfiler implements CostRecorder {

    private final long[] classCounts = new long[CostClass.values().length];

    private final Map<String, CostClass> classCache = new HashMap<>();

    private long instructions;

    private long branchesTaken;
    private long branchesNotTaken;

    private long fetchTranslations;
    private long loadTranslations;
    private long storeTranslations;

    private long byteLoads;
    private long halfWordLoads;
    private long wordLoads;
    private long doubleWordLoads;

    private long byteStores;
    private long halfWordStores;
    private long wordStores;
    private long doubleWordStores;

    private long ioLoads;
    private long ioStores;

    // 供 afterExecute 判定分支是否落空
    private long expectedNextPc;
    private boolean lastBranchLike;

    public LA64EmulatorProfiler() { }

    public void reset() {
        Arrays.fill(classCounts, 0L);
        instructions = 0;
        branchesTaken = 0;
        branchesNotTaken = 0;
        fetchTranslations = 0;
        loadTranslations = 0;
        storeTranslations = 0;
        byteLoads = 0;
        halfWordLoads = 0;
        wordLoads = 0;
        doubleWordLoads = 0;
        byteStores = 0;
        halfWordStores = 0;
        wordStores = 0;
        doubleWordStores = 0;
        ioLoads = 0;
        ioStores = 0;
        expectedNextPc = 0;
        lastBranchLike = false;
    }

    private CostClass classOf(String mnemonic) {
        CostClass cached = classCache.get(mnemonic);
        if (cached == null) {
            cached = CostClass.classify(mnemonic);
            classCache.put(mnemonic, cached);
        }
        return cached;
    }

    @Override
    public void beforeExecute(long pc, String mnemonic) {
        ++instructions;
        CostClass costClass = classOf(mnemonic);
        ++classCounts[costClass.ordinal()];
        lastBranchLike = costClass.isBranchLike();
        expectedNextPc = pc + 4;
    }

    @Override
    public void afterExecute(long newPc) {
        if (!lastBranchLike) {
            // 非分支指令执行后 PC 必然 +4，无需比较
            return;
        }
        if (newPc == expectedNextPc) {
            ++branchesNotTaken;
        } else {
            ++branchesTaken;
        }
    }

    @Override
    public void recordFetchTranslation() {
        ++fetchTranslations;
    }

    @Override
    public void recordLoadTranslation() {
        ++loadTranslations;
    }

    @Override
    public void recordStoreTranslation() {
        ++storeTranslations;
    }

    @Override
    public void recordLoad(int width) {
        switch (width) {
            case 1 -> ++byteLoads;
            case 2 -> ++halfWordLoads;
            case 4 -> ++wordLoads;
            case 8 -> ++doubleWordLoads;
            default -> throw new IllegalArgumentException("Invalid load width: " + width);
        }
    }

    @Override
    public void recordStore(int width) {
        switch (width) {
            case 1 -> ++byteStores;
            case 2 -> ++halfWordStores;
            case 4 -> ++wordStores;
            case 8 -> ++doubleWordStores;
            default -> throw new IllegalArgumentException("Invalid store width: " + width);
        }
    }

    @Override
    public void recordIo(boolean isLoad) {
        if (isLoad) {
            ++ioLoads;
        } else {
            ++ioStores;
        }
    }

    /**
     * 当前计数的不可变快照
     */
    public SimulationCost snapshot() {
        return new SimulationCost(
            instructions,
            classCounts[CostClass.ALU.ordinal()],
            classCounts[CostClass.MUL.ordinal()],
            classCounts[CostClass.DIV.ordinal()],
            classCounts[CostClass.FLOAT.ordinal()],
            classCounts[CostClass.FLOAT_DIV.ordinal()],
            classCounts[CostClass.MEMORY.ordinal()],
            classCounts[CostClass.CALL.ordinal()],
            branchesTaken,
            branchesNotTaken,
            fetchTranslations,
            loadTranslations,
            storeTranslations,
            new SimulationCost.AccessCounts(byteLoads, halfWordLoads, wordLoads, doubleWordLoads),
            new SimulationCost.AccessCounts(byteStores, halfWordStores, wordStores, doubleWordStores),
            ioLoads,
            ioStores);
    }

    /**
     * 报告当前的模拟代价
     *
     * @param out   输出流
     * @param model 代价模型
     */
    public void report(PrintStream out, CostModel model) {
        SimulationCost cost = snapshot();

        out.println("=== Simulation Report ===");
        out.printf("%-20s: %d%n", "Insts", cost.instructions());
        out.printf("%-20s: %d/%d/%d%n", "  ALU/MUL/DIV", cost.aluOps(), cost.mulOps(), cost.divOps());
        out.printf("%-20s: %d/%d%n", "  FLOAT/FDIV", cost.floatOps(), cost.floatDivOps());
        out.printf("%-20s: %d/%d%n", "  MEMORY/CALL", cost.memoryOps(), cost.callOps());
        out.printf("%-20s: %d/%d%n", "Branch (T/NT)", cost.branchesTaken(), cost.branchesNotTaken());
        out.printf("%-20s: %d/%d/%d%n", "Addr Trans (F/L/S)",
                   cost.fetchTranslations(), cost.loadTranslations(), cost.storeTranslations());
        out.printf("%-20s: %d (%d/%d/%d/%d)%n", "Mem Load (B/H/W/D)",
                   cost.totalLoads(),
                   cost.loads().bytes(), cost.loads().halfWords(),
                   cost.loads().words(), cost.loads().doubleWords());
        out.printf("%-20s: %d (%d/%d/%d/%d)%n", "Mem Store (B/H/W/D)",
                   cost.totalStores(),
                   cost.stores().bytes(), cost.stores().halfWords(),
                   cost.stores().words(), cost.stores().doubleWords());
        out.printf("%-20s: %d/%d%n", "Device I/O (R/W)", cost.ioLoads(), cost.ioStores());
        out.printf("%-20s: %.1f%n", "Weighted Cost", cost.weightedCost(model));
        out.println("=== End of Report ===");
        out.println();
    }
}
