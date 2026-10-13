package net.flymachine.minecraftclanguage.content.logic.architecture.la64.abi.lp64d.callConv;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.record.Field;
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
     * 根据调用约定，分配函数返回值的寄存器/栈位置
     *
     * @param returnType 返回类型
     */
    public static Placement allocateReturn(Type returnType) {
        return allocateReturn(returnType, new State());
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

        // (void) 函数没有参数，直接返回
        if (paramTypes.isEmpty() || paramTypes.size() == 1 && paramTypes.get(0).isVoid()) {
            return new Signature(List.of(), ret, s.stackOffset);
        }

        List<Placement> params = new ArrayList<>(paramTypes.size());
        List<Integer> byRefIndices = new ArrayList<>();
        for (int i = 0; i < paramTypes.size(); i++) {
            Placement p = allocateParam(paramTypes.get(i), s);
            if (p instanceof Placement.ByReference) {
                byRefIndices.add(i);
            }
            params.add(p);
        }

        // 从 stackSize 开始分配副本
        for (int idx : byRefIndices) {
            Placement.ByReference br = (Placement.ByReference) params.get(idx);
            Type t = paramTypes.get(idx);
            Slot.Stack copySlot = allocStackAligned(s, t.sizeof(), t.alignof());
            params.set(idx, new Placement.ByReference(br.pointerSlot(), copySlot));
        }

        return new Signature(List.copyOf(params), ret, s.stackOffset);
    }

    private record FlatInfo(int floatCount, int intCount, boolean isFirstFloat) {
        boolean onlyFloat() {
            return intCount == 0 && floatCount > 0;
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
                FlatInfo fi = flatten(fields.get(idx).type);
                if (idx == 0) {
                    firstFloat = fi.isFirstFloat();
                }
                f += fi.floatCount();
                i += fi.intCount();
            }
            return new FlatInfo(f, i, firstFloat);
        }
        if (type instanceof UnionType ut) {
            // 联合体展开时一律按整数处理
            long size = ut.sizeof();
            int intCount = (int) ((size + GRLEN - 1) / GRLEN);
            return new FlatInfo(0, intCount, false);
        }
        throw new IllegalArgumentException("Unsupported type for flatten: " + type);
    }

    private static Placement allocateParam(Type t, State s) {
        if (t.isScalar()) {
            return allocateScalar(t.toAsmType(), s);
        }
        if (t instanceof StructType st) {
            return allocateStruct(st, s);
        }
        if (t instanceof UnionType ut) {
            return allocateUnion(ut, s);
        }
        throw new IllegalArgumentException("Unsupported parameter type: " + t);
    }

    private static Placement allocateScalar(AsmType t, State s) {
        return t.equals(AsmType.DOUBLE) ? allocateSingleFar(s) : allocateSingleGar(t, s);
    }

    private static Placement allocateStruct(StructType st, State s) {
        int size = (int) st.sizeof();
        if (size == 0) {
            throw new IllegalArgumentException("Empty struct is not supported");
        }
        if (size > 2 * GRLEN) {
            return allocateByReference(s);
        }

        FlatInfo fi = flatten(st);

        if (size <= GRLEN) {
            return fi.onlyFloat() ? allocateSingleFar(s) : allocateSingleGar(slotAsmType(size), s);
        }

        // GRLEN < size <= 2 * GRLEN
        if (fi.onlyFloat()) {
            // 只可能是两个 double
            return allocateFarPair(s);
        }
        if (fi.floatCount() == 1 && fi.intCount() == 1) {
            return allocateMixedFarGar(fi, s);
        }
        return allocateGarPair(s, GRLEN, size - GRLEN);
    }

    private static Placement allocateUnion(UnionType ut, State s) {
        int size = (int) ut.sizeof();
        if (size == 0) {
            throw new IllegalArgumentException("Empty union is not supported");
        }
        if (size > 2 * GRLEN) {
            return allocateByReference(s);
        }
        if (size <= GRLEN) {
            return allocateSingleGar(slotAsmType(size), s);
        }
        // GRLEN < size <= 2 * GRLEN
        return allocateGarPair(s, GRLEN, size - GRLEN);
    }

    private static Placement allocateSingleGar(AsmType t, State s) {
        if (s.garUsed < MAX_GAR) {
            return one(new Slot.GAR(s.garUsed++, t));
        }
        return one(allocStack(s, t));
    }

    private static Placement allocateSingleFar(State s) {
        if (s.farUsed < MAX_FAR) {
            return one(new Slot.FAR(s.farUsed++, AsmType.DOUBLE));
        }
        return allocateSingleGar(AsmType.DWORD, s);
    }

    private static Placement allocateGarPair(State s, int w0, int w1) {
        AsmType t0 = slotAsmType(w0);
        AsmType t1 = slotAsmType(w1);
        int remaining = MAX_GAR - s.garUsed;

        if (remaining >= 2) {
            return slots(new Slot.GAR(s.garUsed++, t0), new Slot.GAR(s.garUsed++, t1));
        }
        if (remaining == 1) {
            return slots(new Slot.GAR(s.garUsed++, t0), allocStack(s, t1));
        }
        return slots(allocStack(s, t0), allocStack(s, t1));
    }

    private static Placement allocateFarPair(State s) {
        if (s.farUsed + 2 <= MAX_FAR) {
            return slots(new Slot.FAR(s.farUsed++, AsmType.DOUBLE),
                         new Slot.FAR(s.farUsed++, AsmType.DOUBLE));
        }
        return allocateGarPair(s, GRLEN, GRLEN);
    }

    private static Placement allocateMixedFarGar(FlatInfo fi, State s) {
        if (s.farUsed < MAX_FAR && s.garUsed < MAX_GAR) {
            if (fi.isFirstFloat) {
                return slots(new Slot.FAR(s.farUsed++, AsmType.DOUBLE),
                             new Slot.GAR(s.garUsed++, AsmType.DWORD));
            }
            return slots(new Slot.GAR(s.garUsed++, AsmType.DWORD),
                         new Slot.FAR(s.farUsed++, AsmType.DOUBLE));
        }
        return allocateGarPair(s, GRLEN, GRLEN);
    }

    private static Placement allocateByReference(State s) {
        Slot pointerSlot;
        if (s.garUsed < MAX_GAR) {
            pointerSlot = new Slot.GAR(s.garUsed++, AsmType.DWORD);
        } else {
            pointerSlot = allocStack(s, AsmType.DWORD);
        }
        return new Placement.ByReference(pointerSlot, null);
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

    private static Slot allocStack(State s, AsmType t) {
        long off = s.stackOffset;
        long roundedSize = (t.size() + GRLEN - 1) & -GRLEN;
        s.stackOffset = off + roundedSize;
        return new Slot.Stack(off, t);
    }

    private static Slot.Stack allocStackAligned(State s, long size, long alignment) {
        long off = s.stackOffset;
        off = (off + alignment - 1) / alignment * alignment;
        s.stackOffset = off + size;
        return new Slot.Stack(off, new AsmType.ByteArray(size, alignment));
    }

    private static AsmType slotAsmType(int bytes) {
        return switch (bytes) {
            case 1 -> AsmType.BYTE;
            case 2 -> AsmType.HALF;
            case 4 -> AsmType.WORD;
            case 8 -> AsmType.DWORD;
            default -> new AsmType.ByteArray(bytes, GRLEN);
        };
    }

    private static final class State {
        int garUsed = 0;
        int farUsed = 0;
        long stackOffset = 0;
    }
}

