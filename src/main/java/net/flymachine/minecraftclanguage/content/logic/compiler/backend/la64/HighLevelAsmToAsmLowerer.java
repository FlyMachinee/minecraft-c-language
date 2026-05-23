package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.util.BitMath;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmDirective;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmInstruction;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmLabel;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmStatement;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.IntInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.UnsignedIntInit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister.*;

public final class HighLevelAsmToAsmLowerer implements HighLevelVisitor<Void> {

    public HighLevelAsmToAsmLowerer(BackendSymbolTable backendSymbolTable) {
        this.backendSymbolTable = backendSymbolTable;
    }

    private final BackendSymbolTable backendSymbolTable;

    private final List<LA64AsmStatement> target = new ArrayList<>();

    private void emit(LA64AsmStatement statement) {
        target.add(statement);
    }

    private void emitLabel(String label) {
        target.add(new LA64AsmLabel(label));
    }

    private void emitDir(String directive, List<LA64DirectiveArgument> args) {
        target.add(new LA64AsmDirective(directive, args));
    }

    private void emitDir(String directive, LA64DirectiveArgument... args) {
        target.add(new LA64AsmDirective(directive, Arrays.asList(args)));
    }

    private void emitDir(String directive) {
        target.add(new LA64AsmDirective(directive, List.of()));
    }

    private void emitInst(String mnemonic, List<LA64AsmOperand> operands) {
        target.add(new LA64AsmInstruction(mnemonic, operands));
    }

    private void emitInst(String mnemonic, LA64AsmOperand... operands) {
        target.add(new LA64AsmInstruction(mnemonic, Arrays.asList(operands)));
    }

    private void emitInst(String mnemonic) {
        target.add(new LA64AsmInstruction(mnemonic, List.of()));
    }

    public List<LA64AsmStatement> lower(HighLevelProgram highLevelProgram) {
        for (HighLevelTopLevel topLevel : highLevelProgram.topLevels) {
            if (topLevel instanceof HighLevelFunction func) {
                lowerFunction(func);
            } else if (topLevel instanceof HighLevelStaticVar staticVar) {
                lowerStaticVariable(staticVar);
            }
        }
        return target;
    }

    private void lowerStaticVariable(HighLevelStaticVar staticVar) {
        if (staticVar.global) {
            emitDir("global", new LA64DirectiveSymArg(staticVar.name));
        }
        long initValue = staticVar.init.toConstantLong().value();
        if (initValue == 0) {
            // 初始化为0，放在bss段
            emitDir("bss");
        } else {
            // 初始化非0，放在data段
            emitDir("data");
        }
        emitDir("balign", new LA64DirectiveNumArg(staticVar.alignment));
        emitLabel(staticVar.name);
        if (initValue != 0) {
            if (staticVar.init instanceof IntInit || staticVar.init instanceof UnsignedIntInit) {
                emitDir("word", new LA64DirectiveNumArg((int) initValue));
            } else {
                emitDir("dword", new LA64DirectiveNumArg(initValue));
            }
        } else {
            emitDir("zero", new LA64DirectiveNumArg(staticVar.alignment));
        }
    }

    private HighLevelFunction functionContext;

    private void lowerFunction(HighLevelFunction function) {
        functionContext = function;

        if (function.global) {
            emitDir("global", new LA64DirectiveSymArg(function.name));
        }
        emitDir("text");
        emitDir("balign", new LA64DirectiveNumArg(4));
        emitLabel(function.name);
        generatePrologue(function);

        for (HighLevelInstruction instruction : function.insts) {
            instruction.accept(this);
        }
    }

    private void generatePrologue(HighLevelFunction function) {
        int size = function.getStackFrameSize();
        // 申请栈空间
        emitInst("addi.d", SP, SP, new LA64AsmImmOperand(-size));
        // 保存 ra 与 fp
        emitInst("st.d", RA, SP, new LA64AsmImmOperand(size - 8));
        emitInst("st.d", FP, SP, new LA64AsmImmOperand(size - 16));
        // 设置新的 fp
        emitInst("addi.d", FP, SP, new LA64AsmImmOperand(size));
    }

