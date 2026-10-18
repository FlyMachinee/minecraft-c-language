package net.flymachine.minecraftclanguage.content.logic.profiler;

import net.flymachine.minecraftclanguage.content.logic.profiler.branchPredictor.BranchInfo;

/**
 * 代价计数的接收端
 * <p>
 * 避免 cpu/memory等 反向依赖 emulator
 * <p>
 * 实现类不应在此做任何 I/O、字符串拼接或集合分配
 */
public interface CostRecorder {

    /**
     * 执行一条指令之前调用
     *
     * @param pc       本条指令的地址
     * @param mnemonic 指令助记符
     */
    void beforeExecute(long pc, String mnemonic);

    /**
     * 一条指令执行完成之后调用
     *
     * @param newPc 执行后的 PC
     */
    void afterExecute(long newPc);

    /**
     * 一次取指的地址翻译
     */
    void recordFetchTranslation();

    /**
     * 一次数据读取的地址翻译
     */
    void recordLoadTranslation();

    /**
     * 一次数据写入的地址翻译
     */
    void recordStoreTranslation();

    /**
     * 一次数据读取，按访问位宽区分
     *
     * @param width 字节数，取值 1 / 2 / 4 / 8
     */
    void recordLoad(int width);

    /**
     * 一次数据写入，按访问位宽区分
     *
     * @param width 字节数，取值 1 / 2 / 4 / 8
     */
    void recordStore(int width);

    /**
     * 一次设备（终端）访问
     */
    void recordIo(boolean isLoad);

    /**
     * 结算分支预测结果，更新预测器状态
     *
     * @param info         分支信息
     * @param actualNextPc 执行后的实际 PC
     */
    void resolveBranch(BranchInfo info, long actualNextPc);
}
