package net.flymachine.minecraftclanguage.content.logic.architecture.la64.register;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class LA64RegisterResolver {
    private final Map<String, LA64Register> nameToRegister;
    private final EnumMap<LA64Register.RegType, LA64Register[]> registersByType;

    private LA64RegisterResolver() {
        nameToRegister = new HashMap<>();
        registersByType = new EnumMap<>(LA64Register.RegType.class);
        for (LA64Register.RegType type : LA64Register.RegType.values()) {
            registersByType.put(type, new LA64Register[32]);
        }

        // 加载通用寄存器
        for (GeneralPurposeRegister reg : GeneralPurposeRegister.values()) {
            register(reg);
        }
        // 加载浮点寄存器
        for (FloatingPointRegister reg : FloatingPointRegister.values()) {
            register(reg);
        }
        // 加载条件标志寄存器
        for (ConditionFlagRegister reg : ConditionFlagRegister.values()) {
            register(reg);
        }
    }

    private void register(LA64Register reg) {
        // 填充名称映射
        for (String name : reg.getNames()) {
            String lowerName = name.toLowerCase();
            LA64Register existing = nameToRegister.put(lowerName, reg);
            if (existing != null && existing != reg) {
                throw new IllegalStateException(String.format(
                    "Duplicate register name '%s' for %s and %s",
                    name,
                    existing,
                    reg));
            }
        }
        // 填充编号映射
        LA64Register[] arr = registersByType.get(reg.getType());
        if (reg.getNumber() >= 0 && reg.getNumber() < arr.length) {
            arr[reg.getNumber()] = reg;
        }
    }

    private static final class Holder {
        static final LA64RegisterResolver INSTANCE = new LA64RegisterResolver();
    }

    public static LA64RegisterResolver getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 根据名称查找寄存器（不区分大小写）
     *
     * @param name 寄存器名称，如 "a0", "r4", "fa0", "f3" 等
     * @return Optional 包装的 LA64Register 对象
     */
    public Optional<LA64Register> findRegister(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(nameToRegister.get(name.trim().toLowerCase()));
    }

    /**
     * 查询字符串是否是一个有效的寄存器名称（不区分大小写）
     *
     * @param str 字符串
     * @return 如果是有效的寄存器名称则返回 {@code true}，否则返回 {@code false}
     */
    public boolean isRegisterName(String str) {
        if (str == null || str.isBlank()) {
            return false;
        }
        return nameToRegister.containsKey(str.trim().toLowerCase());
    }

    /**
     * 根据类型和编号查找寄存器
     *
     * @param type   寄存器类型
     * @param number 寄存器编号
     * @return 对应的寄存器，如果无效则返回空
     */
    public Optional<LA64Register> getRegister(LA64Register.RegType type, int number) {
        LA64Register[] arr = registersByType.get(type);
        if (arr == null || number < 0 || number >= arr.length) {
            return Optional.empty();
        }
        return Optional.ofNullable(arr[number]);
    }

    /**
     * 根据编号查找通用寄存器
     *
     * @param number 寄存器编号
     * @return 对应的通用寄存器，如果无效则返回空
     */
    public Optional<LA64Register> getGeneralPurposeRegister(int number) {
        return getRegister(LA64Register.RegType.GPR, number);
    }

    /**
     * 根据编号查找浮点寄存器
     *
     * @param number 寄存器编号
     * @return 对应的浮点寄存器，如果无效则返回空
     */
    public Optional<LA64Register> getFloatingPointRegister(int number) {
        return getRegister(LA64Register.RegType.FPR, number);
    }

    /**
     * 根据编号查找条件标志寄存器
     *
     * @param number 寄存器编号
     * @return 对应的条件标志寄存器，如果无效则返回空
     */
    public Optional<LA64Register> getConditionFlagRegister(int number) {
        return getRegister(LA64Register.RegType.CFR, number);
    }
}