    private void generateEpilogue(HighLevelFunction function) {
        int size = function.getStackFrameSize();
        // 恢复 ra 与 fp
        emitInst("ld.d", RA, SP, new LA64AsmImmOperand(size - 8));
        emitInst("ld.d", FP, SP, new LA64AsmImmOperand(size - 16));
        // 释放栈空间
        emitInst("addi.d", SP, SP, new LA64AsmImmOperand(size));
        // 返回
        emitInst("ret");
    }

    @Override
    public Void visitMove(Move inst) {
        AsmType asmType = inst.asmType;
        HighLevelOperand dst = inst.dst;
        HighLevelOperand src = inst.src;

        if (dst instanceof Immediate) {
            throw new UnsupportedOperationException("Cannot move to an immediate");
        }
        if (src instanceof Pseudo || dst instanceof Pseudo) {
            throw new UnsupportedOperationException(
                "Cannot move from/to pseudo register, should be replaced earlier");
        }

        // 特殊情况优化
        if (dst instanceof GeneralPurposeRegister dstReg) {
            // -> 寄存器
            if (src instanceof Immediate srcImm) {
                // 立即数 -> 寄存器
                loadImm(asmType, dstReg, srcImm);
            } else if (src instanceof GeneralPurposeRegister srcReg) {
                // 寄存器 -> 寄存器
                lowerRegMove(srcReg, dstReg);
            } else if (src instanceof Stack srcStack) {
                // 栈 -> 寄存器
                loadStack(asmType, dstReg, srcStack);
            } else if (src instanceof Data srcData) {
                // 全局符号 -> 寄存器
                loadData(asmType, dstReg, srcData);
            }
            return null;
        }
        if (src instanceof GeneralPurposeRegister srcReg) {
            // 寄存器 ->
            if (dst instanceof Stack dstStack) {
                // 寄存器 -> 栈
                storeStack(asmType, srcReg, dstStack);
            } else if (dst instanceof Data dstData) {
                // 寄存器 -> 全局符号
                storeData(asmType, srcReg, dstData, T0);
            }
            return null;
        }

        // 其他情况：将源加载到临时寄存器 T0，再存储到目标
        // 此时 src 为 Immediate/Stack/Data，dst 为 Stack/Data
        GeneralPurposeRegister tmp = T0;
        if (src instanceof Immediate srcImm) {
            loadImm(asmType, tmp, srcImm);
        } else if (src instanceof Stack srcStack) {
            loadStack(asmType, tmp, srcStack);
        } else if (src instanceof Data srcData) {
            loadData(asmType, tmp, srcData);
        } else {
            throw new UnsupportedOperationException("Unsupported source type: " + src.getClass().getSimpleName());
        }

        if (dst instanceof Stack dstStack) {
            storeStack(asmType, tmp, dstStack);
        } else if (dst instanceof Data dstData) {
            storeData(asmType, tmp, dstData, T1);
        } else {
            throw new UnsupportedOperationException("Unsupported destination type: " + dst.getClass().getSimpleName());
        }
        return null;
    }

    private void lowerRegMove(GeneralPurposeRegister srcReg, GeneralPurposeRegister dstReg) {
        if (srcReg == dstReg) {
            return;
        }
        emitInst("move", dstReg, srcReg);
    }

    @Override
    public Void visitRet(Ret inst) {
        generateEpilogue(functionContext);
        return null;
    }

