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

    static Type commonRealType(Type t1, Type t2) {
        if (!(t1 instanceof BasicType basicType1) || !(t2 instanceof BasicType basicType2)) {
            return ErrorType.INSTANCE;
        } else {
            return commonRealType(basicType1, basicType2);
        }
    }

    static Type commonRealType(BasicType t1, BasicType t2) {
        if (!t1.isArithmetic() || !t2.isArithmetic()) {
            return ErrorType.INSTANCE;
        }
        // 否则，若一个操作数是 double、double complex 或 double imaginary，则会按下列方式隐式转换另一操作数：
        // 整数或实浮点数类型转换成 double
        // 复数类型转换成 double complex
        // 虚数类型转换成 double imaginary
        if (t1 == BasicType.DOUBLE || t2 == BasicType.DOUBLE) {
            return BasicType.DOUBLE;
        }
        // 否则两个操作数均为整数。两个操作数都会经历整数提升；经过整数提升后，适用于以下情况之一：
        // 若两类型相同，则该类型即为公共类型
        if (t1 == t2) {
            return t1;
        }
        // 否则，两类型不同：
        // 若两类型有相同的符号性（均为有符号或均为无符号），则拥有较低转换等级者会隐式转换为另一类型
        if (t1.isSigned() && t2.isSigned() || t1.isUnsigned() && t2.isUnsigned()) {
            if (t1.sizeof() < t2.sizeof()) {
                return t2;
            } else {
                return t1;
            }
        }
        // 否则，两者符号性不同：
        if (t1.isUnsigned()) {
            // 左为无符号类型
            // 若无符号类型的转换等级大于或等于有符号类型的等级，则有符号类型操作数会隐式转换成无符号类型
            if (t1.sizeof() >= t2.sizeof()) {
                return t1;
            }
            // 否则，无符号类型的转换等级小于有符号类型：
            // 若有符号类型可以表达无符号类型的所有值，则无无符号类型的操作数被隐式转换成有符号操作数的类型
            return t2;
        } else {
            // 右为无符号类型
            // 若无符号类型的转换等级大于或等于有符号类型的等级，则有符号类型操作数会隐式转换成无符号类型
            if (t2.sizeof() >= t1.sizeof()) {
                return t2;
            }
            // 否则，无符号类型的转换等级小于有符号类型：
            // 若有符号类型可以表达无符号类型的所有值，则无无符号类型的操作数被隐式转换成有符号操作数的类型
            return t1;
        }
    }
}
