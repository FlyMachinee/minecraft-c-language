package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.simple;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacAddressDescriptor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacConstant;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacValue;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacVariable;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.common.FunctionContext;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.reachingCopies.CopyInterner;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.reachingCopies.ReachingCopiesAnalysis;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.reachingCopies.ReachingCopiesResult;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;

import java.util.BitSet;
import java.util.List;

public final class CopyPropagation implements SimpleTraversalPass, TacVisitor<Boolean> {

    private boolean changed;
    private final BitSet state = new BitSet();
    private final BitSet scratchUse = new BitSet();
    private final BitSet scratchDef = new BitSet();
    private CopyInterner interner;
    private FunctionContext ctx;

    @Override
    public boolean runOn(FunctionContext ctx) {
        this.ctx = ctx;
        ReachingCopiesResult rc = ReachingCopiesAnalysis.analyze(ctx);
        interner = rc.interner();
        if (interner.size() == 0) {
            // 没有任何复制指令
            return false;
        }

        BitSet nextState = new BitSet();

        for (BasicBlock block : ctx.cfg().blocks()) {
            // 块入口处的到达复制集合
            state.clear();
            state.or(rc.in(block));

            List<TacInstruction> insts = block.instructions();
            for (int i = 0; i < insts.size(); ) {
                TacInstruction inst = insts.get(i);

                // 先推导下一条指令的状态，需要使用原始指令推导
                nextState.clear();
                nextState.or(state);
                ReachingCopiesAnalysis.transferOne(ctx, rc.interner(), inst, nextState, scratchUse, scratchDef);

                // 随后重写指令
                boolean delete = inst.accept(this);
                if (delete) {
                    insts.remove(i);
                    changed = true;
                    continue;
                }

                state.clear();
                state.or(nextState);
                i++;
            }
        }

        return changed;
    }

    private TacValue rewriteOperand(TacValue operand) {
        if (operand instanceof TacVariable var) {
            BitSet reachingCopies = (BitSet) state.clone();
            // 到达此处的，以 var 为 dst 的复制指令集合
            reachingCopies.and(interner.defCopies(var.name));
            if (!reachingCopies.isEmpty()) {
                if (reachingCopies.cardinality() > 1) {
                    throw new IllegalStateException(
                        "multiple reaching copies for variable " + var.name + ": " + reachingCopies);
                }
                int copyIdx = reachingCopies.nextSetBit(0);
                CopyInterner.CopyKey copy = interner.copyOf(copyIdx);
                // 递归替换，加快迭代速度
                changed = true;
                Type targetType = ctx.symbolTable().get(var.name).type;
                operand = rewriteOperand(copy.src());
                if (operand instanceof TacConstant constant) {
                    // 避免常量折叠出现类型错误
                    constant.value = constant.value.castTo(targetType);
                }
            }
        }
        return operand;
    }

    private TacAddressDescriptor rewriteDescriptor(TacAddressDescriptor addr) {
        TacValue base = rewriteOperand(addr.base());
        TacValue index = addr.index() != null ? rewriteOperand(addr.index()) : null;
        if (base != addr.base() || index != addr.index()) {
            return new TacAddressDescriptor(base, index, addr.scale(), addr.offset()).fold();
        }
        return addr;
    }


    private String rewriteAggregate(String aggregate) {
        BitSet reachingCopies = (BitSet) state.clone();
        // 到达此处的，以 var 为 dst 的复制指令集合
        reachingCopies.and(interner.defCopies(aggregate));
        if (!reachingCopies.isEmpty()) {
            if (reachingCopies.cardinality() > 1) {
                throw new IllegalStateException(
                    "multiple reaching copies for variable " + aggregate + ": " + reachingCopies);
            }
            int copyIdx = reachingCopies.nextSetBit(0);
            CopyInterner.CopyKey copy = interner.copyOf(copyIdx);
            // 递归替换，加快迭代速度
            changed = true;
            aggregate = rewriteAggregate(((TacVariable) copy.src()).name);
        }
        return aggregate;
    }

    @Override
    public Boolean visit(TacReturn inst) {
        inst.value = rewriteOperand(inst.value);
        return false;
    }

    @Override
    public Boolean visit(TacUnaryOperation inst) {
        inst.src = rewriteOperand(inst.src);
        return false;
    }

    @Override
    public Boolean visit(TacBinaryOperation inst) {
        inst.lhs = rewriteOperand(inst.lhs);
        inst.rhs = rewriteOperand(inst.rhs);
        return false;
    }