    @Override
    public Void visitBinary(Binary binary) {
        Binary.Operator op = binary.op;
        AsmType asmType = binary.asmType;
        HighLevelOperand lhs = binary.lhs;
        HighLevelOperand rhs = binary.rhs;
        HighLevelOperand dst = binary.dst;
        testBinaryHighLevelOperand(lhs, rhs, dst);

        // 特殊情况检查
        // 立即数加法
        if (op == Binary.Operator.ADD && (lhs instanceof Immediate || rhs instanceof Immediate)) {
            // 检查立即数是否可用 si12 表示，如果可以，生成 addi.w(d) 指令，否则使用默认处理
            if (lhs instanceof Immediate imm && BitMath.isSi12(imm.value())) {
                // 立即数在左操作数
                visitAddSi12(new AddSi12(asmType, rhs, (int) imm.value(), dst));
                return null;
            } else if (rhs instanceof Immediate imm && BitMath.isSi12(imm.value())) {
                // 立即数在右操作数
                visitAddSi12(new AddSi12(asmType, lhs, (int) imm.value(), dst));
                return null;
            }
        }
        // 立即数减法，处理减立即数的情况
        if (op == Binary.Operator.SUB && rhs instanceof Immediate imm && BitMath.isSi12(-imm.value())) {
            visitAddSi12(new AddSi12(asmType, lhs, (int) -imm.value(), dst));
            return null;
        }

        // 通用处理
        String suffix = asmType == AsmType.WORD ? ".w" : ".d";
        String mnemonic = op.mnemonic() + suffix;

        // 加载操作数至寄存器
        GeneralPurposeRegister lhsReg = loadOperand(asmType, lhs, T0);
        GeneralPurposeRegister rhsReg = loadOperand(asmType, rhs, T1);

        // 计算结果的存放地点
        GeneralPurposeRegister dstReg = calcDestination(dst, T0);

        // 计算结果
        // op rd, rj, rk
        emitInst(mnemonic, dstReg, lhsReg, rhsReg);

        storeToDest(asmType, dstReg, dst, T1);
        return null;
    }

    @Override
    public Void visitAddSi12(AddSi12 addSi12) {
        AsmType asmType = addSi12.asmType;
        HighLevelOperand src = addSi12.src;
        int si12 = addSi12.si12;
        HighLevelOperand dst = addSi12.dst;
        testUnaryHighLevelOperand(src, dst);

        if (!BitMath.isSi12(si12)) {
            throw new UnsupportedOperationException("The immediate value must be a si12");
        }

        // 加载操作数至寄存器
        GeneralPurposeRegister srcReg = loadOperand(asmType, src, T0);

        // 计算结果的存放地点
        GeneralPurposeRegister dstReg = calcDestination(dst, T0);

        // 计算结果
        // addi.w(d) rd, rj, si12
        emitInst(asmType == AsmType.WORD ? "addi.w" : "addi.d", dstReg, srcReg, new LA64AsmImmOperand(si12));

        storeToDest(asmType, dstReg, dst, T1);
        return null;
    }

    @Override
    public Void visitLabel(Label inst) {
        emitLabel(".L" + inst.name);
        return null;
    }

    @Override
    public Void visitBranch(Branch inst) {
        emitInst("b", new LA64AsmSymOperand(".L" + inst.target));
        return null;
    }

    @Override
    public Void visitBranchIfZero(BranchIfZero inst) {
        HighLevelOperand cond = inst.cond;
        AsmType asmType = inst.asmType;
        String branchTarget = inst.target;
        testUnaryHighLevelOperand(cond, null);

        // 加载比较值至寄存器
        GeneralPurposeRegister reg = loadOperand(asmType, cond, T0);

        // 跳转
        // beqz rj, offs21
        emitInst("beqz", reg, new LA64AsmSymOperand(".L" + branchTarget));
        return null;
    }

    @Override
    public Void visitBranchIfNotZero(BranchIfNotZero inst) {
        HighLevelOperand cond = inst.cond;
        AsmType asmType = inst.asmType;
        String branchTarget = inst.target;
        testUnaryHighLevelOperand(cond, null);

        // 加载比较值至寄存器
        GeneralPurposeRegister reg = loadOperand(asmType, cond, T0);

        // 跳转
        // bnez rj, offs21
        emitInst("bnez", reg, new LA64AsmSymOperand(".L" + branchTarget));
        return null;
    }

    @Override
    public Void visitBranchIfComparison(BranchIfComparison inst) {
        Comparison cond = inst.cond;
        boolean isUnsigned = inst.isUnsigned;
        AsmType asmType = inst.asmType;
        HighLevelOperand lhs = inst.lhs;
        HighLevelOperand rhs = inst.rhs;
        String branchTarget = inst.target;
        testBinaryHighLevelOperand(lhs, rhs, null);

        String mnemonic = switch (cond) {
            case EQUAL -> "beq";
            case NOT_EQUAL -> "bne";
            case LESS -> isUnsigned ? "bltu" : "blt";
            case LESS_EQUAL -> {
                lhs = inst.rhs;
                rhs = inst.lhs;
                yield isUnsigned ? "bgeu" : "bge";
            }
            case GREATER -> {
                lhs = inst.rhs;
                rhs = inst.lhs;
                yield isUnsigned ? "bltu" : "blt";
            }
            case GREATER_EQUAL -> isUnsigned ? "bgeu" : "bge";
        };

        // 加载左、右操作数至寄存器
        GeneralPurposeRegister lhsReg = loadOperand(asmType, lhs, T0);
        GeneralPurposeRegister rhsReg = loadOperand(asmType, rhs, T1);

        // 跳转
        // beq/bne/blt/bge rj, rd, offs16
        emitInst(mnemonic, lhsReg, rhsReg, new LA64AsmSymOperand(".L" + branchTarget));
        return null;
    }

