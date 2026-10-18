package net.flymachine.minecraftclanguage.content.logic.profiler;

/**
 * 各类指令的消耗权重
 *
 * @param alu            ALU 类指令
 * @param mul            乘法
 * @param div            除法与取模
 * @param floatOp        浮点运算
 * @param floatDiv       浮点除法
 * @param memoryAccess   内存访问次数
 * @param mmuTranslation 地址翻译次数 (仅统计读/写)
 * @param branchTaken    分支命中
 * @param branchNotTaken 分支落空
 * @param call           函数调用/间接跳转
 * @param ioAccess       设备 I/O 访问
 */
public record CostModel(
    double alu,
    double mul,
    double div,
    double floatOp,
    double floatDiv,
    double memoryAccess,
    double mmuTranslation,
    double branchTaken,
    double branchNotTaken,
    double call,
    double ioAccess
) {

    public static final CostModel ZERO = new CostModel(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

    public static final CostModel LA64_DEFAULT = new CostModel(
        1.0,
        4.0,
        5.0,
        5.0,
        8.0,
        1.0,
        3.0,
        1.0,
        5.0,
        10.0,
        50.0
    );

    public CostModel {
        requireNonNegative(alu, "alu");
        requireNonNegative(mul, "mul");
        requireNonNegative(div, "div");
        requireNonNegative(floatOp, "floatOp");
        requireNonNegative(floatDiv, "floatDiv");
        requireNonNegative(memoryAccess, "memoryAccess");
        requireNonNegative(mmuTranslation, "mmuTranslation");
        requireNonNegative(branchTaken, "branchTaken");
        requireNonNegative(branchNotTaken, "branchNotTaken");
        requireNonNegative(call, "call");
        requireNonNegative(ioAccess, "ioAccess");
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
