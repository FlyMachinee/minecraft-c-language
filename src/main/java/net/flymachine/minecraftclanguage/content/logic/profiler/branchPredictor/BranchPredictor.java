package net.flymachine.minecraftclanguage.content.logic.profiler.branchPredictor;

/**
 * 直接映射 BTB + 环形 RAS
 * <p>
 * BTB 采用直接映射：index 取 {@code (pc >>> 2) & (SIZE-1)}
 */
public final class BranchPredictor {

    public static final int BTB_SIZE = 64;

    public static final int RAS_DEPTH = 32;

    private static final int PREDICT_TAKEN_THRESHOLD = 2;

    private final long[] btbTag = new long[BTB_SIZE];
    private final long[] btbTarget = new long[BTB_SIZE];
    private final boolean[] btbValid = new boolean[BTB_SIZE];
    private final int[] btbDirection = new int[BTB_SIZE];

    private final long[] ras = new long[RAS_DEPTH];
    private int rasNext = 0;
    private int rasSize = 0;

    public BranchPredictor() { }

    /**
     * 清空全部状态，用于每次模拟开始前复位
     */
    public void reset() {
        java.util.Arrays.fill(btbValid, false);
        java.util.Arrays.fill(btbTag, 0L);
        java.util.Arrays.fill(btbTarget, 0L);
        java.util.Arrays.fill(btbDirection, 0);
        java.util.Arrays.fill(ras, 0L);
        rasNext = 0;
        rasSize = 0;
    }

    private static int index(long pc) {
        return (((int) pc) >>> 2) & (BTB_SIZE - 1);
    }

    private long rasPeek() {
        return ras[(rasNext - 1 + RAS_DEPTH) % RAS_DEPTH];
    }

    private void rasPush(long returnAddress) {
        ras[rasNext] = returnAddress;
        rasNext = (rasNext + 1) % RAS_DEPTH;
        if (rasSize < RAS_DEPTH) {
            rasSize++;
        }
    }

    private void rasPop() {
        if (rasSize > 0) {
            rasNext = (rasNext - 1 + RAS_DEPTH) % RAS_DEPTH;
            rasSize--;
        }
    }

    public BranchPredictionStatus resolve(BranchInfo info, long actualNextPc) {
        int index = index(info.pc());
        boolean btbHit = btbValid[index] && btbTag[index] == info.pc();
        boolean taken = actualNextPc != info.fallThrough();
        long actualTarget = taken ? actualNextPc : info.fallThrough();

        BranchPredictionStatus outcome = check(info, index, btbHit, taken, actualTarget, actualNextPc);
        update(info, index, taken, actualTarget);
        return outcome;
    }

    private BranchPredictionStatus check(
        BranchInfo info, int index, boolean btbHit, boolean taken, long actualTarget, long actualNextPc) {

        BranchKind kind = info.kind();

        if (kind.predictedByRas()) {
            if (rasSize == 0) {
                return BranchPredictionStatus.RAS_MISPREDICT;
            }
            boolean hit = rasPeek() == actualNextPc;
            return hit ? BranchPredictionStatus.RAS_HIT : BranchPredictionStatus.RAS_MISPREDICT;
        }

        if (!btbHit) {
            return BranchPredictionStatus.BTB_MISS;
        }

        boolean predictedTaken = kind.isUnconditional() || btbDirection[index] >= PREDICT_TAKEN_THRESHOLD;

        if (predictedTaken != taken) {
            return BranchPredictionStatus.DIRECTION_MISMATCH;
        }
        if (predictedTaken && btbTarget[index] != actualTarget) {
            return BranchPredictionStatus.TARGET_MISMATCH;
        }
        return predictedTaken ? BranchPredictionStatus.BTB_HIT_TAKEN : BranchPredictionStatus.BTB_HIT_NOT_TAKEN;
    }

    private void update(BranchInfo info, int index, boolean taken, long actualTarget) {
        BranchKind kind = info.kind();

        btbValid[index] = true;
        btbTag[index] = info.pc();
        if (taken) {
            btbTarget[index] = actualTarget;
            if (btbDirection[index] < 3) {
                btbDirection[index]++;
            }
        } else {
            if (btbDirection[index] > 0) {
                btbDirection[index]--;
            }
        }

        if (kind.pushesReturnAddress()) {
            rasPush(info.fallThrough());
        } else if (kind.popsReturnAddress()) {
            rasPop();
        }
    }
}