    @Override
    public Void visitCall(Call inst) {
        emitInst("bl", new LA64AsmSymOperand(inst.name));
        return null;
    }

    @Override
    public Void visitCompare(Compare inst) {
        Comparison op = inst.cond;
        boolean isUnsigned = inst.isUnsigned;
        AsmType asmType = inst.asmType;
        HighLevelOperand lhs = inst.lhs;
        HighLevelOperand rhs = inst.rhs;
        HighLevelOperand dst = inst.dst;
        testBinaryHighLevelOperand(lhs, rhs, dst);

        // 交换左、右操作数，全部转换为小于或小于等于
        switch (op) {
            case GREATER -> {
                op = Comparison.LESS;
                lhs = inst.rhs;
                rhs = inst.lhs;
            }
            case GREATER_EQUAL -> {
                op = Comparison.LESS_EQUAL;
                lhs = inst.rhs;
                rhs = inst.lhs;
            }
        }

        // 小于立即数
        if (op == Comparison.LESS && rhs instanceof Immediate imm && BitMath.isSi12(imm.value())) {
            GeneralPurposeRegister srcReg = loadOperand(asmType, lhs, T0);
            GeneralPurposeRegister dstReg = calcDestination(dst, T0);
            // slti rd, rj, si12
            emitInst("slti", dstReg, srcReg, new LA64AsmImmOperand((int) imm.value()));
            storeToDest(AsmType.WORD, dstReg, dst, T1);
            return null;
        }

        // 通用处理
        String mnemonic = switch (op) {
            case EQUAL -> "seq";
            case NOT_EQUAL -> "sne";
            case LESS -> (isUnsigned ? "sltu" : "slt");
            case LESS_EQUAL -> (isUnsigned ? "sleu" : "sle");
            default -> throw new IllegalStateException("Unexpected comparison operator: " + op);
        };

        // 加载操作数至寄存器
        GeneralPurposeRegister lhsReg = loadOperand(asmType, lhs, T0);
        GeneralPurposeRegister rhsReg = loadOperand(asmType, rhs, T1);

        // 计算结果的存放地点
        GeneralPurposeRegister dstReg = calcDestination(dst, T0);

        // 计算结果
        // op rd, rj, rk
        emitInst(mnemonic, dstReg, lhsReg, rhsReg);

        storeToDest(AsmType.WORD, dstReg, dst, T1);
        return null;
    }

    @Override
    public Void visitDivOrMod(DivOrMod inst) {
        boolean isDiv = inst.isDiv;
        boolean isUnsigned = inst.isUnsigned;
        AsmType asmType = inst.asmType;
        HighLevelOperand lhs = inst.lhs;
        HighLevelOperand rhs = inst.rhs;
        HighLevelOperand dst = inst.dst;
        testBinaryHighLevelOperand(lhs, rhs, dst);

        String mnemonic = (isDiv ? "div" : "mod") + (asmType == AsmType.WORD ? ".w" : ".d") + (isUnsigned ? "u" : "");

        // 加载操作数至寄存器
        GeneralPurposeRegister lhsReg = loadOperand(asmType, lhs, T0);
        GeneralPurposeRegister rhsReg = loadOperand(asmType, rhs, T1);

        // 计算结果的存放地点
        GeneralPurposeRegister dstReg = calcDestination(dst, T0);

        // 计算结果
        // op rd, rj, rk
        emitInst(mnemonic, dstReg, lhsReg, rhsReg);

        storeToDest(asmType, dstReg, dst, T1);
        return null;
    }

