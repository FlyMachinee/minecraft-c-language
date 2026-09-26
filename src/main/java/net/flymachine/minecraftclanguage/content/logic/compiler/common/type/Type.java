package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;

public abstract class Type {

    abstract TypeKind kind();

    public final boolean isBasic() {
        return kind() == TypeKind.BASIC;
    }

    public final boolean isVoid() {
        return kind() == TypeKind.VOID;
    }

    public final boolean isFunction() {
        return kind() == TypeKind.FUNCTION;
    }

    public final boolean isPointer() {
        return kind() == TypeKind.POINTER;
    }

    public final boolean isArray() {
        return kind() == TypeKind.ARRAY;
    }

    public final boolean isError() {
        return kind() == TypeKind.ERROR;
    }

    public abstract String format(String declarator);

    public String typename() {
        return this.format("");
    }

    protected final boolean isConst;

    protected Type(boolean isConst) {
        this.isConst = isConst;
    }

    public boolean isConst() {
        return this.isConst;
    }

    public abstract Type setConst(boolean isConst);

    public final Type addConst() {
        return this.setConst(true);
    }

    public final Type removeConst() {
        return this.setConst(false);
    }

    public final Type removeQualifiers() {
        return this.removeConst();
    }

    public abstract boolean isCompatible(Type other);

    public abstract Type merge(Type other);

    /**
     * 完整类型：在编译期内可确定大小的类型
     * <p>
     * 不完整类型包括：void、大小未知的数组、内容未知的结构体或联合体类型
     */
    public abstract boolean isComplete();

    /**
     * 算术类型：整数类型和浮点数类型
     */
    public final boolean isArithmetic() {
        return isBasic();
    }

    /**
     * 标量类型：算术类型和指针类型
     */
    public final boolean isScalar() {
        return isBasic() || isPointer();
    }

    /**
     * 整数类型：char、有符号整数类型、无符号整数类型、枚举类型
     */
    public final boolean isInteger() {
        return isBasic() && ((BasicType) this).primitive() != BasicType.Primitive.DOUBLE;
    }

    /**
     * 实数类型：整数类型和实浮点数类型
     */
    public final boolean isReal() {
        return isBasic();
    }

    /**
     * 对象类型：不是函数类型、且要求完整
     */
    public final boolean isObject() {
        return !isFunction() && isComplete();
    }

    /**
     * 聚合类型：数组类型、结构体类型
     */
    public final boolean isAggregate() {
        return isArray();
    }


    public final boolean isInt() {
        return isBasic() && ((BasicType) this).primitive() == BasicType.Primitive.INT;
    }

    public final boolean isLong() {
        return isBasic() && ((BasicType) this).primitive() == BasicType.Primitive.LONG;
    }

    public final boolean isUnsignedInt() {
        return isBasic() && ((BasicType) this).primitive() == BasicType.Primitive.UNSIGNED_INT;
    }

    public final boolean isUnsignedLong() {
        return isBasic() && ((BasicType) this).primitive() == BasicType.Primitive.UNSIGNED_LONG;
    }

    public final boolean isDouble() {
        return isBasic() && ((BasicType) this).primitive() == BasicType.Primitive.DOUBLE;
    }

    public abstract long sizeof();

    public abstract AsmType toAsmType();

    public static BasicType commonRealType(BasicType t1, BasicType t2) {
        if (!t1.isArithmetic() || !t2.isArithmetic()) {
            throw new IllegalArgumentException("can only handle arithmetic types");
        }
        // 否则，若一个操作数是 double、double complex 或 double imaginary，则会按下列方式隐式转换另一操作数：
        // 整数或实浮点数类型转换成 double
        // 复数类型转换成 double complex
        // 虚数类型转换成 double imaginary
        if (t1.isDouble() || t2.isDouble()) {
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

    public static String wrapIfPointer(String declarator) {
        String trimmed = declarator.trim();
        if (trimmed.isEmpty()) { return declarator; }
        if (trimmed.startsWith("*")) {
            return "(" + declarator + ")";
        }
        return declarator;
    }
}
