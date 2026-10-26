package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.registerAllocator.linear;

import com.google.common.collect.MinMaxPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.FloatingPointRegister;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.LA64Register;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.BackendSymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Pseudo;

import java.util.*;

import static net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.FloatingPointRegister.*;
import static net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister.*;

public final class SimpleLinearAllocator {

    private final static GeneralPurposeRegister[] GPR_ALLOWS = {S0, S1, S2, S3, S4, S5, S6, S7, S8};
    private final static FloatingPointRegister[] FPR_ALLOWS = {FS0, FS1, FS2, FS3, FS4, FS5, FS6, FS7};

    private final BackendSymbolTable symbolTable;

    public SimpleLinearAllocator(BackendSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public void allocate(HighLevelFunction func) {
        Map<Pseudo, Integer> integerAlloc = allocate(func, true, GPR_ALLOWS.length);
        Map<Pseudo, Integer> floatingAlloc = allocate(func, false, FPR_ALLOWS.length);

        // 进行操作数替换
        PseudoReplacer.replace(func, integerAlloc, GPR_ALLOWS);
        PseudoReplacer.replace(func, floatingAlloc, FPR_ALLOWS);

        // 将分配使用的寄存器写入函数信息
        func.savedRegisters.addAll(
            integerAlloc.values().stream().distinct().sorted().map(i -> GPR_ALLOWS[i]).toList());
        func.savedRegisters.addAll(
            floatingAlloc.values().stream().distinct().sorted().map(i -> FPR_ALLOWS[i]).toList());
    }

    @SuppressWarnings({"UnstableApiUsage"})
    private Map<Pseudo, Integer> allocate(HighLevelFunction func, boolean allocateGpr, int regCount) {
        // 已按 start 升序
        List<LiveRange> liveRanges = LiveRangeCollector.collect(func, symbolTable, allocateGpr);

        IntHeapPriorityQueue freeRegisters = new IntHeapPriorityQueue();
        for (int i = 0; i < regCount; ++i) {
            freeRegisters.enqueue(i);
        }
        MinMaxPriorityQueue<LiveRange> active =
            MinMaxPriorityQueue.<LiveRange>orderedBy(
                Comparator.comparingInt(lr -> lr.end)).create();
        Map<Pseudo, Integer> allocation = new HashMap<>();

        for (LiveRange lr : liveRanges) {
            // 回收过期区间及其分配的寄存器
            int now = lr.start;
            while (!active.isEmpty() && Objects.requireNonNull(active.peek()).end < now) {
                LiveRange expired = active.poll();
                freeRegisters.enqueue((int) allocation.get(Objects.requireNonNull(expired).pseudo));
            }

            // 尝试分配当前区间
            if (!freeRegisters.isEmpty()) {
                // 有空闲寄存器
                int regIndex = freeRegisters.dequeueInt();
                allocation.put(lr.pseudo, regIndex);
                active.add(lr);
            } else {
                // 没有空闲寄存器，溢出
                // 溢出 end 最大的
                LiveRange spill = active.peekLast();
                if (Objects.requireNonNull(spill).end > lr.end) {
                    // 溢出 spill，分配其寄存器给当前区间
                    active.pollLast();
                    allocation.put(lr.pseudo, allocation.get(spill.pseudo));
                    allocation.remove(spill.pseudo);
                    active.add(lr);
                }
                // 否则溢出自己
            }
        }

        return allocation;
    }

    private static class LiveRange {
        int start;
        int end;
        final Pseudo pseudo;

        LiveRange(int start, int end, Pseudo pseudo) {
            this.start = start;
            this.end = end;
            this.pseudo = pseudo;
        }
    }

    private static class LiveRangeCollector implements HighLevelVisitor<Void> {
        Map<Pseudo, LiveRange> pseudos;
        Set<Pseudo> escaped;
        boolean allocateGpr;
        BackendSymbolTable symbolTable;

        private LiveRangeCollector(BackendSymbolTable symbolTable, boolean allocateGpr) {
            this.pseudos = new HashMap<>();
            this.escaped = new HashSet<>();
            this.allocateGpr = allocateGpr;
            this.symbolTable = symbolTable;
        }

        public static List<LiveRange> collect(
            HighLevelFunction func, BackendSymbolTable symbolTable, boolean allocateGpr) {

            LiveRangeCollector collector = new LiveRangeCollector(symbolTable, allocateGpr);
            collector.collectInner(func);
            return collector.pseudos
                .values().stream()
                .sorted(Comparator.comparingInt(lr -> lr.start)).toList();
        }

        int position = 0;

        private void collectInner(HighLevelFunction func) {
            for (HighLevelInstruction inst : func.insts) {
                inst.accept(this);
            }
        }

        private void visitOperand(HighLevelOperand operand, AsmType asmType) {
            if (operand instanceof Pseudo pseudo && asmType instanceof AsmType.PrimitiveType primitive) {
                if (allocateGpr == (primitive.p() != AsmType.Primitive.DOUBLE)) {
                    update(pseudo);
                }
            }
        }

        private void visitIntegerOperand(HighLevelOperand operand) {
            if (allocateGpr && operand instanceof Pseudo pseudo) {
                update(pseudo);
            }
        }

        private void visitFloatingOperand(HighLevelOperand operand) {
            if (!allocateGpr && operand instanceof Pseudo pseudo) {
                update(pseudo);
            }
        }

        private void update(Pseudo pseudo) {
            if (escaped.contains(pseudo)) {
                return;
            }
            if (!pseudos.containsKey(pseudo)) {
                BackendSymbolTable.Entry entry = symbolTable.get(pseudo.name());
                if (entry == null) {
                    throw new IllegalStateException("Undefined symbol: " + pseudo.name());
                }
                if (entry instanceof BackendSymbolTable.FuncEntry) {
                    escaped.add(pseudo);
                    return;
                }
                BackendSymbolTable.ObjectEntry objectEntry = (BackendSymbolTable.ObjectEntry) entry;
                if (objectEntry.isStatic()) {
                    escaped.add(pseudo);
                    return;
                }
                pseudos.put(pseudo, new LiveRange(position, position, pseudo));
            } else {
                LiveRange range = pseudos.get(pseudo);
                // 请保证 line 只增不减
                range.end = position;
            }
        }

        private void escape(HighLevelOperand operand) {
            if (operand instanceof Pseudo pseudo) {
                escaped.add(pseudo);
                pseudos.remove(pseudo);
            }
        }

        @Override
        public Void visit(Move inst) {
            visitOperand(inst.src, inst.asmType);
            ++position;
            visitOperand(inst.dst, inst.asmType);
            ++position;
            return null;
        }

        @Override
        public Void visit(Ret inst) {
            return null;
        }

        @Override
        public Void visit(Binary inst) {
            visitOperand(inst.lhs, inst.asmType);
            visitOperand(inst.rhs, inst.asmType);
            ++position;
            visitOperand(inst.dst, inst.asmType);
            ++position;
            return null;
        }

        @Override
        public Void visit(AddSi12 inst) {
            visitIntegerOperand(inst.src);
            ++position;
            visitIntegerOperand(inst.dst);
            ++position;
            return null;
        }

        @Override
        public Void visit(Label inst) {
            return null;
        }

        @Override
        public Void visit(Branch inst) {
            return null;
        }

        @Override
        public Void visit(BranchIfZero inst) {
            visitIntegerOperand(inst.cond);
            ++position;
            return null;
        }

        @Override
        public Void visit(BranchIfNotZero inst) {
            visitIntegerOperand(inst.cond);
            ++position;
            return null;
        }

        @Override
        public Void visit(BranchIfComparison inst) {
            visitIntegerOperand(inst.lhs);
            visitIntegerOperand(inst.rhs);
            ++position;
            return null;
        }

        @Override
        public Void visit(Call inst) {
            return null;
        }

        @Override
        public Void visit(Compare inst) {
            visitIntegerOperand(inst.lhs);
            visitIntegerOperand(inst.rhs);
            ++position;
            visitIntegerOperand(inst.dst);
            ++position;
            return null;
        }

        @Override
        public Void visit(DivOrMod inst) {
            visitOperand(inst.lhs, inst.asmType);
            visitOperand(inst.rhs, inst.asmType);
            ++position;
            visitOperand(inst.dst, inst.asmType);
            ++position;
            return null;
        }

        @Override
        public Void visit(BitwiseShift inst) {
            visitIntegerOperand(inst.lhs);
            visitIntegerOperand(inst.rhs);
            ++position;
            visitIntegerOperand(inst.dst);
            ++position;
            return null;
        }

        @Override
        public Void visit(Bitwise inst) {
            visitIntegerOperand(inst.lhs);
            visitIntegerOperand(inst.rhs);
            ++position;
            visitIntegerOperand(inst.dst);
            ++position;
            return null;
        }

        @Override
        public Void visit(ZeroExtend inst) {
            visitIntegerOperand(inst.src);
            ++position;
            visitIntegerOperand(inst.dst);
            ++position;
            return null;
        }

        @Override
        public Void visit(SignExtend inst) {
            visitIntegerOperand(inst.src);
            ++position;
            visitIntegerOperand(inst.dst);
            ++position;
            return null;
        }

        @Override
        public Void visit(DoubleFromInt inst) {
            visitIntegerOperand(inst.src);
            ++position;
            visitFloatingOperand(inst.dst);
            ++position;
            return null;
        }

        @Override
        public Void visit(DoubleToIntRoundZero inst) {
            visitFloatingOperand(inst.src);
            ++position;
            visitIntegerOperand(inst.dst);
            ++position;
            return null;
        }

        @Override
        public Void visit(CompareDouble inst) {
            visitFloatingOperand(inst.lhs);
            visitFloatingOperand(inst.rhs);
            ++position;
            return null;
        }

        @Override
        public Void visit(GetCC inst) {
            visitIntegerOperand(inst.dst);
            ++position;
            return null;
        }

        @Override
        public Void visit(BranchIfCCZero inst) {
            return null;
        }

        @Override
        public Void visit(BranchIfCCNotZero inst) {
            return null;
        }

        @Override
        public Void visit(DoubleNegate inst) {
            visitFloatingOperand(inst.src);
            ++position;
            visitFloatingOperand(inst.dst);
            ++position;
            return null;
        }

        @Override
        public Void visit(LoadAddress inst) {
            escape(inst.obj);
            visitIntegerOperand(inst.dst);
            ++position;
            return null;
        }

        @Override
        public Void visit(Load inst) {
            visitIntegerOperand(inst.ptr);
            visitIntegerOperand(inst.offset);
            ++position;
            visitOperand(inst.dst, inst.dstAsmType);
            ++position;
            return null;
        }

        @Override
        public Void visit(Store inst) {
            visitIntegerOperand(inst.ptr);
            visitIntegerOperand(inst.offset);
            visitOperand(inst.src, inst.srcAsmType);
            ++position;
            return null;
        }

        @Override
        public Void visit(AddLeftShift inst) {
            visitIntegerOperand(inst.base);
            visitIntegerOperand(inst.index);
            ++position;
            visitIntegerOperand(inst.dst);
            ++position;
            return null;
        }

        @Override
        public Void visit(CallIndirect inst) {
            visitIntegerOperand(inst.funcPtr);
            ++position;
            return null;
        }

        @Override
        public Void visit(CopyByteArray inst) {
            return null;
        }
    }

    private record PseudoReplacer(Map<Pseudo, Integer> allocation, LA64Register[] allows)
        implements HighLevelVisitor<Void> {

        public static void replace(HighLevelFunction func, Map<Pseudo, Integer> allocation, LA64Register[] allows) {
            PseudoReplacer replacer = new PseudoReplacer(allocation, allows);
            replacer.replaceInner(func);
        }

        private void replaceInner(HighLevelFunction func) {
            for (HighLevelInstruction inst : func.insts) {
                inst.accept(this);
            }
        }

        private HighLevelOperand replaceOperand(HighLevelOperand operand) {
            if (operand instanceof Pseudo pseudo && allocation.containsKey(pseudo)) {
                int regIndex = allocation.get(pseudo);
                operand = (HighLevelOperand) allows[regIndex];
            }
            return operand;
        }

        @Override
        public Void visit(Move inst) {
            inst.src = replaceOperand(inst.src);
            inst.dst = replaceOperand(inst.dst);
            return null;
        }

        @Override
        public Void visit(Ret inst) {
            return null;
        }

        @Override
        public Void visit(Binary inst) {
            inst.lhs = replaceOperand(inst.lhs);
            inst.rhs = replaceOperand(inst.rhs);
            inst.dst = replaceOperand(inst.dst);
            return null;
        }

        @Override
        public Void visit(AddSi12 inst) {
            inst.src = replaceOperand(inst.src);
            inst.dst = replaceOperand(inst.dst);
            return null;
        }

        @Override
        public Void visit(Label inst) {
            return null;
        }

        @Override
        public Void visit(Branch inst) {
            return null;
        }

        @Override
        public Void visit(BranchIfZero inst) {
            inst.cond = replaceOperand(inst.cond);
            return null;
        }

        @Override
        public Void visit(BranchIfNotZero inst) {
            inst.cond = replaceOperand(inst.cond);
            return null;
        }

        @Override
        public Void visit(BranchIfComparison inst) {
            inst.lhs = replaceOperand(inst.lhs);
            inst.rhs = replaceOperand(inst.rhs);
            return null;
        }

        @Override
        public Void visit(Call inst) {
            return null;
        }

        @Override
        public Void visit(Compare inst) {
            inst.lhs = replaceOperand(inst.lhs);
            inst.rhs = replaceOperand(inst.rhs);
            inst.dst = replaceOperand(inst.dst);
            return null;
        }

        @Override
        public Void visit(DivOrMod inst) {
            inst.lhs = replaceOperand(inst.lhs);
            inst.rhs = replaceOperand(inst.rhs);
            inst.dst = replaceOperand(inst.dst);
            return null;
        }

        @Override
        public Void visit(BitwiseShift inst) {
            inst.lhs = replaceOperand(inst.lhs);
            inst.rhs = replaceOperand(inst.rhs);
            inst.dst = replaceOperand(inst.dst);
            return null;
        }

        @Override
        public Void visit(Bitwise inst) {
            inst.lhs = replaceOperand(inst.lhs);
            inst.rhs = replaceOperand(inst.rhs);
            inst.dst = replaceOperand(inst.dst);
            return null;
        }

        @Override
        public Void visit(ZeroExtend inst) {
            inst.src = replaceOperand(inst.src);
            inst.dst = replaceOperand(inst.dst);
            return null;
        }

        @Override
        public Void visit(SignExtend inst) {
            inst.src = replaceOperand(inst.src);
            inst.dst = replaceOperand(inst.dst);
            return null;
        }

        @Override
        public Void visit(DoubleFromInt inst) {
            inst.src = replaceOperand(inst.src);
            inst.dst = replaceOperand(inst.dst);
            return null;
        }

        @Override
        public Void visit(DoubleToIntRoundZero inst) {
            inst.src = replaceOperand(inst.src);
            inst.dst = replaceOperand(inst.dst);
            return null;
        }

        @Override
        public Void visit(CompareDouble inst) {
            inst.lhs = replaceOperand(inst.lhs);
            inst.rhs = replaceOperand(inst.rhs);
            return null;
        }

        @Override
        public Void visit(GetCC inst) {
            inst.dst = replaceOperand(inst.dst);
            return null;
        }

        @Override
        public Void visit(BranchIfCCZero inst) {
            return null;
        }

        @Override
        public Void visit(BranchIfCCNotZero inst) {
            return null;
        }

        @Override
        public Void visit(DoubleNegate inst) {
            inst.src = replaceOperand(inst.src);
            inst.dst = replaceOperand(inst.dst);
            return null;
        }

        @Override
        public Void visit(LoadAddress inst) {
            inst.dst = replaceOperand(inst.dst);
            return null;
        }

        @Override
        public Void visit(Load inst) {
            inst.ptr = replaceOperand(inst.ptr);
            inst.offset = replaceOperand(inst.offset);
            inst.dst = replaceOperand(inst.dst);
            return null;
        }

        @Override
        public Void visit(Store inst) {
            inst.ptr = replaceOperand(inst.ptr);
            inst.offset = replaceOperand(inst.offset);
            inst.src = replaceOperand(inst.src);
            return null;
        }

        @Override
        public Void visit(AddLeftShift inst) {
            inst.base = replaceOperand(inst.base);
            inst.index = replaceOperand(inst.index);
            inst.dst = replaceOperand(inst.dst);
            return null;
        }

        @Override
        public Void visit(CallIndirect inst) {
            inst.funcPtr = replaceOperand(inst.funcPtr);
            return null;
        }

        @Override
        public Void visit(CopyByteArray inst) {
            return null;
        }
    }
}