    @Override
    public Void visitBitwiseShift(BitwiseShift inst) {
        boolean isLeftShift = inst.isLeftShift;
        boolean isUnsigned = inst.isUnsigned;
        AsmType asmType = inst.asmType;
        HighLevelOperand lhs = inst.lhs;
        HighLevelOperand rhs = inst.rhs;
        HighLevelOperand dst = inst.dst;
        testBinaryHighLevelOperand(lhs, rhs, dst);

        String prefix = isLeftShift ? "sll" : isUnsigned ? "srl" : "sra";
        String suffix = asmType == AsmType.WORD ? ".w" : ".d";

        // 立即数右移或左移，处理右操作数为立即数的情况
        if (rhs instanceof Immediate imm) {
            String mnemonic = prefix + "i" + suffix;
            GeneralPurposeRegister srcReg = loadOperand(asmType, lhs, T0);
            GeneralPurposeRegister dstReg = calcDestination(dst, T0);
            // op rd, rj, ui5(ui6)
            emitInst(
                mnemonic, dstReg, srcReg,
                new LA64AsmImmOperand(BitMath.extractBits((int) imm.value(), asmType == AsmType.WORD ? 5 : 6)));
            storeToDest(asmType, dstReg, dst, T1);
            return null;
        }

        // 通用处理
        String mnemonic = prefix + suffix;

        // 加载左操作数至寄存器
        GeneralPurposeRegister lhsReg = loadOperand(asmType, lhs, T0);
        GeneralPurposeRegister rhsReg = loadOperand(AsmType.WORD, rhs, T1); // TODO: 修改为 BYTE 当实现 char 时

        // 计算结果的存放地点
        GeneralPurposeRegister dstReg = calcDestination(dst, T0);

        // 计算结果
        // op rd, rj, rk
        emitInst(mnemonic, dstReg, lhsReg, rhsReg);

        storeToDest(asmType, dstReg, dst, T1);
        return null;
    }

    @Override
    public Void visitBitwise(Bitwise inst) {
        Bitwise.Operator op = inst.op;
        AsmType asmType = inst.asmType;
        HighLevelOperand lhs = inst.lhs;
        HighLevelOperand rhs = inst.rhs;
        HighLevelOperand dst = inst.dst;
        testBinaryHighLevelOperand(lhs, rhs, dst);

        // 特殊情况检查
        // 立即数位运算
        if (lhs instanceof Immediate || rhs instanceof Immediate) {
            // 检查立即数是否可用 ui12 表示，如果可以，生成 andi/ori/xori 指令，否则使用默认处理

            if (lhs instanceof Immediate) {
                // 把立即数放在右操作数
                lhs = inst.rhs;
                rhs = inst.lhs;
            }

            // andi/ori/xori rd, rj, ui12
            Immediate imm = (Immediate) rhs;
            if (BitMath.isUi12(imm.value())) {
                String mnemonic = op.mnemonic() + "i";
                GeneralPurposeRegister srcReg = loadOperand(asmType, lhs, T0);
                GeneralPurposeRegister dstReg = calcDestination(dst, T0);
                emitInst(mnemonic, dstReg, srcReg, new LA64AsmImmOperand(imm));
                storeToDest(asmType, dstReg, dst, T1);
                return null;
            }
        }

        // 通用处理
        String mnemonic = op.mnemonic();

        // 加载操作数至寄存器
        GeneralPurposeRegister lhsReg = loadOperand(asmType, lhs, T0);
        GeneralPurposeRegister rhsReg = loadOperand(asmType, rhs, T1);

        // 计算结果的存放地点
        GeneralPurposeRegister dstReg = calcDestination(dst, T0);

        // 计算结果
        // op rd, rj, rk
        emitInst(mnemonic, dstReg, lhsReg, rhsReg);

        storeToDest(asmType, dstReg, dst, T1);
        return null;
    }

    @Override
    public Void visitBstrpickZeroExtend(BstrpickZeroExtend inst) {
        HighLevelOperand src = inst.src;
        HighLevelOperand dst = inst.dst;
        testUnaryHighLevelOperand(src, dst);

        // 加载操作数至寄存器
        GeneralPurposeRegister srcReg = loadOperand(AsmType.WORD, src, T0);

        // 计算结果的存放地点
        GeneralPurposeRegister dstReg = calcDestination(dst, T0);

        // 计算结果
        zeroExtend(srcReg, dstReg);

        storeToDest(AsmType.DWORD, dstReg, dst, T1);
        return null;
    }

