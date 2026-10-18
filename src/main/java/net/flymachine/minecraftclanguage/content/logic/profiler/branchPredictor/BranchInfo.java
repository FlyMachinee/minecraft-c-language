package net.flymachine.minecraftclanguage.content.logic.profiler.branchPredictor;

/**
 * 一条分支指令在预测阶段所需的全部信息。
 *
 * @param pc           分支指令自身的地址
 * @param kind         分支类别
 * @param staticTarget 目标地址；当 {@link BranchKind#hasStaticTarget()} 为 {@code false} 时该值无意义
 */
public record BranchInfo(long pc, BranchKind kind, long staticTarget) {

    public BranchInfo {
        if (pc % 4 != 0) {
            throw new IllegalArgumentException(
                "Branch instruction address must be 4-byte aligned: 0x" + Long.toHexString(pc));
        }
    }

    public long fallThrough() {
        return pc + 4;
    }
}
