package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.simple;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantLong;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantPointer;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.PointerType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacAddressDescriptor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacConstant;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacValue;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacVariable;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.ControlFlowGraph;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.util.TacUtil;

public final class ConstantFolding implements SimpleTraversalPass, TacVisitor<TacInstruction> {

    private boolean folded = false;
    private SymbolTable symbolTable;

    @Override
    public boolean runOn(ControlFlowGraph cfg, SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.folded = false;

        for (BasicBlock block : cfg.blocks()) {
            int i = 0;
            while (i < block.instructions().size()) {
                TacInstruction inst = block.instructions().get(i);
                TacInstruction newInst = inst.accept(this);
                String target = TacUtil.getLabel(inst);
                if (newInst == null) {
                    block.instructions().remove(i);
                    folded = true;
                    if (target != null) {
                        if (i != block.instructions().size()) {
                            throw new IllegalStateException("Jump not at the end of block");
                        }
                        handleJumpFolding(cfg, block, target, false);
                    }
                    continue;
                }
                if (newInst != inst) {
                    block.instructions().set(i, newInst);
                    folded = true;
                    if (target != null) {
                        if (i != block.instructions().size() - 1) {
                            throw new IllegalStateException("Jump not at the end of block");
                        }
                        handleJumpFolding(cfg, block, target, true);
                    }
                }
                i++;
            }
        }
        return folded;
    }

    private Type getType(TacValue value) {
        if (value instanceof TacConstant constant) {
            return constant.value.getType();
        }
        if (value instanceof TacVariable variable) {
            return symbolTable.get(variable.name).type;
        }
        throw new IllegalArgumentException("Unknown TacValue type: " + value.getClass().getName());
    }

    private void handleJumpFolding(ControlFlowGraph cfg, BasicBlock block, String target, boolean jumpTaken) {
        // 条件跳转原本有两条出边：跳转目标与 fallthrough
        // 折叠后只剩一条：jumpTaken 为真时保留跳转边，否则保留 fallthrough 边
        int targetId = cfg.blockByLabel(target).id();

        int dropId;
        if (jumpTaken) {
            // 查找 fallthrough 边的目标块
            dropId = otherSuccessor(block, targetId);
            if (dropId == -1) {
                // 跳转目标与 fallthrough 是同一个块，重边，删一条即可
                dropId = targetId;
            }
        } else {
            dropId = targetId;
        }

        if (!block.successors().remove(Integer.valueOf(dropId))) {
            throw new IllegalStateException("Missing successor edge to drop: " + dropId);
        }
        if (!cfg.requireNode(dropId).predecessors().remove(Integer.valueOf(block.id()))) {
            throw new IllegalStateException("Missing predecessor edge to drop: " + dropId);
        }
    }

    private int otherSuccessor(BasicBlock block, int excluded) {
        for (int successorId : block.successors()) {
            if (successorId != excluded) {
                return successorId;
            }
        }
        return -1;
    }

    @Override
    public TacReturn visit(TacReturn inst) {
        return inst;
    }

    @Override
    public TacInstruction visit(TacUnaryOperation inst) {
        TacValue src = inst.src;
        if (src instanceof TacConstant srcConst) {
            Constant result = srcConst.value.apply(inst.op);
            return new TacCopy(new TacConstant(result), inst.dst);
        }
        return inst;
    }

    @Override
    public TacInstruction visit(TacBinaryOperation inst) {
        TacValue lhs = inst.lhs;
        TacValue rhs = inst.rhs;
        if (lhs instanceof TacConstant lhsConst && rhs instanceof TacConstant rhsConst) {
            Type lhsType = getType(lhsConst);
            Type rhsType = getType(rhsConst);
            Type dstType = getType(inst.dst);
            Constant res;

            if (dstType instanceof PointerType pt) {
                // 指针加/减整数运算
                // 这里我们不能直接用 apply 来计算，因为其作用时将会对整数进行缩放
                res = lhsConst.value.toLong().apply(inst.op, rhsConst.value.toLong()).toPointer(pt.referencedType());
            } else if (lhsType instanceof PointerType || rhsType instanceof PointerType) {
                // 这里用逻辑或，是因为复制传播后，可能会改变指针类型变量/常量为 long / unsigned long 的变量/常量
                // 指针减指针
                // 同理不能直接 apply
                res = lhsConst.value.toLong().apply(inst.op, rhsConst.value.toLong());
            } else {
                res = lhsConst.value.apply(inst.op, rhsConst.value);
            }

            return new TacCopy(new TacConstant(res), inst.dst);
        }
        return inst;
    }

    @Override
    public TacCopy visit(TacCopy inst) {
        // copy 可能用作相同宽度的整数/指针类型之间的转换
        if (inst.src instanceof TacConstant srcConst) {
            Type srcType = getType(srcConst);
            Type dstType = getType(inst.dst);
            if (!(dstType.isCompatible(srcType) ||
                  ((srcType.isChar() || srcType.isSignedChar()) && (dstType.isChar() || dstType.isSignedChar())))) {
                inst.src = new TacConstant(srcConst.value.castTo(dstType));
                folded = true;
            }
        }
        return inst;
    }