    @Override
    public Boolean visit(TacCopy inst) {
        if (ReachingCopiesAnalysis.isSelfCopy(inst)) {
            return true;
        }
        // 特殊处理
        TacVariable dst = (TacVariable) inst.dst;
        BitSet reachingCopies = (BitSet) state.clone();
        reachingCopies.and(interner.defCopies(dst.name));
        if (!reachingCopies.isEmpty()) {
            if (reachingCopies.cardinality() > 1) {
                throw new IllegalStateException(
                    "multiple reaching copies for variable " + dst.name + ": " + reachingCopies);
            }
            int copyIdx = reachingCopies.nextSetBit(0);
            CopyInterner.CopyKey copy = interner.copyOf(copyIdx);
            if (copy.src().equals(inst.src)) {
                // 到达复制与当前复制相同，删除当前复制
                return true;
            }
        }
        inst.src = rewriteOperand(inst.src);
        return false;
    }

    @Override
    public Boolean visit(TacLabel inst) {
        throw new UnsupportedOperationException("TacLabel should not be in basic block");
    }

    @Override
    public Boolean visit(TacJump inst) {
        return false;
    }

    @Override
    public Boolean visit(TacJumpIfZero inst) {
        inst.cond = rewriteOperand(inst.cond);
        return false;
    }

    @Override
    public Boolean visit(TacJumpIfNotZero inst) {
        inst.cond = rewriteOperand(inst.cond);
        return false;
    }

    @Override
    public Boolean visit(TacJumpIfComparison inst) {
        inst.lhs = rewriteOperand(inst.lhs);
        inst.rhs = rewriteOperand(inst.rhs);
        return false;
    }

    @Override
    public Boolean visit(TacDirectCall inst) {
        for (int i = 0; i < inst.args.size(); i++) {
            TacValue arg = inst.args.get(i);
            TacValue newArg = rewriteOperand(arg);
            if (newArg != arg) {
                inst.args.set(i, newArg);
            }
        }
        return false;
    }

    @Override
    public Boolean visit(TacSignExtend inst) {
        inst.src = rewriteOperand(inst.src);
        return false;
    }

    @Override
    public Boolean visit(TacTruncate inst) {
        inst.src = rewriteOperand(inst.src);
        return false;
    }

    @Override
    public Boolean visit(TacZeroExtend inst) {
        inst.src = rewriteOperand(inst.src);
        return false;
    }

    @Override
    public Boolean visit(TacDoubleToInt inst) {
        inst.src = rewriteOperand(inst.src);
        return false;
    }

    @Override
    public Boolean visit(TacDoubleToUnsignedInt inst) {
        inst.src = rewriteOperand(inst.src);
        return false;
    }

    @Override
    public Boolean visit(TacIntToDouble inst) {
        inst.src = rewriteOperand(inst.src);
        return false;
    }

    @Override
    public Boolean visit(TacUnsignedIntToDouble inst) {
        inst.src = rewriteOperand(inst.src);
        return false;
    }

    @Override
    public Boolean visit(TacGetAddress inst) {
        // 例外，不处理
        return false;
    }

    @Override
    public Boolean visit(TacLoad inst) {
        inst.srcAddr = rewriteDescriptor(inst.srcAddr);
        return false;
    }

    @Override
    public Boolean visit(TacStore inst) {
        inst.dstAddr = rewriteDescriptor(inst.dstAddr);
        inst.src = rewriteOperand(inst.src);
        return false;
    }

    @Override
    public Boolean visit(TacAddPointer inst) {
        inst.ptr = rewriteOperand(inst.ptr);
        inst.index = rewriteOperand(inst.index);
        return false;
    }

    @Override
    public Boolean visit(TacCopyToOffset inst) {
        inst.src = rewriteOperand(inst.src);
        return false;
    }

    @Override
    public Boolean visit(TacIndirectCall inst) {
        for (int i = 0; i < inst.args.size(); i++) {
            TacValue arg = inst.args.get(i);
            TacValue newArg = rewriteOperand(arg);
            if (newArg != arg) {
                inst.args.set(i, newArg);
            }
        }
        inst.funcPtr = rewriteOperand(inst.funcPtr);
        return false;
    }

    @Override
    public Boolean visit(TacCopyByteArrayToOffset inst) {
        return false;
    }

    @Override
    public Boolean visit(TacCopyFromOffset inst) {
        inst.src = rewriteAggregate(inst.src);
        return false;
    }
}
