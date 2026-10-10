package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction.LA64FloatCompareCondition;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.ConditionFlagRegister;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.FloatingPointRegister;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.LA64Register;
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
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.FloatingPointRegister.FT0;
import static net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.FloatingPointRegister.FT1;
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
            } else if (topLevel instanceof HighLevelStaticConst staticConst) {
                lowerStaticConst(staticConst);
            }
        }
        return target;
    }

    private void lowerStaticVariable(HighLevelStaticVar staticVar) {
        if (staticVar.global) {
            emitDir("global", new LA64DirectiveSymArg(staticVar.name));
        }
        if (staticVar.init.stream().allMatch(init -> init instanceof ZeroInit)) {
            // 初始化为0，放在bss段
            emitDir("bss");
        } else {
            // 初始化非0，放在data段
            emitDir("data");
        }
        emitDir("balign", new LA64DirectiveNumArg(staticVar.alignment));
        emitLabel(staticVar.name);
        for (StaticInit init : staticVar.init) {
            lowerStaticInit(init);
        }
    }

    private void lowerStaticInit(StaticInit init) {
        if (init instanceof IntInit intInit) {
            emitDir("word", new LA64DirectiveNumArg(intInit.value()));
        } else if (init instanceof LongInit longInit) {
            emitDir("dword", new LA64DirectiveNumArg(longInit.value()));
        } else if (init instanceof UnsignedIntInit unsignedIntInit) {
            emitDir("word", new LA64DirectiveNumArg(unsignedIntInit.value()));
        } else if (init instanceof UnsignedLongInit unsignedLongInit) {
            emitDir("dword", new LA64DirectiveNumArg(unsignedLongInit.value()));
        } else if (init instanceof DoubleInit doubleInit) {
            emitDir("dword", new LA64DirectiveNumArg(Double.doubleToLongBits(doubleInit.value())));
        } else if (init instanceof ZeroInit zeroInit) {
            emitDir("zero", new LA64DirectiveNumArg(zeroInit.bytes()));
        } else if (init instanceof SymbolInit symbolInit) {
            emitDir("dword", new LA64DirectiveSymArg(symbolInit.symbol(), symbolInit.offset()));
        } else if (init instanceof CharInit charInit) {
            emitDir("byte", new LA64DirectiveNumArg(charInit.value()));
        } else if (init instanceof UnsignedCharInit unsignedCharInit) {
            emitDir("byte", new LA64DirectiveNumArg(unsignedCharInit.value()));
        } else if (init instanceof StringInit stringInit) {
            String directive = stringInit.nullTerminated() ? "asciz" : "ascii";
            emitDir(directive, new LA64DirectiveStrArg(toAsciiString(stringInit.bytes())));
        } else {
            throw new UnsupportedOperationException(
                "Unsupported static variable initializer: " + init.getClass().getSimpleName());
        }
    }

    private String toAsciiString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            int v = b & 0xFF;
            switch (v) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\t' -> sb.append("\\t");
                case '\r' -> sb.append("\\r");
                case 0 -> sb.append("\\000");
                default -> {
                    if (v >= 0x20 && v < 0x7F) {
                        sb.append((char) v);
                    } else {
                        sb.append(String.format("\\%03o", v));
                    }
                }
            }
        }
        return sb.toString();
    }

    private void lowerStaticConst(HighLevelStaticConst staticConst) {
        emitDir("rodata");
        emitDir("balign", new LA64DirectiveNumArg(staticConst.alignment));
        emitLabel(".L" + staticConst.name);
        lowerStaticInit(staticConst.staticInit);
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
        long size = function.getStackFrameSize();
        // 申请栈空间
        lowerBinary(Binary.Operator.ADD, AsmType.DWORD, SP, new Immediate(-size), SP);
        // 保存 ra 与 fp
        lowerStore(AsmType.DWORD, RA, SP, new Immediate(size - 8));
        lowerStore(AsmType.DWORD, FP, SP, new Immediate(size - 16));
        // 设置新的 fp
        lowerBinary(Binary.Operator.ADD, AsmType.DWORD, SP, new Immediate(size), FP);
    }

    private void generateEpilogue(HighLevelFunction function) {
        long size = function.getStackFrameSize();
        // 恢复 ra 与 fp
        lowerLoad(AsmType.DWORD, SP, new Immediate(size - 8), RA);
        lowerLoad(AsmType.DWORD, SP, new Immediate(size - 16), FP);
        // 释放栈空间
        lowerBinary(Binary.Operator.ADD, AsmType.DWORD, SP, new Immediate(size), SP);
        // 返回
        emitInst("ret");
    }

    @Override
    public Void visit(Move inst) {
        AsmType asmType = inst.asmType;
        HighLevelOperand dst = inst.dst;
        HighLevelOperand src = inst.src;
        testUnaryHighLevelOperand(src, dst);
        testInnerType(asmType);

        // 特殊情况优化
        if (dst instanceof LA64Register dstReg) {
            // -> 寄存器
            if (src instanceof Immediate srcImm) {
                // 立即数 -> 寄存器
                loadImm(dstReg, srcImm, T0);
            } else if (src instanceof LA64Register srcReg) {
                // 寄存器 -> 寄存器
                lowerRegMove(srcReg, dstReg);
            } else if (src instanceof Memory srcMem) {
                // 内存 -> 寄存器
                lowerLoad(asmType, srcMem.gpr(), new Immediate(srcMem.offset()), (HighLevelOperand) dstReg);
            } else if (src instanceof Data srcData) {
                // 全局符号 -> 寄存器
                loadData(asmType, dstReg, srcData, T0);
            }
            return null;
        }
        if (src instanceof LA64Register srcReg) {
            // 寄存器 ->
            if (dst instanceof Memory dstMem) {
                // 寄存器 -> 内存
                lowerStore(asmType, (HighLevelOperand) srcReg, dstMem.gpr(), new Immediate(dstMem.offset()));
            } else if (dst instanceof Data dstData) {
                // 寄存器 -> 全局符号
                storeData(asmType, srcReg, dstData, T0);
            }
            return null;
        }

        // 其他情况：将源加载到临时寄存器，再存储到目标
        // 此时 src 为 Immediate/Stack/Data，dst 为 Stack/Data
        LA64Register tmp = asmType.equals(AsmType.DOUBLE) ? FT0 : T0;
        if (src instanceof Immediate srcImm) {
            loadImm(tmp, srcImm, T0);
        } else if (src instanceof Memory srcMem) {
            lowerLoad(asmType, srcMem.gpr(), new Immediate(srcMem.offset()), (HighLevelOperand) tmp);
        } else if (src instanceof Data srcData) {
            loadData(asmType, tmp, srcData, T0);
        } else {
            throw new UnsupportedOperationException("Unsupported source type: " + src.getClass().getSimpleName());
        }

        if (dst instanceof Memory dstMem) {
            lowerStore(asmType, (HighLevelOperand) tmp, dstMem.gpr(), new Immediate(dstMem.offset()));
        } else if (dst instanceof Data dstData) {
            storeData(asmType, tmp, dstData, T1);
        } else {
            throw new UnsupportedOperationException("Unsupported destination type: " + dst.getClass().getSimpleName());
        }
        return null;
    }

    // 只进行相同二进制表示之间的移动
    private void lowerRegMove(LA64Register srcReg, LA64Register dstReg) {
        if (srcReg.equals(dstReg)) {
            return;
        }
        if (srcReg instanceof GeneralPurposeRegister srcGpr) {
            if (dstReg instanceof GeneralPurposeRegister dstGpr) {
                emitInst("move", dstGpr, srcGpr);
                return;
            } else if (dstReg instanceof FloatingPointRegister dstFpr) {
                emitInst("movgr2fr.d", dstFpr, srcGpr);
                return;
            }
        } else if (srcReg instanceof FloatingPointRegister srcFpr) {
            if (dstReg instanceof GeneralPurposeRegister dstGpr) {
                emitInst("movfr2gr.d", dstGpr, srcFpr);
                return;
            } else if (dstReg instanceof FloatingPointRegister dstFpr) {
                emitInst("fmov.d", dstFpr, srcFpr);
                return;
            }
        } else if (srcReg instanceof ConditionFlagRegister srcCfr) {
            if (dstReg instanceof GeneralPurposeRegister dstGpr) {
                emitInst("movcf2gr", dstGpr, srcCfr);
                return;
            }
        }
        throw new UnsupportedOperationException("Invalid reg move from " + srcReg + " to " + dstReg);
    }

    @Override
    public Void visit(Ret inst) {
        generateEpilogue(functionContext);
        return null;
    }

    @Override
    public Void visit(Binary binary) {
        lowerBinary(binary.op, binary.asmType, binary.lhs, binary.rhs, binary.dst);
        return null;
    }

    private void lowerBinary(
        Binary.Operator op, AsmType asmType, HighLevelOperand lhs, HighLevelOperand rhs, HighLevelOperand dst) {

        testBinaryHighLevelOperand(lhs, rhs, dst);
        testByteType(asmType);
        testInnerType(asmType);

        // 浮点处理
        if (asmType.equals(AsmType.DOUBLE)) {
            String mnemonic = "f" + op.mnemonic() + ".d";
            FloatingPointRegister lhsReg = (FloatingPointRegister) loadOperand(AsmType.DOUBLE, lhs, FT0, T0);
            FloatingPointRegister rhsReg = (FloatingPointRegister) loadOperand(AsmType.DOUBLE, rhs, FT1, T0);
            FloatingPointRegister dstReg = (FloatingPointRegister) calcDestination(dst, FT0);
            emitInst(mnemonic, dstReg, lhsReg, rhsReg);
            storeToDest(AsmType.DOUBLE, dstReg, dst, T0);
            return;
        }

        // 特殊情况检查
        // 立即数加法
        if (op == Binary.Operator.ADD && (lhs instanceof Immediate || rhs instanceof Immediate)) {
            // 检查立即数是否可用 si12 表示，如果可以，生成 addi.w(d) 指令，否则使用默认处理
            if (lhs instanceof Immediate imm && BitMath.isSi12(imm.value())) {
                // 立即数在左操作数
                visit(new AddSi12(asmType, rhs, (int) imm.value(), dst));
                return;
            } else if (rhs instanceof Immediate imm && BitMath.isSi12(imm.value())) {
                // 立即数在右操作数
                visit(new AddSi12(asmType, lhs, (int) imm.value(), dst));
                return;
            }
        }
        // 立即数减法，处理减立即数的情况
        if (op == Binary.Operator.SUB && rhs instanceof Immediate imm && BitMath.isSi12(-imm.value())) {
            visit(new AddSi12(asmType, lhs, (int) -imm.value(), dst));
            return;
        }

        // 通用处理
        String suffix = asmType.equals(AsmType.WORD) ? ".w" : ".d";
        String mnemonic = op.mnemonic() + suffix;
        GeneralPurposeRegister lhsReg = (GeneralPurposeRegister) loadOperand(asmType, lhs, T0, T0);
        GeneralPurposeRegister rhsReg = (GeneralPurposeRegister) loadOperand(asmType, rhs, T1, T1);
        GeneralPurposeRegister dstReg = (GeneralPurposeRegister) calcDestination(dst, T0);
        emitInst(mnemonic, dstReg, lhsReg, rhsReg);
        storeToDest(asmType, dstReg, dst, T1);
    }

    @Override
    public Void visit(AddSi12 addSi12) {
        AsmType asmType = addSi12.asmType;
        testByteType(asmType);
        testInnerType(asmType);
        HighLevelOperand src = addSi12.src;
        int si12 = addSi12.si12;
        HighLevelOperand dst = addSi12.dst;
        testUnaryHighLevelOperand(src, dst);

        if (!BitMath.isSi12(si12)) {
            throw new UnsupportedOperationException("The immediate value must be a si12");
        }

        // 加载操作数至寄存器
        GeneralPurposeRegister srcReg = (GeneralPurposeRegister) loadOperand(asmType, src, T0, T0);

        // 计算结果的存放地点
        GeneralPurposeRegister dstReg = (GeneralPurposeRegister) calcDestination(dst, T0);

        // 计算结果
        // addi.w(d) rd, rj, si12
        emitInst(asmType.equals(AsmType.WORD) ? "addi.w" : "addi.d", dstReg, srcReg, new LA64AsmImmOperand(si12));

        storeToDest(asmType, dstReg, dst, T1);
        return null;
    }

    @Override
    public Void visit(Label inst) {
        emitLabel(".L" + inst.name);
        return null;
    }

    @Override
    public Void visit(Branch inst) {
        emitInst("b", new LA64AsmSymOperand(".L" + inst.target));
        return null;
    }

    @Override
    public Void visit(BranchIfZero inst) {
        HighLevelOperand cond = inst.cond;
        AsmType asmType = inst.asmType;
        String branchTarget = inst.target;
        testUnaryHighLevelOperand(cond, null);
        testInnerType(asmType);

        // 加载比较值至寄存器
        GeneralPurposeRegister reg = (GeneralPurposeRegister) loadOperand(asmType, cond, T0, T0);

        // 跳转
        // beqz rj, offs21
        emitInst("beqz", reg, new LA64AsmSymOperand(".L" + branchTarget));
        return null;
    }

    @Override
    public Void visit(BranchIfNotZero inst) {
        HighLevelOperand cond = inst.cond;
        AsmType asmType = inst.asmType;
        String branchTarget = inst.target;
        testUnaryHighLevelOperand(cond, null);
        testInnerType(asmType);

        // 加载比较值至寄存器
        GeneralPurposeRegister reg = (GeneralPurposeRegister) loadOperand(asmType, cond, T0, T0);

        // 跳转
        // bnez rj, offs21
        emitInst("bnez", reg, new LA64AsmSymOperand(".L" + branchTarget));
        return null;
    }

    @Override
    public Void visit(BranchIfComparison inst) {
        Comparison cond = inst.cond;
        boolean isUnsigned = inst.isUnsigned;
        AsmType asmType = inst.asmType;
        HighLevelOperand lhs = inst.lhs;
        HighLevelOperand rhs = inst.rhs;
        String branchTarget = inst.target;
        testBinaryHighLevelOperand(lhs, rhs, null);
        testByteType(asmType);
        testInnerType(asmType);

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
        GeneralPurposeRegister lhsReg = (GeneralPurposeRegister) loadOperand(asmType, lhs, T0, T0);
        GeneralPurposeRegister rhsReg = (GeneralPurposeRegister) loadOperand(asmType, rhs, T1, T1);

        // 跳转
        // beq/bne/blt/bge rj, rd, offs16
        emitInst(mnemonic, lhsReg, rhsReg, new LA64AsmSymOperand(".L" + branchTarget));
        return null;
    }

    @Override
    public Void visit(Call inst) {
        emitInst("bl", new LA64AsmSymOperand(inst.name));
        return null;
    }

    @Override
    public Void visit(Compare inst) {
        Comparison op = inst.cond;
        boolean isUnsigned = inst.isUnsigned;
        AsmType asmType = inst.asmType;
        HighLevelOperand lhs = inst.lhs;
        HighLevelOperand rhs = inst.rhs;
        HighLevelOperand dst = inst.dst;
        testBinaryHighLevelOperand(lhs, rhs, dst);
        testByteType(asmType);
        testInnerType(asmType);

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
            GeneralPurposeRegister srcReg = (GeneralPurposeRegister) loadOperand(asmType, lhs, T0, T0);
            GeneralPurposeRegister dstReg = (GeneralPurposeRegister) calcDestination(dst, T0);
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
        GeneralPurposeRegister lhsReg = (GeneralPurposeRegister) loadOperand(asmType, lhs, T0, T0);
        GeneralPurposeRegister rhsReg = (GeneralPurposeRegister) loadOperand(asmType, rhs, T1, T1);
        GeneralPurposeRegister dstReg = (GeneralPurposeRegister) calcDestination(dst, T0);
        emitInst(mnemonic, dstReg, lhsReg, rhsReg);
        storeToDest(AsmType.WORD, dstReg, dst, T1);
        return null;
    }

    @Override
    public Void visit(DivOrMod inst) {
        boolean isDiv = inst.isDiv;
        boolean isUnsigned = inst.isUnsigned;
        AsmType asmType = inst.asmType;
        HighLevelOperand lhs = inst.lhs;
        HighLevelOperand rhs = inst.rhs;
        HighLevelOperand dst = inst.dst;
        testBinaryHighLevelOperand(lhs, rhs, dst);
        testByteType(asmType);
        testInnerType(asmType);

        if (asmType.equals(AsmType.DOUBLE)) {
            String mnemonic = "fdiv.d";
            FloatingPointRegister lhsReg = (FloatingPointRegister) loadOperand(AsmType.DOUBLE, lhs, FT0, T0);
            FloatingPointRegister rhsReg = (FloatingPointRegister) loadOperand(AsmType.DOUBLE, rhs, FT1, T0);
            FloatingPointRegister dstReg = (FloatingPointRegister) calcDestination(dst, FT0);
            emitInst(mnemonic, dstReg, lhsReg, rhsReg);
            storeToDest(AsmType.DOUBLE, dstReg, dst, T0);
            return null;
        }

        String mnemonic = (isDiv ? "div" : "mod") + (asmType.equals(AsmType.WORD) ? ".w" : ".d") +
                          (isUnsigned ? "u" : "");

        // 加载操作数至寄存器
        GeneralPurposeRegister lhsReg = (GeneralPurposeRegister) loadOperand(asmType, lhs, T0, T0);
        GeneralPurposeRegister rhsReg = (GeneralPurposeRegister) loadOperand(asmType, rhs, T1, T1);
        GeneralPurposeRegister dstReg = (GeneralPurposeRegister) calcDestination(dst, T0);
        emitInst(mnemonic, dstReg, lhsReg, rhsReg);
        storeToDest(asmType, dstReg, dst, T1);
        return null;
    }

    @Override
    public Void visit(BitwiseShift inst) {
        boolean isLeftShift = inst.isLeftShift;
        boolean isUnsigned = inst.isUnsigned;
        AsmType asmType = inst.asmType;
        HighLevelOperand lhs = inst.lhs;
        HighLevelOperand rhs = inst.rhs;
        HighLevelOperand dst = inst.dst;
        testBinaryHighLevelOperand(lhs, rhs, dst);
        testByteType(asmType);
        testInnerType(asmType);

        String prefix = isLeftShift ? "sll" : isUnsigned ? "srl" : "sra";
        String suffix = asmType.equals(AsmType.WORD) ? ".w" : ".d";

        // 立即数右移或左移，处理右操作数为立即数的情况
        if (rhs instanceof Immediate imm) {
            String mnemonic = prefix + "i" + suffix;
            GeneralPurposeRegister srcReg = (GeneralPurposeRegister) loadOperand(asmType, lhs, T0, T0);
            GeneralPurposeRegister dstReg = (GeneralPurposeRegister) calcDestination(dst, T0);
            // op rd, rj, ui5(ui6)
            emitInst(
                mnemonic, dstReg, srcReg,
                new LA64AsmImmOperand(BitMath.extractBits((int) imm.value(), asmType.equals(AsmType.WORD) ? 5 : 6)));
            storeToDest(asmType, dstReg, dst, T1);
            return null;
        }

        // 通用处理
        String mnemonic = prefix + suffix;
        GeneralPurposeRegister lhsReg = (GeneralPurposeRegister) loadOperand(asmType, lhs, T0, T0);
        GeneralPurposeRegister rhsReg = (GeneralPurposeRegister) loadOperand(AsmType.WORD, rhs, T1, T1);
        // TODO: 修改为 BYTE 当实现 char 时

        GeneralPurposeRegister dstReg = (GeneralPurposeRegister) calcDestination(dst, T0);
        emitInst(mnemonic, dstReg, lhsReg, rhsReg);
        storeToDest(asmType, dstReg, dst, T1);
        return null;
    }

    @Override
    public Void visit(Bitwise inst) {
        Bitwise.Operator op = inst.op;
        AsmType asmType = inst.asmType;
        HighLevelOperand lhs = inst.lhs;
        HighLevelOperand rhs = inst.rhs;
        HighLevelOperand dst = inst.dst;
        testBinaryHighLevelOperand(lhs, rhs, dst);
        testByteType(asmType);
        testInnerType(asmType);

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
                GeneralPurposeRegister srcReg = (GeneralPurposeRegister) loadOperand(asmType, lhs, T0, T0);
                GeneralPurposeRegister dstReg = (GeneralPurposeRegister) calcDestination(dst, T0);
                emitInst(mnemonic, dstReg, srcReg, new LA64AsmImmOperand(imm));
                storeToDest(asmType, dstReg, dst, T1);
                return null;
            }
        }

        // 通用处理
        String mnemonic = op.mnemonic();
        GeneralPurposeRegister lhsReg = (GeneralPurposeRegister) loadOperand(asmType, lhs, T0, T0);
        GeneralPurposeRegister rhsReg = (GeneralPurposeRegister) loadOperand(asmType, rhs, T1, T1);
        GeneralPurposeRegister dstReg = (GeneralPurposeRegister) calcDestination(dst, T0);
        emitInst(mnemonic, dstReg, lhsReg, rhsReg);
        storeToDest(asmType, dstReg, dst, T1);
        return null;
    }

    @Override
    public Void visit(ZeroExtend inst) {
        HighLevelOperand src = inst.src;
        HighLevelOperand dst = inst.dst;
        AsmType.PrimitiveType srcType = (AsmType.PrimitiveType) inst.srcType;
        AsmType.PrimitiveType dstType = (AsmType.PrimitiveType) inst.dstType;
        testUnaryHighLevelOperand(src, dst);
        testInnerType(srcType, dstType);
        if (srcType.equals(AsmType.DOUBLE) || dstType.equals(AsmType.DOUBLE)) {
            throw new IllegalArgumentException("ZeroExtend only supports integer types");
        }

        GeneralPurposeRegister dstReg = (GeneralPurposeRegister) calcDestination(dst, T0);

        // 当源操作数宽度小于等于目标操作数宽度时，将源操作数的高位用零填充，随后存放到目标操作数
        // 当源操作数宽度大于目标操作数宽度时，将源操作数截断到目标操作数的宽度后，进行零拓展，随后存放到目标操作数
        if (src instanceof LA64Register || src instanceof Immediate) {
            LA64Register srcReg = loadOperand(null, src, T0, null);
            // 源操作数直接在寄存器中
            if (srcType.size() <= dstType.size()) {
                // 检查是否自然零拓展
                if (srcType.equals(AsmType.UBYTE)) {
                    // 自然零拓展，直接写回即可
                    storeToDest(dstType, srcReg, dst, T1);
                    return null;
                }
            }
            // 否则人为进行零拓展，或截断后零拓展
            // 二者形式相同
            long msbd = Math.min(srcType.size(), dstType.size()) * 8 - 1;
            // bstrpick.d rd, rj, msbd, lsbd
            emitInst("bstrpick.d", dstReg, srcReg, new LA64AsmImmOperand(msbd), LA64AsmImmOperand.ZERO);
            storeToDest(dstType, dstReg, dst, T1);
        } else {
            // 在内存中，直接在取出时进行零拓展，随后写回
            long minSize = Math.min(srcType.size(), dstType.size());
            AsmType loadType = switch ((int) minSize) {
                case 1 -> AsmType.UBYTE;
                case 4 -> AsmType.UWORD;
                case 8 -> AsmType.DWORD;
                default -> throw new IllegalArgumentException("Control should not reach here");
            };
            loadOperand(loadType, src, dstReg, T0);
            storeToDest(dstType, dstReg, dst, T1);
        }
        return null;
    }

    @Override
    public Void visit(SignExtend inst) {
        HighLevelOperand src = inst.src;
        HighLevelOperand dst = inst.dst;
        AsmType.PrimitiveType srcType = (AsmType.PrimitiveType) inst.srcType;
        AsmType.PrimitiveType dstType = (AsmType.PrimitiveType) inst.dstType;
        testUnaryHighLevelOperand(src, dst);
        testInnerType(srcType, dstType);
        if (srcType.equals(AsmType.DOUBLE) || dstType.equals(AsmType.DOUBLE)) {
            throw new IllegalArgumentException("SignExtend only supports integer types");
        }

        GeneralPurposeRegister dstReg = (GeneralPurposeRegister) calcDestination(dst, T0);

        // 当源操作数宽度小于等于目标操作数宽度时，将源操作数的符号位扩展，随后存放到目标操作数
        // 当源操作数宽度大于目标操作数宽度时，将源操作数截断到目标操作数的宽度后，进行符号拓展，随后存放到目标操作数
        if (src instanceof LA64Register || src instanceof Immediate) {
            LA64Register srcReg = loadOperand(null, src, T0, null);
            // 源操作数直接在寄存器中
            if (srcType.size() <= dstType.size()) {
                // 检查是否自然拓展
                if (!srcType.equals(AsmType.UBYTE)) {
                    // 自然拓展，直接将源寄存器的值移动到目标寄存器即可
                    storeToDest(dstType, srcReg, dst, T1);
                    return null;
                }
            }
            // 否则人为进行符号拓展，或截断后符号拓展
            // 二者形式相同
            long minSize = Math.min(srcType.size(), dstType.size());
            switch ((int) minSize) {
                case 1 -> emitInst("ext.w.b", dstReg, srcReg);
                case 4 -> emitInst("add.w", dstReg, srcReg, ZERO);
                default -> throw new IllegalArgumentException("Control should not reach here");
            }
            storeToDest(dstType, dstReg, dst, T1);
        } else {
            // 在内存中，直接在取出时进行符号拓展，随后写回
            long minSize = Math.min(srcType.size(), dstType.size());
            AsmType loadType = switch ((int) minSize) {
                case 1 -> AsmType.BYTE;
                case 4 -> AsmType.WORD;
                case 8 -> AsmType.DWORD;
                default -> throw new IllegalArgumentException("Control should not reach here");
            };
            loadOperand(loadType, src, dstReg, T0);
            storeToDest(dstType, dstReg, dst, T1);
        }
        return null;
    }

    @Override
    public Void visit(DoubleFromInt inst) {
        HighLevelOperand src = inst.src;
        HighLevelOperand dst = inst.dst;
        AsmType srcAsmType = inst.srcAsmType;
        testUnaryHighLevelOperand(src, dst);
        testByteType(srcAsmType);
        testInnerType(srcAsmType);

        String mnemonic = srcAsmType.equals(AsmType.WORD) ? "ffint.d.w" : "ffint.d.l";
        GeneralPurposeRegister srcReg = (GeneralPurposeRegister) loadOperand(srcAsmType, src, T0, T0);
        lowerRegMove(srcReg, FT0);
        FloatingPointRegister dstReg = (FloatingPointRegister) calcDestination(dst, FT0);
        emitInst(mnemonic, dstReg, FT0);
        storeToDest(AsmType.DOUBLE, dstReg, dst, T0);
        return null;
    }

    @Override
    public Void visit(DoubleToIntRoundZero inst) {
        HighLevelOperand src = inst.src;
        HighLevelOperand dst = inst.dst;
        AsmType dstAsmType = inst.dstAsmType;
        testUnaryHighLevelOperand(src, dst);
        testByteType(dstAsmType);
        testInnerType(dstAsmType);

        String mnemonic = dstAsmType.equals(AsmType.WORD) ? "ftintrz.w.d" : "ftintrz.l.d";
        FloatingPointRegister srcReg = (FloatingPointRegister) loadOperand(AsmType.DOUBLE, src, FT0, T0);
        emitInst(mnemonic, FT0, srcReg);
        GeneralPurposeRegister dstReg = (GeneralPurposeRegister) calcDestination(dst, T0);
        lowerRegMove(FT0, dstReg);
        storeToDest(dstAsmType, dstReg, dst, T1);
        return null;
    }

    @Override
    public Void visit(CompareDouble inst) {
        LA64FloatCompareCondition cond = inst.cond;
        HighLevelOperand lhs = inst.lhs;
        HighLevelOperand rhs = inst.rhs;
        ConditionFlagRegister cc = inst.cc;
        testBinaryHighLevelOperand(lhs, rhs, null);

        String mnemonic = "fcmp." + cond.mnemonic() + ".d";
        FloatingPointRegister lhsReg = (FloatingPointRegister) loadOperand(AsmType.DOUBLE, lhs, FT0, T0);
        FloatingPointRegister rhsReg = (FloatingPointRegister) loadOperand(AsmType.DOUBLE, rhs, FT1, T0);
        emitInst(mnemonic, cc, lhsReg, rhsReg);
        return null;
    }

    @Override
    public Void visit(GetCC inst) {
        HighLevelOperand dst = inst.dst;
        ConditionFlagRegister cc = inst.cc;
        testUnaryHighLevelOperand(null, dst);

        GeneralPurposeRegister dstReg = (GeneralPurposeRegister) calcDestination(dst, T0);
        emitInst("movcf2gr", dstReg, cc);
        storeToDest(AsmType.WORD, dstReg, dst, T1);
        return null;
    }

    @Override
    public Void visit(BranchIfCCZero inst) {
        emitInst("bceqz", inst.cc, new LA64AsmSymOperand(".L" + inst.target));
        return null;
    }

    @Override
    public Void visit(BranchIfCCNotZero inst) {
        emitInst("bcnez", inst.cc, new LA64AsmSymOperand(".L" + inst.target));
        return null;
    }

    @Override
    public Void visit(DoubleNegate inst) {
        HighLevelOperand src = inst.src;
        HighLevelOperand dst = inst.dst;
        testUnaryHighLevelOperand(src, dst);

        FloatingPointRegister srcReg = (FloatingPointRegister) loadOperand(AsmType.DOUBLE, src, FT0, T0);
        FloatingPointRegister dstReg = (FloatingPointRegister) calcDestination(dst, FT0);
        emitInst("fneg.d", dstReg, srcReg);
        storeToDest(AsmType.DOUBLE, dstReg, dst, T0);
        return null;
    }

    @Override
    public Void visit(LoadAddress inst) {
        HighLevelOperand obj = inst.obj;
        HighLevelOperand dst = inst.dst;
        testUnaryHighLevelOperand(obj, dst);

        GeneralPurposeRegister dstReg = (GeneralPurposeRegister) calcDestination(dst, T0);

        // 获取操作数的地址
        if (obj instanceof LA64Register) {
            throw new IllegalArgumentException("Cannot take register address");
        }
        if (obj instanceof Immediate) {
            throw new IllegalArgumentException("Cannot take immediate address");
        }

        if (obj instanceof Memory objMem) {
            // 直接进行加法
            visit(new Binary(Binary.Operator.ADD, AsmType.DWORD, objMem.gpr(), new Immediate(objMem.offset()), dstReg));
        } else if (obj instanceof Data objData) {
            // 通过宏指令获取
            boolean isConstant = ((BackendSymbolTable.ObjectEntry) backendSymbolTable.get(objData.name())).isConstant();
            var symOp = new LA64AsmSymOperand(isConstant ? ".L" + objData.name() : objData.name(), objData.offset());
            emitInst("la.pcrel", dstReg, symOp);
        } else {
            throw new IllegalStateException("Unknown object HighLevelOperand type: " + obj);
        }

        storeToDest(AsmType.DWORD, dstReg, dst, T1);
        return null;
    }

    @Override
    public Void visit(Load inst) {
        testInnerType(inst.dstAsmType);
        lowerLoad(inst.dstAsmType, inst.ptr, inst.offset, inst.dst);
        return null;
    }

    private void lowerLoad(AsmType dstAsmType, HighLevelOperand ptr, HighLevelOperand offset, HighLevelOperand dst) {
        testBinaryHighLevelOperand(ptr, offset, dst);
        // 唯二允许内部类型处

        GeneralPurposeRegister ptrReg = (GeneralPurposeRegister) loadOperand(AsmType.DWORD, ptr, T0, T0);
        LA64Register dstReg = calcDestination(dst, dstAsmType.equals(AsmType.DOUBLE) ? FT0 : T0);

        if (offset instanceof Immediate imm && BitMath.isSi12(imm.value())) {
            loadMem(dstAsmType, dstReg, ptrReg, (int) imm.value());
        } else {
            GeneralPurposeRegister idxReg = (GeneralPurposeRegister) loadOperand(AsmType.DWORD, offset, T1, T1);
            loadMem(dstAsmType, dstReg, ptrReg, idxReg);
        }

        storeToDest(dstAsmType, dstReg, dst, T1);
    }

    @Override
    public Void visit(Store inst) {
        testInnerType(inst.srcAsmType);
        lowerStore(inst.srcAsmType, inst.src, inst.ptr, inst.offset);
        return null;
    }

    private void lowerStore(AsmType srcAsmType, HighLevelOperand src, HighLevelOperand ptr, HighLevelOperand offset) {
        testTernaryHighLevelOperand(src, ptr, offset, null);
        // 唯二允许内部类型处

        GeneralPurposeRegister ptrReg = (GeneralPurposeRegister) loadOperand(AsmType.DWORD, ptr, T0, T0);
        LA64Register srcReg = loadOperand(srcAsmType, src, srcAsmType.equals(AsmType.DOUBLE) ? FT1 : T1, T1);

        if (offset instanceof Immediate imm && BitMath.isSi12(imm.value())) {
            storeMem(srcAsmType, srcReg, ptrReg, (int) imm.value());
        } else {
            GeneralPurposeRegister idxReg = (GeneralPurposeRegister) loadOperand(AsmType.DWORD, offset, T2, T2);
            storeMem(srcAsmType, srcReg, ptrReg, idxReg);
        }
    }

    @Override
    public Void visit(AddLeftShift inst) {
        // alsl.d rd, rj, rk, sa2
        // rd = (rj << (sa2 + 1)) + rk

        HighLevelOperand base = inst.base;
        HighLevelOperand index = inst.index;
        int sa2 = inst.shiftAmount - 1;
        HighLevelOperand dst = inst.dst;
        testBinaryHighLevelOperand(base, index, dst);

        GeneralPurposeRegister baseReg = (GeneralPurposeRegister) loadOperand(AsmType.DWORD, base, T0, T0);
        GeneralPurposeRegister indexReg = (GeneralPurposeRegister) loadOperand(AsmType.DWORD, index, T1, T1);
        GeneralPurposeRegister dstReg = (GeneralPurposeRegister) calcDestination(dst, T0);
        emitInst("alsl.d", dstReg, indexReg, baseReg, new LA64AsmImmOperand(sa2));
        storeToDest(AsmType.DWORD, dstReg, dst, T1);
        return null;
    }

    @Override
    public Void visit(CallIndirect inst) {
        HighLevelOperand funcPtr = inst.funcPtr;
        testUnaryHighLevelOperand(funcPtr, null);

        GeneralPurposeRegister ptrReg = (GeneralPurposeRegister) loadOperand(AsmType.DWORD, funcPtr, T0, T0);
        emitInst("jirl", RA, ptrReg, LA64AsmImmOperand.ZERO);
        return null;
    }

    @Override
    public Void visit(CopyByteArray inst) {
        Memory dst = (Memory) inst.dst;
        if (dst.gpr() != SP && dst.gpr() != FP) {
            throw new UnsupportedOperationException(
                "CopyByteArray only supports stack or frame pointer as destination");
        }
        // 认为 sp 和 fp 都是 16 字节对齐的，进行可能的合并访存
        long i = dst.offset(), j = 0, bound = i + inst.data.length;
        while (i < bound) {
            for (int align = 8; align >= 1; align /= 2) {
                if (i % align == 0 && bound - i >= align) {
                    // 对齐访存
                    long val = 0;
                    for (int k = 0; k < align; k++) {
                        val |= ((long) inst.data[(int) j + k] & 0xFF) << (k * 8);
                    }
                    AsmType asmType = switch (align) {
                        case 1 -> AsmType.BYTE;
                        case 2 -> AsmType.HALF;
                        case 4 -> AsmType.WORD;
                        case 8 -> AsmType.DWORD;
                        default -> throw new IllegalStateException("Unexpected alignment: " + align);
                    };
                    lowerStore(asmType, new Immediate(val), dst.gpr(), new Immediate(i));
                    i += align;
                    j += align;
                    break;
                }
            }
        }
        return null;
    }

    private static void testByteType(AsmType... asmType) {
        for (AsmType type : asmType) {
            if (type.equals(AsmType.BYTE) || type.equals(AsmType.UBYTE)) {
                throw new UnsupportedOperationException("Should be promoted to WORD before");
            }
        }
    }

    private static void testInnerType(AsmType... asmType) {
        for (AsmType type : asmType) {
            if (type.equals(AsmType.HALF) || type.equals(AsmType.UHALF) || type.equals(AsmType.UWORD)) {
                throw new UnsupportedOperationException("Should only used in inner instructions");
            }
        }
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

    private static void testTernaryHighLevelOperand(
        HighLevelOperand src1, HighLevelOperand src2, HighLevelOperand src3, HighLevelOperand dst) {

        if (dst instanceof Immediate) {
            throw new UnsupportedOperationException("Cannot store result in an immediate");
        }
        if (src1 instanceof Pseudo || src2 instanceof Pseudo || src3 instanceof Pseudo || dst instanceof Pseudo) {
            throw new UnsupportedOperationException("Cannot operate on pseudo register, should be replaced earlier");
        }
    }

    /**
     * 加载操作数至寄存器
     *
     * @param asmType  操作数类型
     * @param toLoad   操作数
     * @param fallback 若操作数为内存，用于存放操作数位置的寄存器
     * @param tmp      临时寄存器，加载浮点立即数或全局符号时使用
     * @return 最终存放操作数的寄存器
     */
    private LA64Register loadOperand(
        AsmType asmType, HighLevelOperand toLoad, LA64Register fallback, GeneralPurposeRegister tmp) {

        // 加载操作数至寄存器
        if (toLoad instanceof LA64Register reg) {
            // 本身就在寄存器，直接使用
            return reg;
        }
        if (toLoad instanceof Memory mem) {
            // 在内存，加载到 fallback 寄存器
            lowerLoad(asmType, mem.gpr(), new Immediate(mem.offset()), (HighLevelOperand) fallback);
            return fallback;
        }
        if (toLoad instanceof Immediate immediate) {
            // 立即数，加载到 fallback 寄存器
            loadImm(fallback, immediate, tmp);
            return fallback;
        }
        if (toLoad instanceof Data data) {
            // 全局符号，加载到 fallback 寄存器
            loadData(asmType, fallback, data, tmp);
            return fallback;
        }
        throw new UnsupportedOperationException("Unsupported operand type: " + toLoad.getClass().getSimpleName());
    }

    /**
     * 计算将结果存放至 {@code dst} 需要的寄存器
     *
     * @param dst      结果存放位置
     * @param fallback 如果需要存储，存放到该寄存器
     * @return 计算结果，实际存放结果的寄存器
     */
    private static LA64Register calcDestination(HighLevelOperand dst, LA64Register fallback) {
        // 计算结果的存放地点
        if (dst instanceof LA64Register reg) {
            // 存放至寄存器，直接赋值
            return reg;
        }
        if (dst instanceof Memory || dst instanceof Data) {
            // 存放至内存，得先存放到 fallback
            return fallback;
        }
        throw new UnsupportedOperationException("Unsupported destination type: " + dst.getClass().getSimpleName());
    }

    /**
     * 将结果存放至目标地点
     *
     * @param asmType 结果值类型
     * @param val     计算结果
     * @param dest    结果需要存放的位置
     * @param tmp     临时寄存器，当存放至全局符号时使用
     */
    private void storeToDest(AsmType asmType, LA64Register val, HighLevelOperand dest, GeneralPurposeRegister tmp) {
        // 将先前存放在寄存器的结果写回目标地点
        if (dest instanceof LA64Register reg) {
            lowerRegMove(val, reg);
        } else if (dest instanceof Memory dstMem) {
            lowerStore(asmType, (HighLevelOperand) val, dstMem.gpr(), new Immediate(dstMem.offset()));
        } else if (dest instanceof Data dstData) {
            storeData(asmType, val, dstData, tmp);
        }
    }

    /**
     * 加载立即数至寄存器
     *
     * @param dst 目标寄存器
     * @param imm 加载的立即数
     * @param tmp 临时寄存器，当加载立即数至浮点寄存器时使用
     */
    private void loadImm(LA64Register dst, Immediate imm, GeneralPurposeRegister tmp) {
        if (dst instanceof GeneralPurposeRegister dstGpr) {
            emitInst("li.d", dstGpr, new LA64AsmImmOperand(imm));
        } else if (dst instanceof FloatingPointRegister dstFpr) {
            emitInst("li.d", tmp, new LA64AsmImmOperand(imm));
            lowerRegMove(tmp, dstFpr);
        } else {
            throw new UnsupportedOperationException("Invalid register: " + dst);
        }
    }

    /**
     * 加载内存值至寄存器
     *
     * @param asmType 加载值类型
     * @param dst     目标寄存器
     * @param src     基址寄存器
     * @param si12    偏移量，保证在 si12 范围内
     */
    private void loadMem(AsmType asmType, LA64Register dst, GeneralPurposeRegister src, int si12) {
        if (dst instanceof GeneralPurposeRegister dstGpr) {
            if (!(asmType instanceof AsmType.PrimitiveType t)) {
                throw new UnsupportedOperationException("Invalid asmType: " + asmType);
            }
            String mnemonic = "ld." + t.p().suffix;
            emitInst(mnemonic, dstGpr, src, new LA64AsmImmOperand(si12));
        } else if (dst instanceof FloatingPointRegister dstFpr) {
            if (asmType != AsmType.DOUBLE) {
                throw new UnsupportedOperationException("Invalid asmType: " + asmType);
            }
            emitInst("fld.d", dstFpr, src, new LA64AsmImmOperand(si12));
        } else {
            throw new UnsupportedOperationException("Invalid register: " + dst);
        }
    }

    /**
     * 加载内存值至寄存器
     *
     * @param asmType 加载值类型
     * @param dst     目标寄存器
     * @param src     基址寄存器
     * @param idx     偏移量寄存器
     */
    private void loadMem(AsmType asmType, LA64Register dst, GeneralPurposeRegister src, GeneralPurposeRegister idx) {
        if (dst instanceof GeneralPurposeRegister dstGpr) {
            if (!(asmType instanceof AsmType.PrimitiveType t)) {
                throw new UnsupportedOperationException("Invalid asmType: " + asmType);
            }
            String mnemonic = "ldx." + t.p().suffix;
            emitInst(mnemonic, dstGpr, src, idx);
        } else if (dst instanceof FloatingPointRegister dstFpr) {
            if (asmType != AsmType.DOUBLE) {
                throw new UnsupportedOperationException("Invalid asmType: " + asmType);
            }
            emitInst("fldx.d", dstFpr, src, idx);
        } else {
            throw new UnsupportedOperationException("Invalid register: " + dst);
        }
    }

    /**
     * 将寄存器值写入内存位置
     *
     * @param asmType 写入值类型
     * @param val     写入值所在寄存器，保证写入寄存器不变
     * @param dst     基址寄存器
     * @param si12    偏移量，保证在 si12 范围内
     */
    private void storeMem(AsmType asmType, LA64Register val, GeneralPurposeRegister dst, int si12) {
        if (val instanceof GeneralPurposeRegister srcGpr) {
            if (!(asmType instanceof AsmType.PrimitiveType t)) {
                throw new UnsupportedOperationException("Invalid asmType: " + asmType);
            }
            String mnemonic = "st." + t.p().suffix.charAt(0);
            emitInst(mnemonic, srcGpr, dst, new LA64AsmImmOperand(si12));
        } else if (val instanceof FloatingPointRegister srcFpr) {
            if (asmType != AsmType.DOUBLE) {
                throw new UnsupportedOperationException("Invalid asmType: " + asmType);
            }
            emitInst("fst.d", srcFpr, dst, new LA64AsmImmOperand(si12));
        } else {
            throw new UnsupportedOperationException("Invalid register: " + val);
        }
    }

    /**
     * 将寄存器值写入内存位置
     *
     * @param asmType 写入值类型
     * @param val     写入值所在寄存器，保证写入寄存器不变
     * @param dst     基址寄存器
     * @param idx     偏移量寄存器
     */
    private void storeMem(AsmType asmType, LA64Register val, GeneralPurposeRegister dst, GeneralPurposeRegister idx) {
        if (val instanceof GeneralPurposeRegister srcGpr) {
            if (!(asmType instanceof AsmType.PrimitiveType t)) {
                throw new UnsupportedOperationException("Invalid asmType: " + asmType);
            }
            String mnemonic = "stx." + t.p().suffix.charAt(0);
            emitInst(mnemonic, srcGpr, dst, idx);
        } else if (val instanceof FloatingPointRegister srcFpr) {
            if (asmType != AsmType.DOUBLE) {
                throw new UnsupportedOperationException("Invalid asmType: " + asmType);
            }
            emitInst("fstx.d", srcFpr, dst, idx);
        } else {
            throw new UnsupportedOperationException("Invalid register: " + val);
        }
    }

    /**
     * 加载全局符号所指向内存中的值至寄存器
     *
     * @param asmType 加载值类型
     * @param dst     目标寄存器
     * @param sym     全局符号
     * @param tmpAddr 临时寄存器，用于地址计算，调用者保证在调用前后该寄存器值不被使用
     */
    private void loadData(AsmType asmType, LA64Register dst, Data sym, GeneralPurposeRegister tmpAddr) {
        // 先加载符号地址
        boolean isConstant = ((BackendSymbolTable.ObjectEntry) backendSymbolTable.get(sym.name())).isConstant();
        var symOp = new LA64AsmSymOperand(isConstant ? ".L" + sym.name() : sym.name(), sym.offset());
        emitInst("la.pcrel", tmpAddr, symOp);
        // 再对符号地址访存
        if (dst instanceof GeneralPurposeRegister dstGpr) {
            if (!(asmType instanceof AsmType.PrimitiveType t)) {
                throw new UnsupportedOperationException("Invalid asmType: " + asmType);
            }
            String mnemonic = "ld." + t.p().suffix;
            emitInst(mnemonic, dstGpr, tmpAddr, LA64AsmImmOperand.ZERO);
        } else if (dst instanceof FloatingPointRegister dstFpr) {
            if (asmType != AsmType.DOUBLE) {
                throw new UnsupportedOperationException("Invalid asmType: " + asmType);
            }
            emitInst("fld.d", dstFpr, tmpAddr, LA64AsmImmOperand.ZERO);
        } else {
            throw new UnsupportedOperationException("Invalid register: " + dst);
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
    private void storeData(AsmType asmType, LA64Register val, Data sym, GeneralPurposeRegister tmpAddr) {
        // 先加载符号地址
        boolean isConstant = ((BackendSymbolTable.ObjectEntry) backendSymbolTable.get(sym.name())).isConstant();
        var symOp = new LA64AsmSymOperand(isConstant ? ".L" + sym.name() : sym.name(), sym.offset());
        emitInst("la.pcrel", tmpAddr, symOp);
        // 再将值写入符号地址
        if (val instanceof GeneralPurposeRegister srcGpr) {
            if (!(asmType instanceof AsmType.PrimitiveType t)) {
                throw new UnsupportedOperationException("Invalid asmType: " + asmType);
            }
            String mnemonic = "st." + t.p().suffix.charAt(0);
            emitInst(mnemonic, srcGpr, tmpAddr, LA64AsmImmOperand.ZERO);
        } else if (val instanceof FloatingPointRegister srcFpr) {
            if (asmType != AsmType.DOUBLE) {
                throw new UnsupportedOperationException("Invalid asmType: " + asmType);
            }
            emitInst("fst.d", srcFpr, tmpAddr, LA64AsmImmOperand.ZERO);
        } else {
            throw new UnsupportedOperationException("Invalid register: " + val);
        }
    }
}
