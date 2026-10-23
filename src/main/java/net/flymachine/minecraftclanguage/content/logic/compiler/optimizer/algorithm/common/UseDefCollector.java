package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.common;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacAddressDescriptor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacValue;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacVariable;

import java.util.BitSet;

/**
 * 收集一条指令使用的变量和被定义的变量，结果写入调用方提供的 BitSet
 * <p>
 * 变量下标在第一次遇到时惰性分配，因此调用方不需要预先遍历函数
 */
public final class UseDefCollector implements TacVisitor<Void> {
    private final VariableInterner interner;
    private BitSet useSet;
    private BitSet defSet;

    public UseDefCollector(VariableInterner interner) {
        this.interner = interner;
    }

    public void collect(TacInstruction inst, BitSet useSet, BitSet defSet) {
        this.useSet = useSet;
        this.defSet = defSet;
        inst.accept(this);
    }

    private void use(TacValue val) {
        if (val instanceof TacVariable var) {
            useSet.set(interner.intern(var.name));
        }
    }

    private void use(TacAddressDescriptor addr) {
        use(addr.base());
        if (addr.index() != null) {
            use(addr.index());
        }
    }

    private void def(TacValue val) {
        if (val instanceof TacVariable var) {
            defSet.set(interner.intern(var.name));
        }
    }

    @Override
    public Void visit(TacReturn inst) {
        use(inst.value);
        return null;
    }

    @Override
    public Void visit(TacUnaryOperation inst) {
        use(inst.src);
        def(inst.dst);
        return null;
    }

    @Override
    public Void visit(TacBinaryOperation inst) {
        use(inst.lhs);
        use(inst.rhs);
        def(inst.dst);
        return null;
    }

    @Override
    public Void visit(TacCopy inst) {
        use(inst.src);
        def(inst.dst);
        return null;
    }

    @Override
    public Void visit(TacLabel inst) {
        throw new IllegalArgumentException("Label instruction should not be in basic block");
    }

    @Override
    public Void visit(TacJump inst) {
        return null;
    }

    @Override
    public Void visit(TacJumpIfZero inst) {
        use(inst.cond);
        return null;
    }

    @Override
    public Void visit(TacJumpIfNotZero inst) {
        use(inst.cond);
        return null;
    }

    @Override
    public Void visit(TacJumpIfComparison inst) {
        use(inst.lhs);
        use(inst.rhs);
        return null;
    }

    @Override
    public Void visit(TacDirectCall inst) {
        // 在具体分析中考虑对静态变量和被取地址变量
        for (TacValue arg : inst.args) {
            use(arg);
        }
        if (inst.dst != null) {
            def(inst.dst);
        }
        return null;
    }

    @Override
    public Void visit(TacSignExtend inst) {
        use(inst.src);
        def(inst.dst);
        return null;
    }

    @Override
    public Void visit(TacTruncate inst) {
        use(inst.src);
        def(inst.dst);
        return null;
    }

    @Override
    public Void visit(TacZeroExtend inst) {
        use(inst.src);
        def(inst.dst);
        return null;
    }

    @Override
    public Void visit(TacDoubleToInt inst) {
        use(inst.src);
        def(inst.dst);
        return null;
    }

    @Override
    public Void visit(TacDoubleToUnsignedInt inst) {
        use(inst.src);
        def(inst.dst);
        return null;
    }

    @Override
    public Void visit(TacIntToDouble inst) {
        use(inst.src);
        def(inst.dst);
        return null;
    }

    @Override
    public Void visit(TacUnsignedIntToDouble inst) {
        use(inst.src);
        def(inst.dst);
        return null;
    }

    @Override
    public Void visit(TacGetAddress inst) {
        def(inst.dst);
        return null;
    }

    @Override
    public Void visit(TacLoad inst) {
        // 在具体分析中考虑对静态变量和被取地址变量
        use(inst.srcAddr);
        def(inst.dst);
        return null;
    }

    @Override
    public Void visit(TacStore inst) {
        // 在具体分析中考虑对静态变量和被取地址变量
        use(inst.src);
        use(inst.dstAddr);
        return null;
    }

    @Override
    public Void visit(TacAddPointer inst) {
        use(inst.ptr);
        use(inst.index);
        def(inst.dst);
        return null;
    }

    @Override
    public Void visit(TacIndirectCall inst) {
        // 在具体分析中考虑对静态变量和被取地址变量
        use(inst.funcPtr);
        for (TacValue arg : inst.args) {
            use(arg);
        }
        if (inst.dst != null) {
            def(inst.dst);
        }
        return null;
    }

    @Override
    public Void visit(TacCopyToOffset inst) {
        // 在具体分析中考虑 dst
        use(inst.src);
        return null;
    }

    @Override
    public Void visit(TacCopyByteArrayToOffset inst) {
        // 在具体分析中考虑 dst
        return null;
    }

    @Override
    public Void visit(TacCopyFromOffset inst) {
        // 在具体分析中考虑 src
        def(inst.dst);
        return null;
    }
}
