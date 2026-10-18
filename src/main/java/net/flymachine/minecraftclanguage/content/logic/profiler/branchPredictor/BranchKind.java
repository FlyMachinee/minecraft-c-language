package net.flymachine.minecraftclanguage.content.logic.profiler.branchPredictor;

/**
 * 分支指令的类别，决定预测器应当如何预测它。
 * <p>
 * 类别由 {@code jirl} 的寄存器用法区分：{@code jirl rd, rj, offs} 总会把 {@code PC+4}
 * 写入 {@code GR[rd]}，因此 {@code rd != 0} 表示这是一次「带链接」的跳转（调用）。
 */
public enum BranchKind {

    /**
     * 条件分支：beq / bne / blt / bge / bltu / bgeu / beqz / bnez / bceqz / bcnez
     */
    CONDITIONAL(true, false, false),

    /**
     * 无条件直接跳转：b
     */
    JUMP(true, false, false),

    /**
     * 直接调用：bl
     */
    CALL(true, false, false),

    /**
     * 函数返回：jirl r0, ra, offs，由 RAS 预测
     */
    RETURN(false, true, true),

    /**
     * 间接调用：jirl rd(rd != 0), rj, offs
     */
    INDIRECT_CALL(false, true, false),

    /**
     * 间接跳转：jirl r0, rj(rj != ra), offs，例如跳转表
     */
    INDIRECT_JUMP(false, true, false);

    private final boolean hasStaticTarget;
    private final boolean jirl;
    private final boolean returnViaRas;

    BranchKind(boolean hasStaticTarget, boolean jirl, boolean returnViaRas) {
        this.hasStaticTarget = hasStaticTarget;
        this.jirl = jirl;
        this.returnViaRas = returnViaRas;
    }

    public boolean hasStaticTarget() {
        return hasStaticTarget;
    }

    public boolean isJirl() {
        return jirl;
    }

    public boolean predictedByRas() {
        return returnViaRas;
    }

    public boolean pushesReturnAddress() {
        return this == CALL || this == INDIRECT_CALL;
    }

    public boolean popsReturnAddress() {
        return this == RETURN;
    }

    public boolean isUnconditional() {
        return this != CONDITIONAL;
    }
}