    @Override
    public Void visitAddSignExtend(AddSignExtend inst) {
        HighLevelOperand src = inst.src;
        HighLevelOperand dst = inst.dst;
        testUnaryHighLevelOperand(src, dst);

        // 加载操作数至寄存器
        GeneralPurposeRegister srcReg = loadOperand(AsmType.WORD, src, T0);

        // 计算结果的存放地点
        GeneralPurposeRegister dstReg = calcDestination(dst, T0);

        // 计算结果
        signedExtend(srcReg, dstReg);

        storeToDest(AsmType.DWORD, dstReg, dst, T1);
        return null;
    }

    private static void testUnaryHighLevelOperand(HighLevelOperand src, HighLevelOperand dst) {
        if (dst instanceof Immediate) {
            throw new UnsupportedOperationException("Cannot store result in an immediate");
        }
        if (src instanceof Pseudo || dst instanceof Pseudo) {
            throw new UnsupportedOperationException("Cannot operate on pseudo register, should be replaced earlier");
        }
    }

    private static void testBinaryHighLevelOperand(HighLevelOperand lhs, HighLevelOperand rhs, HighLevelOperand dst) {
        if (dst instanceof Immediate) {
            throw new UnsupportedOperationException("Cannot store result in an immediate");
        }
        if (lhs instanceof Pseudo || rhs instanceof Pseudo || dst instanceof Pseudo) {
            throw new UnsupportedOperationException("Cannot operate on pseudo register, should be replaced earlier");
        }
    }

    private void zeroExtend(GeneralPurposeRegister src, GeneralPurposeRegister dst) {
        // bstrpick.d rd, rj, msbd, lsbd
        emitInst("bstrpick.d", dst, src, new LA64AsmImmOperand(31), new LA64AsmImmOperand(0));
    }

    private void signedExtend(GeneralPurposeRegister src, GeneralPurposeRegister dst) {
        // add.w rd, rj, r0
        // emitInst("add.w", dst, src, ZERO);
        // 默认内容均为符号拓展，无需显式处理
    }

    /**
     * 加载操作数至寄存器
     *
     * @param asmType  操作数类型
     * @param toLoad   操作数
     * @param fallback 若操作数为内存，用于存放操作数位置的寄存器
     * @return 最终存放操作数的寄存器
     */
    private GeneralPurposeRegister loadOperand(
        AsmType asmType, HighLevelOperand toLoad, GeneralPurposeRegister fallback) {

        // 加载操作数至寄存器
        if (toLoad instanceof GeneralPurposeRegister reg) {
            // 本身就在寄存器，直接使用
            return reg;
        }
        if (toLoad instanceof Stack stack) {
            // 在内存，加载到 fallback 寄存器
            loadStack(asmType, fallback, stack);
            return fallback;
        } else if (toLoad instanceof Immediate immediate) {
            // 立即数，加载到 fallback 寄存器
            loadImm(asmType, fallback, immediate);
            return fallback;
        } else if (toLoad instanceof Data data) {
            // 全局符号，加载到 fallback 寄存器
            loadData(asmType, fallback, data);
            return fallback;
        } else {
            throw new UnsupportedOperationException("Unsupported operand type: " + toLoad.getClass().getSimpleName());
        }
    }

    /**
     * 计算将结果存放至 {@code dst} 需要的寄存器
     *
     * @param dst      结果存放位置
     * @param fallback 如果需要存储，存放到该寄存器
     * @return 计算结果，实际存放结果的寄存器
     */
    private static GeneralPurposeRegister calcDestination(HighLevelOperand dst, GeneralPurposeRegister fallback) {

        // 计算结果的存放地点
        if (dst instanceof GeneralPurposeRegister reg) {
            // 存放至寄存器，直接赋值
            return reg;
        } else if (dst instanceof Stack || dst instanceof Data) {
            // 存放至内存，得先存放到 fallback
            return fallback;
        } else {
            throw new UnsupportedOperationException("Unsupported destination type: " + dst.getClass().getSimpleName());
        }
    }