    @Override
    public TacLabel visit(TacLabel inst) {
        throw new UnsupportedOperationException("TacLabel should not be in basic block");
    }

    @Override
    public TacJump visit(TacJump inst) {
        return inst;
    }

    @Override
    public TacInstruction visit(TacJumpIfZero inst) {
        if (inst.cond instanceof TacConstant cond) {
            return cond.value.isZero() ? new TacJump(inst.target) : null;
        }
        return inst;
    }

    @Override
    public TacInstruction visit(TacJumpIfNotZero inst) {
        if (inst.cond instanceof TacConstant cond) {
            return cond.value.isZero() ? null : new TacJump(inst.target);
        }
        return inst;
    }

    @Override
    public TacInstruction visit(TacJumpIfComparison inst) {
        if (inst.lhs instanceof TacConstant lhs && inst.rhs instanceof TacConstant rhs) {
            Constant result = lhs.value.apply(inst.cond, rhs.value);
            boolean taken = !result.isZero();
            if (inst.inverse) {
                taken = !taken;
            }
            return taken ? new TacJump(inst.target) : null;
        }
        return inst;
    }

    @Override
    public TacDirectCall visit(TacDirectCall inst) {
        return inst;
    }

    @Override
    public TacInstruction visit(TacSignExtend inst) {
        if (inst.src instanceof TacConstant srcConst) {
            Constant result = srcConst.value.castTo(getType(inst.dst));
            return new TacCopy(new TacConstant(result), inst.dst);
        }
        return inst;
    }

    @Override
    public TacInstruction visit(TacTruncate inst) {
        if (inst.src instanceof TacConstant srcConst) {
            Constant result = srcConst.value.castTo(getType(inst.dst));
            return new TacCopy(new TacConstant(result), inst.dst);
        }
        return inst;
    }

    @Override
    public TacInstruction visit(TacZeroExtend inst) {
        if (inst.src instanceof TacConstant srcConst) {
            Constant result = srcConst.value.castTo(getType(inst.dst));
            return new TacCopy(new TacConstant(result), inst.dst);
        }
        return inst;
    }

    @Override
    public TacInstruction visit(TacDoubleToInt inst) {
        if (inst.src instanceof TacConstant srcConst) {
            Constant result = srcConst.value.castTo(getType(inst.dst));
            return new TacCopy(new TacConstant(result), inst.dst);
        }
        return inst;
    }

    @Override
    public TacInstruction visit(TacDoubleToUnsignedInt inst) {
        if (inst.src instanceof TacConstant srcConst) {
            Constant result = srcConst.value.castTo(getType(inst.dst));
            return new TacCopy(new TacConstant(result), inst.dst);
        }
        return inst;
    }

    @Override
    public TacInstruction visit(TacIntToDouble inst) {
        if (inst.src instanceof TacConstant srcConst) {
            Constant result = srcConst.value.castTo(getType(inst.dst));
            return new TacCopy(new TacConstant(result), inst.dst);
        }
        return inst;
    }

    @Override
    public TacInstruction visit(TacUnsignedIntToDouble inst) {
        if (inst.src instanceof TacConstant srcConst) {
            Constant result = srcConst.value.castTo(getType(inst.dst));
            return new TacCopy(new TacConstant(result), inst.dst);
        }
        return inst;
    }

    @Override
    public TacGetAddress visit(TacGetAddress inst) {
        return inst;
    }

    @Override
    public TacLoad visit(TacLoad inst) {
        TacAddressDescriptor addr = inst.srcAddr, addrFolded = addr.fold();
        if (addrFolded != addr) {
            inst.srcAddr = addrFolded;
            folded = true;
        }
        return inst;
    }

    @Override
    public TacStore visit(TacStore inst) {
        TacAddressDescriptor addr = inst.dstAddr, addrFolded = addr.fold();
        if (addrFolded != addr) {
            inst.dstAddr = addrFolded;
            folded = true;
        }
        return inst;
    }

    @Override
    public TacInstruction visit(TacAddPointer inst) {
        // dst = ptr + index * scale
        if (inst.index instanceof TacConstant indexConst) {
            if (inst.ptr instanceof TacConstant ptrConst) {
                // 都是常量，直接计算
                long value = ptrConst.value.toLong().value() + indexConst.value.toLong().value() * inst.scale;
                ConstantPointer result = new ConstantPointer(value, ((PointerType) getType(inst.dst)).referencedType());
                return new TacCopy(new TacConstant(result), inst.dst);
            } else {
                // index 为常量，退化为指针加整数
                ConstantLong scaledConst = new ConstantLong(indexConst.value.toLong().value() * inst.scale);
                return new TacBinaryOperation(BinaryOperator.ADD, inst.ptr, new TacConstant(scaledConst), inst.dst);
            }
        }
        return inst;
    }

    @Override
    public TacCopyToOffset visit(TacCopyToOffset inst) {
        return inst;
    }

    @Override
    public TacIndirectCall visit(TacIndirectCall inst) {
        return inst;
    }

    @Override
    public TacCopyByteArrayToOffset visit(TacCopyByteArrayToOffset inst) {
        return inst;
    }

    @Override
    public TacCopyFromOffset visit(TacCopyFromOffset inst) {
        return inst;
    }
}
