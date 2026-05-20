package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;

public interface Type {
    boolean isCompatible(Type other);

    /**
     * 完整类型：在编译期内可确定大小的类型
     * <p>
     * 不完整类型包括：void、大小未知的数组、内容未知的结构体或联合体类型
     */
    boolean isComplete();

    /**
     * 算术类型：整数类型和浮点数类型
     */
    boolean isArithmetic();

    /**
     * 标量类型：算术类型和指针类型
     */
    boolean isScalar();

    /**
     * 整数类型：char、有符号整数类型、无符号整数类型、枚举类型
     */
    boolean isInteger();

    /**
     * 实数类型：整数类型和实浮点数类型
     */
    boolean isReal();

    default boolean isVoid() {
        return this instanceof BasicType basicType && basicType == BasicType.VOID;
    }

    long sizeof();

    AsmType toAsmType();

    <R> R accept(TypeVisitor<R> visitor);

    static BasicType commonRealType(BasicType t1, BasicType t2) {
        if (t1 == t2) {
            return t1;
        } else {
            return BasicType.LONG;
        }
    }
}