    /**
     * 将结果存放至目标地点
     *
     * @param asmType 结果值类型
     * @param val     计算结果
     * @param dest    结果需要存放的位置
     * @param tmpAddr 写入全局符号时使用的临时寄存器，调用者保证在调用前后该寄存器值不被使用
     */
    private void storeToDest(
        AsmType asmType, GeneralPurposeRegister val, HighLevelOperand dest, GeneralPurposeRegister tmpAddr) {

        // 将先前存放在寄存器的结果写回目标地点
        if (dest instanceof GeneralPurposeRegister reg) {
            lowerRegMove(val, reg);
        } else if (dest instanceof Stack dstStack) {
            storeStack(asmType, val, dstStack);
        } else if (dest instanceof Data dstData) {
            storeData(asmType, val, dstData, tmpAddr);
        }
    }

    /**
     * 加载立即数至寄存器
     *
     * @param asmType 目标寄存器类型
     * @param dst     目标寄存器
     * @param imm     加载的立即数
     */
    private void loadImm(AsmType asmType, GeneralPurposeRegister dst, Immediate imm) {
        switch (asmType) {
            case WORD -> emitInst("li.w", dst, new LA64AsmImmOperand((int) imm.value()));
            case DWORD -> emitInst("li.d", dst, new LA64AsmImmOperand(imm));
        }
    }

    /**
     * 加载栈上值至寄存器
     *
     * @param asmType 加载值类型
     * @param dst     目标寄存器
     * @param src     栈位置
     */
    private void loadStack(AsmType asmType, GeneralPurposeRegister dst, Stack src) {
        GeneralPurposeRegister regRef = src.fpRelative() ? FP : SP;
        switch (asmType) {
            case WORD -> emitInst("ld.w", dst, regRef, new LA64AsmImmOperand(src.offset()));
            case DWORD -> emitInst("ld.d", dst, regRef, new LA64AsmImmOperand(src.offset()));
        }
    }

    /**
     * 将寄存器值写入栈上位置
     *
     * @param asmType 写入值类型
     * @param val     写入值所在寄存器，保证写入寄存器不变
     * @param dst     栈位置
     */
    private void storeStack(AsmType asmType, GeneralPurposeRegister val, Stack dst) {
        GeneralPurposeRegister regRef = dst.fpRelative() ? FP : SP;
        switch (asmType) {
            case WORD -> emitInst("st.w", val, regRef, new LA64AsmImmOperand(dst.offset()));
            case DWORD -> emitInst("st.d", val, regRef, new LA64AsmImmOperand(dst.offset()));
        }
    }

    /**
     * 加载全局符号所指向内存中的值至寄存器
     *
     * @param asmType 加载值类型
     * @param dst     目标寄存器
     * @param sym     全局符号
     */
    private void loadData(AsmType asmType, GeneralPurposeRegister dst, Data sym) {
        // 先加载符号地址
        emitInst("la.pcrel", dst, new LA64AsmSymOperand(sym.name()));
        // 再对符号地址访存
        switch (asmType) {
            case WORD -> emitInst("ld.w", dst, dst, LA64AsmImmOperand.ZERO);
            case DWORD -> emitInst("ld.d", dst, dst, LA64AsmImmOperand.ZERO);
        }
    }

    /**
     * 将寄存器值写入全局符号所指向内存中的位置
     *
     * @param asmType 写入值的类型
     * @param val     写入值所在寄存器，保证写入寄存器值不变，除非 val 与 tmpExt 相同
     * @param sym     全局符号
     * @param tmpAddr 临时寄存器，用于地址计算，调用者保证在调用前后该寄存器值不被使用
     */
    private void storeData(
        AsmType asmType, GeneralPurposeRegister val, Data sym,
        GeneralPurposeRegister tmpAddr) {

        // 先加载符号地址
        emitInst("la.pcrel", tmpAddr, new LA64AsmSymOperand(sym.name()));
        // 再将值写入符号地址
        switch (asmType) {
            case WORD -> emitInst("st.w", val, tmpAddr, LA64AsmImmOperand.ZERO);
            case DWORD -> emitInst("st.d", val, tmpAddr, LA64AsmImmOperand.ZERO);
        }
    }
}
