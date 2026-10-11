package net.flymachine.minecraftclanguage.content.logic.architecture.la64.abi.lp64d.callConv;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class CallingConvention {

    public static final int MAX_GAR = 8;
    public static final int MAX_FAR = 8;
    public static final int GRLEN = 8;

    private CallingConvention() { }

    /**
     * 根据调用约定，分配函数参数和返回值的寄存器/栈位置
     *
     * @param funcType 函数类型
     */
    public static Signature allocate(FunctionType funcType) {
        return allocate(funcType.returnType(), funcType.parameterTypes());
    }

    /**
     * 根据调用约定，分配函数参数和返回值的寄存器/栈位置
     *
     * @param returnType 返回类型
     * @param paramTypes 参数类型列表
     */
    public static Signature allocate(@NotNull Type returnType, @NotNull List<Type> paramTypes) {
        State s = new State();

        // 先处理返回值：大结构体返回会占用 a0 作为隐藏指针
        Placement ret = allocateReturn(returnType, s);

        // 再分配参数
        List<Placement> params = new ArrayList<>(paramTypes.size());
        for (Type t : paramTypes) {
            params.add(allocateParam(t, s));
        }
        return new Signature(List.copyOf(params), ret, s.stackOffset);
    }

    private record FlatInfo(int floatCount, int intCount, boolean isFirstFloat) {
        boolean onlyFloat() {
            return intCount == 0 && floatCount > 0;
        }

        boolean onlyInt() {
            return floatCount == 0 && intCount > 0;
        }
    }

    private static FlatInfo flatten(Type type) {
        if (type.isDouble()) {
            return new FlatInfo(1, 0, true);
        }
        if (type.isInteger() || type.isPointer()) {
            return new FlatInfo(0, 1, false);
        }
        if (type instanceof ArrayType at) {
            int n = (int) at.size().value();
            FlatInfo elem = flatten(at.elementType());
            return new FlatInfo(elem.floatCount() * n, elem.intCount() * n, elem.isFirstFloat());
        }
        if (type instanceof StructType st) {
            int f = 0, i = 0;
            boolean firstFloat = false;
            List<Field> fields = st.fields();
            for (int idx = 0; idx < fields.size(); idx++) {
                Field field = fields.get(idx);
                FlatInfo fi = flatten(field.type);
                if (idx == 0) {
                    firstFloat = fi.isFirstFloat();
                }
                f += fi.floatCount();
                i += fi.intCount();
            }
            return new FlatInfo(f, i, firstFloat);
        }
        throw new IllegalArgumentException("Unsupported type for flatten: " + type);
    }

    private static Placement allocateParam(Type type, State s) {
        if (type.isScalar()) {
            return allocateScalar(type, s);
        }
        if (type instanceof StructType st) {
            return allocateStruct(st, s);
        }
        throw new IllegalArgumentException("Unsupported parameter type: " + type);
    }

    private static Placement allocateScalar(Type type, State s) {
        return type.isDouble() ? allocateSingleFar(s) : allocateSingleGar(s);
    }

    private static Placement allocateStruct(StructType st, State s) {
        long size = st.sizeof();
        if (size == 0) {
            throw new IllegalArgumentException("Empty struct is not supported");
        }

        // 若一个聚合体位宽大于 2 * GRLEN，则它通过引用传递
        if (size > 2 * GRLEN) {
            return allocateByReference(s);
        }

        FlatInfo fi = flatten(st);

        if (fi.onlyFloat()) {
            return fi.floatCount == 1 ? allocateSingleFar(s) : allocateFarPair(s);
        }

        if (fi.onlyInt()) {
            return size <= GRLEN ? allocateSingleGar(s) : allocateGarPair(s);
        }

        // 混合情况
        // 浮点数只有 double，8 字节不可能同时装下 double 和整数
        // 从而 8 < size ≤ 16
        if (fi.floatCount() == 1 && fi.intCount() == 1) {
            return allocateMixedFarGar(fi, s);
        }
        return allocateGarPair(s);
    }

    private static Placement allocateSingleGar(State s) {
        if (s.garUsed < MAX_GAR) {
            return one(new Slot.GAR(s.garUsed++));
        }
        return allocStack(s, GRLEN);
    }

    private static Placement allocateSingleFar(State s) {
        if (s.farUsed < MAX_FAR) {
            return one(new Slot.FAR(s.farUsed++));
        }
        return allocateSingleGar(s);
    }

    private static Placement allocateGarPair(State s) {
        return switch (MAX_GAR - s.garUsed) {
            case 0 -> allocStack(s, 2 * GRLEN);
            case 1 -> {
                List<Slot> out = List.of(new Slot.GAR(s.garUsed++), new Slot.Stack(s.stackOffset));
                s.stackOffset += GRLEN;
                yield new Placement.InSlots(out);
            }
            default -> slots(new Slot.GAR(s.garUsed++), new Slot.GAR(s.garUsed++));
        };
    }

    private static Placement allocateFarPair(State s) {
        if (s.farUsed + 2 <= MAX_FAR) {
            return slots(new Slot.FAR(s.farUsed++), new Slot.FAR(s.farUsed++));
        }
        return allocateGarPair(s);
    }

    private static Placement allocateMixedFarGar(FlatInfo fi, State s) {
        if (s.farUsed < MAX_FAR && s.garUsed < MAX_GAR) {
            if (fi.isFirstFloat) {
                return slots(new Slot.FAR(s.farUsed++), new Slot.GAR(s.garUsed++));
            }
            return slots(new Slot.GAR(s.garUsed++), new Slot.FAR(s.farUsed++));
        }
        return allocateGarPair(s);
    }

    private static Placement allocateByReference(State s) {
        Slot pointerSlot;
        if (s.garUsed < MAX_GAR) {
            pointerSlot = new Slot.GAR(s.garUsed++);
        } else {
            pointerSlot = new Slot.Stack(s.stackOffset);
            s.stackOffset += GRLEN;
        }
        return new Placement.ByReference(pointerSlot);

    }

    private static Placement allocateReturn(Type type, State s) {
        if (type.isVoid()) {
            return new Placement.Ignored();
        }

        // 返回值的传递方式与第一个同类型命名参数的传递方式相同
        Placement ret = allocateParam(type, new State());
        if (ret instanceof Placement.ByReference) {
            // 如果这样的实参是通过引用传递的，则调用者为返回值分配内存，并将地址作为隐式的第一个参数传递
            s.garUsed++;
        }
        return ret;
    }

    private static Placement one(Slot s) {
        return new Placement.InSlots(List.of(s));
    }

    private static Placement slots(Slot... ss) {
        return new Placement.InSlots(List.of(ss));
    }

    private static Placement allocStack(State s, long size) {
        long off = s.stackOffset;
        int n = (int) (size / GRLEN);
        List<Slot> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(new Slot.Stack(off + (long) i * GRLEN));
        }
        s.stackOffset += size;
        return new Placement.InSlots(out);
    }

    private static final class State {
        int garUsed = 0;
        int farUsed = 0;
        long stackOffset = 0;
    }
}

