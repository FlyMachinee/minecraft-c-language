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
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.IntInit;

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
            emitDir(staticVar.init instanceof IntInit ? "word" : "dword", new LA64DirectiveNumArg(initValue));
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
        HighLevelOperand dst = inst.dst;
        HighLevelOperand src = inst.src;
        panicIfMultiple(dst.asmType());

        if (dst instanceof Immediate) {
            throw new UnsupportedOperationException("Cannot move to an immediate");
        }
        if (src instanceof Pseudo || dst instanceof Pseudo) {
            throw new UnsupportedOperationException(
                "Cannot move from/to pseudo register, should be replaced earlier");
        }

        // 特殊情况优化
        if (dst instanceof Reg dstReg) {
            // -> 寄存器
            if (src instanceof Immediate srcImm) {
                // 立即数 -> 寄存器
                loadImm(dstReg, srcImm);
            } else if (src instanceof Reg srcReg) {
                // 寄存器 -> 寄存器
                lowerRegMove(srcReg, dstReg);
            } else if (src instanceof Stack srcStack) {
                // 栈 -> 寄存器
                loadStack(dstReg, srcStack);
            } else if (src instanceof Data srcData) {
                // 全局符号 -> 寄存器
                loadData(dstReg, srcData);
            }
            return null;
        }
        if (src instanceof Reg srcReg) {
            // 寄存器 ->
            if (dst instanceof Stack dstStack) {
                // 寄存器 -> 栈
                storeStack(srcReg, dstStack, T0);
            } else if (dst instanceof Data dstData) {
                // 寄存器 -> 全局符号
                storeData(srcReg, dstData, T0, T1);
            }
            return null;
        }

        // 其他情况：将源加载到临时寄存器 T0，再存储到目标
        // 此时 src 为 Immediate/Stack/Data，dst 为 Stack/Data
        Reg tmp = new Reg(T0, src.asmType());
        AsmType loadResultType;
        if (src instanceof Immediate srcImm) {
            loadResultType = loadImm(tmp, srcImm);
        } else if (src instanceof Stack srcStack) {
            loadResultType = loadStack(tmp, srcStack);
        } else if (src instanceof Data srcData) {
            loadResultType = loadData(tmp, srcData);
        } else {
            throw new UnsupportedOperationException("Unsupported source type: " + src.getClass().getSimpleName());
        }
        tmp = new Reg(tmp.reg(), loadResultType);

        if (dst instanceof Stack dstStack) {
            storeStack(tmp, dstStack, T1);
        } else if (dst instanceof Data dstData) {
            storeData(tmp, dstData, T0, T1);
        } else {
            throw new UnsupportedOperationException("Unsupported destination type: " + dst.getClass().getSimpleName());
        }
        return null;
    }

    private void lowerRegMove(Reg srcReg, Reg dstReg) {
        panicIfMultiple(dstReg.asmType());

        if (srcReg.reg() == dstReg.reg()) {
            // W/D -> W none
            // W/D -> D none
            // D -> D none
            // W -> W add.w
            // W -> D add.w
            // D -> W add.w
            if (!srcReg.asmType().isSpecific()) {
                return;
            }
        }

        // D -> D move
        // W -> D add.w
        // D -> W add.w
        // W -> W add.w
        if (srcReg.asmType().isDWord() && dstReg.asmType().isDWord()) {
            if (srcReg.reg() != dstReg.reg()) {
                emitInst("move", dstReg.reg(), srcReg.reg());
            }
        } else {
            emitInst("add.w", dstReg.reg(), srcReg.reg(), ZERO);
        }
    }

    @Override
    public Void visitRet(Ret inst) {
        generateEpilogue(functionContext);
        return null;
    }

    @Override
    public Void visitUnary(Unary unary) {
        UnaryOperator op = unary.op;
        HighLevelOperand src = unary.src;
        HighLevelOperand dst = unary.dst;
        testUnaryHighLevelOperand(src, dst);

        String opName = switch (op) {
            case NEGATE -> src.asmType().isWord() ? "sub.w" : "sub.d";
            case COMPLEMENT -> "nor";
            case NOT -> "sltui"; // 使用 sltui rd, rj, 1 来计算
        };

        // 先加载操作数到寄存器中
        Reg operand = loadOperand(src, T0);

        // 计算结果的存放地点
        GeneralPurposeRegister dstReg = calcDestination(dst, T0);

        // 计算结果
        AsmType resultAsmType;
        if (op == UnaryOperator.NOT) {
            // sltui rd, rj, 1
            emitInst(opName, dstReg, operand.reg(), new LA64AsmImmOperand(1));
            // destResult = new Pair<>(new Reg(dstReg.reg(), AsmType.WORD_OR_DWORD), destResult.getSecond());
            resultAsmType = AsmType.WORD_OR_DWORD;
        } else {
            // sub.w(d)/nor rd, zero, rk
            emitInst(opName, dstReg, ZERO, operand.reg());
            resultAsmType = op == UnaryOperator.COMPLEMENT ? src.asmType()
                : src.asmType().isWord() ? AsmType.WORD_OR_DWORD : src.asmType();
        }

        storeToDest(new Reg(dstReg, resultAsmType), dst, T0, T1);
        return null;
    }

    @Override
    public Void visitBinary(Binary binary) {
        BinaryOperator op = binary.op;
        HighLevelOperand lhs = binary.lhs;
        HighLevelOperand rhs = binary.rhs;
        HighLevelOperand dst = binary.dst;
        testBinaryHighLevelOperand(lhs, rhs, dst);

        // 检查类型是否合法
        switch (op) {
            case LEFT_SHIFT, RIGHT_SHIFT -> { }
            case LOGICAL_AND, LOGICAL_OR ->
                throw new UnsupportedOperationException("Logical operators should be handled in frontend");
            default -> {
                if (!lhs.asmType().equals(rhs.asmType())) {
                    throw new UnsupportedOperationException(
                        "Mismatched operand types: " + lhs.asmType() + " and " + rhs.asmType());
                }
            }
        }

        // 特殊情况检查
        // 立即数加法
        if (op == BinaryOperator.ADD && (lhs instanceof Immediate || rhs instanceof Immediate)) {
            // 检查立即数是否可用 si12 表示，如果可以，生成 addi.w(d) 指令，否则使用默认处理
            if (lhs instanceof Immediate imm && BitMath.isSi12(imm.value())) {
                // 立即数在左操作数
                visitAddSi12(new AddSi12(rhs, (int) imm.value(), dst));
                return null;
            } else if (rhs instanceof Immediate imm && BitMath.isSi12(imm.value())) {
                // 立即数在右操作数
                visitAddSi12(new AddSi12(lhs, (int) imm.value(), dst));
                return null;
            }
        }
        // 立即数减法，处理减立即数的情况
        if (op == BinaryOperator.SUBTRACT && rhs instanceof Immediate imm && BitMath.isSi12(-imm.value())) {
            visitAddSi12(new AddSi12(lhs, (int) -imm.value(), dst));
            return null;
        }
        // 立即数右移或左移，处理右操作数为立即数的情况
        if ((op == BinaryOperator.LEFT_SHIFT || op == BinaryOperator.RIGHT_SHIFT) && rhs instanceof Immediate imm) {
            String opName = lhs.asmType().isWord() ?
                (op == BinaryOperator.LEFT_SHIFT ? "slli.w" : "srai.w") :
                (op == BinaryOperator.LEFT_SHIFT ? "slli.d" : "srai.d");
            Reg srcReg = loadOperand(lhs, T0);
            GeneralPurposeRegister dstReg = calcDestination(dst, T0);
            // slli.w(d)/srai.w(d) rd, rj, ui5(ui6)
            emitInst(
                opName, dstReg, srcReg.reg(),
                new LA64AsmImmOperand(BitMath.extractBits((int) imm.value(), lhs.asmType().isWord() ? 5 : 6)));
            storeToDest(new Reg(dstReg, lhs.asmType().isWord() ? AsmType.WORD_OR_DWORD : lhs.asmType()), dst, T0, T1);
            return null;
        }
        // 立即数位运算
        if ((op == BinaryOperator.BITWISE_AND || op == BinaryOperator.BITWISE_OR || op == BinaryOperator.BITWISE_XOR) &&
            ((lhs instanceof Immediate || rhs instanceof Immediate))) {
            // 检查立即数是否可用 ui12 表示，如果可以，生成 andi/ori/xori 指令，否则使用默认处理

            if (lhs instanceof Immediate) {
                // 把立即数放在右操作数
                lhs = binary.rhs;
                rhs = binary.lhs;
            }

            // andi/ori/xori rd, rj, ui12
            Immediate imm = (Immediate) rhs;
            if (BitMath.isUi12(imm.value())) {
                String opName = switch (op) {
                    case BITWISE_AND -> "andi";
                    case BITWISE_OR -> "ori";
                    case BITWISE_XOR -> "xori";
                    default -> throw new IllegalStateException("Unexpected operator: " + op);
                };
                Reg srcReg = loadOperand(lhs, T0);
                GeneralPurposeRegister dstReg = calcDestination(dst, T0);
                emitInst(opName, dstReg, srcReg.reg(), new LA64AsmImmOperand(imm));
                storeToDest(new Reg(dstReg, lhs.asmType()), dst, T0, T1);
                return null;
            }
        }

        // 交换左、右操作数，全部转换为小于或小于等于
        switch (op) {
            case GREATER_THAN -> {
                op = BinaryOperator.LESS_THAN;
                lhs = binary.rhs;
                rhs = binary.lhs;
            }
            case GREATER_OR_EQUAL -> {
                op = BinaryOperator.LESS_OR_EQUAL;
                lhs = binary.rhs;
                rhs = binary.lhs;
            }
        }

        // 小于立即数
        if (op == BinaryOperator.LESS_THAN && rhs instanceof Immediate imm && BitMath.isSi12(imm.value())) {
            Reg srcReg = loadOperand(lhs, T0);
            GeneralPurposeRegister dstReg = calcDestination(dst, T0);
            // slti rd, rj, si12
            emitInst("slti", dstReg, srcReg.reg(), new LA64AsmImmOperand((int) imm.value()));
            storeToDest(new Reg(dstReg, AsmType.WORD_OR_DWORD), dst, T0, T1);
            return null;
        }

        // 通用处理
        boolean isWordArithmetic = lhs.asmType().isWord();
        String suffix = isWordArithmetic ? ".w" : ".d";
        String opName = switch (op) {
            case ADD -> "add" + suffix;
            case SUBTRACT -> "sub" + suffix;
            case MULTIPLY -> "mul" + suffix;
            case DIVIDE -> "div" + suffix;
            case MODULO -> "mod" + suffix;
            case LEFT_SHIFT -> "sll" + suffix;
            case RIGHT_SHIFT -> "sra" + suffix; // 算术右移
            case BITWISE_AND -> "and";
            case BITWISE_OR -> "or";
            case BITWISE_XOR -> "xor";
            case LESS_THAN -> "slt";
            case EQUAL -> "seq"; // 宏指令
            case NOT_EQUAL -> "sne"; // 宏指令
            case LESS_OR_EQUAL -> "sle"; // 宏指令
            default -> throw new IllegalStateException("Unexpected operator: " + op);
        };

        // 加载左操作数至寄存器
        Reg lhsReg = loadOperand(lhs, T0);

        // 加载右操作数至寄存器
        Reg rhsReg = loadOperand(rhs, T1);

        // 计算结果的存放地点
        GeneralPurposeRegister dstReg = calcDestination(dst, T0);

        // 计算结果
        // op rd, rj, rk
        AsmType resultAsmType;
        emitInst(opName, dstReg, lhsReg.reg(), rhsReg.reg());
        switch (op) {
            case ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO, LEFT_SHIFT, RIGHT_SHIFT ->
                resultAsmType = lhs.asmType().isWord() ? AsmType.WORD_OR_DWORD : lhs.asmType();
            case BITWISE_AND, BITWISE_OR, BITWISE_XOR -> resultAsmType = lhs.asmType();
            case EQUAL, NOT_EQUAL, LESS_OR_EQUAL, LESS_THAN -> resultAsmType = AsmType.WORD_OR_DWORD;
            default -> throw new IllegalStateException("Unexpected operator: " + op);
        }

        storeToDest(new Reg(dstReg, resultAsmType), dst, T0, T1);
        return null;
    }

    @Override
    public Void visitAddSi12(AddSi12 addSi12) {
        HighLevelOperand src = addSi12.src;
        int si12 = addSi12.si12;
        HighLevelOperand dst = addSi12.dst;
        testUnaryHighLevelOperand(src, dst);

        if (!BitMath.isSi12(si12)) {
            throw new UnsupportedOperationException("The immediate value must be a si12");
        }

        // 加载操作数至寄存器
        Reg srcReg = loadOperand(src, T0);

        // 计算结果的存放地点
        GeneralPurposeRegister dstReg = calcDestination(dst, T0);

        // 计算结果
        // addi.w(d) rd, rj, si12
        emitInst(
            src.asmType().isWord() ? "addi.w" : "addi.d", dstReg, srcReg.reg(),
            new LA64AsmImmOperand(si12));

        storeToDest(new Reg(dstReg, src.asmType().isWord() ? AsmType.WORD_OR_DWORD : src.asmType()), dst, T0, T1);
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
        String branchTarget = inst.target;
        testUnaryHighLevelOperand(cond, null);

        // 加载比较值至寄存器
        Reg reg = loadOperand(cond, T0);

        // 跳转
        // beqz rj, offs21
        emitInst("beqz", reg.reg(), new LA64AsmSymOperand(".L" + branchTarget));
        return null;
    }

    @Override
    public Void visitBranchIfNotZero(BranchIfNotZero inst) {
        HighLevelOperand cond = inst.cond;
        String branchTarget = inst.target;
        testUnaryHighLevelOperand(cond, null);

        // 加载比较值至寄存器
        Reg reg = loadOperand(cond, T0);

        // 跳转
        // bnez rj, offs21
        emitInst("bnez", reg.reg(), new LA64AsmSymOperand(".L" + branchTarget));
        return null;
    }

    @Override
    public Void visitBranchIfComparison(BranchIfComparison inst) {
        Comparison cond = inst.cond;
        HighLevelOperand lhs = inst.lhs;
        HighLevelOperand rhs = inst.rhs;
        String branchTarget = inst.target;
        testBinaryHighLevelOperand(lhs, rhs, null);

        if (!lhs.asmType().equals(rhs.asmType())) {
            throw new UnsupportedOperationException(
                "Mismatched operand types: " + lhs.asmType() + " and " + rhs.asmType());
        }

        String opName = switch (cond) {
            case EQUAL -> "beq";
            case NOT_EQUAL -> "bne";
            case LESS -> "blt";
            case LESS_EQUAL -> {
                lhs = inst.rhs;
                rhs = inst.lhs;
                yield "bge";
            }
            case GREATER -> {
                lhs = inst.rhs;
                rhs = inst.lhs;
                yield "blt";
            }
            case GREATER_EQUAL -> "bge";
        };

        // 加载左、右操作数至寄存器
        Reg lhsReg = loadOperand(lhs, T0);
        Reg rhsReg = loadOperand(rhs, T1);

        // 跳转
        // beq/bne/blt/bge rj, rd, offs16
        emitInst(opName, lhsReg.reg(), rhsReg.reg(), new LA64AsmSymOperand(".L" + branchTarget));
        return null;
    }

    @Override
    public Void visitCall(Call inst) {
        emitInst("bl", new LA64AsmSymOperand(inst.name));
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


    private Reg loadOperand(HighLevelOperand toLoad, GeneralPurposeRegister fallback) {
        // 加载操作数至寄存器
        if (toLoad instanceof Reg reg) {
            // 本身就在寄存器，直接使用
            return reg;
        }
        Reg fallbackReg = new Reg(fallback, toLoad.asmType());
        if (toLoad instanceof Stack stack) {
            // 在内存，加载到 fallback 寄存器
            return new Reg(fallback, loadStack(fallbackReg, stack));
        } else if (toLoad instanceof Immediate immediate) {
            // 立即数，加载到 fallback 寄存器
            return new Reg(fallback, loadImm(fallbackReg, immediate));
        } else if (toLoad instanceof Data data) {
            // 全局符号，加载到 fallback 寄存器
            return new Reg(fallback, loadData(fallbackReg, data));
        } else {
            throw new UnsupportedOperationException("Unsupported operand type: " + toLoad.getClass().getSimpleName());
        }
    }

    /**
     * 计算将结果存放至 {@code dest} 需要的寄存器
     *
     * @param dest     结果存放位置
     * @param fallback 如果需要存储，存放到该寄存器
     * @return 计算结果，实际存放结果的寄存器
     */
    private static GeneralPurposeRegister calcDestination(
        HighLevelOperand dest, GeneralPurposeRegister fallback) {

        // 计算结果的存放地点
        if (dest instanceof Reg reg) {
            // 存放至寄存器，直接赋值
            return (GeneralPurposeRegister) reg.reg();
        } else if (dest instanceof Stack || dest instanceof Data) {
            // 存放至内存，得先存放到 fallback
            return fallback;
        } else {
            throw new UnsupportedOperationException("Unsupported destination type: " + dest.getClass().getSimpleName());
        }
    }

    /**
     * 将结果存放至目标地点
     *
     * @param val     计算结果
     * @param dest    结果需要存放的位置
     * @param tmpExt  写入全局符号时使用的临时寄存器，调用者保证在调用前后该寄存器值不被使用
     * @param tmpAddr 写入全局符号时使用的临时寄存器，调用者保证在调用前后该寄存器值不被使用
     */
    private void storeToDest(
        Reg val, HighLevelOperand dest, GeneralPurposeRegister tmpExt, GeneralPurposeRegister tmpAddr) {
        // 将先前存放在寄存器的结果写回目标地点
        if (dest instanceof Reg reg) {
            lowerRegMove(val, reg);
        } else if (dest instanceof Stack dstStack) {
            storeStack(val, dstStack, tmpExt);
        } else if (dest instanceof Data dstData) {
            storeData(val, dstData, tmpExt, tmpAddr);
        }
    }

    /**
     * 加载立即数至寄存器
     *
     * @param dst 目标寄存器
     * @param imm 加载的立即数
     * @return 存放结果的寄存器的类型
     */
    private AsmType loadImm(Reg dst, Immediate imm) {
        panicIfMultiple(dst.asmType());
        emitInst(dst.asmType().isWord() ? "li.w" : "li.d", dst.reg(), new LA64AsmImmOperand(imm));
        return dst.asmType().isWord() ? AsmType.WORD_OR_DWORD : dst.asmType();
    }

    /**
     * 加载栈上值至寄存器
     *
     * @param dst 目标寄存器
     * @param src 栈位置
     * @return 存放结果的寄存器的类型
     */
    private AsmType loadStack(Reg dst, Stack src) {
        panicIfMultiple(dst.asmType());
        GeneralPurposeRegister regRef = src.fpRelative() ? FP : SP;
        // W <- W ld.w
        // D <- W ld.w
        // W <- D ld.w
        // D <- D ld.d
        boolean isLoadWord = dst.asmType().isWord() || src.asmType().isWord();
        String mnemonic = isLoadWord ? "ld.w" : "ld.d";
        emitInst(mnemonic, dst.reg(), regRef, new LA64AsmImmOperand(src.offset()));
        return isLoadWord ? AsmType.WORD_OR_DWORD : dst.asmType();
    }

    /**
     * 将寄存器值写入栈上位置
     *
     * @param val 写入值所在寄存器，保证写入寄存器不变
     * @param dst 栈位置
     * @param tmp 临时寄存器，用于可能的符号拓展，调用者保证在调用前后该寄存器值不被使用
     */
    private void storeStack(Reg val, Stack dst, GeneralPurposeRegister tmp) {
        panicIfMultiple(dst.asmType());
        GeneralPurposeRegister regRef = dst.fpRelative() ? FP : SP;
        if (dst.asmType().isWord()) {
            // D -> W st.w
            // W -> W st.w
            emitInst("st.w", val.reg(), regRef, new LA64AsmImmOperand(dst.offset()));
        } else {
            if (val.asmType().isDWord()) {
                // D -> D st.d
                emitInst("st.d", val.reg(), regRef, new LA64AsmImmOperand(dst.offset()));
            } else {
                // W -> D ==> W -[signExt]-> D(tmp) -[st.d]-> D
                lowerRegMove(val, new Reg(tmp, dst.asmType()));
                emitInst("st.d", tmp, regRef, new LA64AsmImmOperand(dst.offset()));
            }
        }
    }

    /**
     * 加载全局符号所指向内存中的值至寄存器
     *
     * @param dst 目标寄存器
     * @param sym 全局符号
     * @return 存放结果的寄存器的类型
     */
    private AsmType loadData(Reg dst, Data sym) {
        panicIfMultiple(dst.asmType());
        // 先加载符号地址
        emitInst("la.pcrel", dst.reg(), new LA64AsmSymOperand(sym.name()));
        // 再对符号地址访存
        // W <- W ld.w
        // D <- W ld.w
        // W <- D ld.w
        // D <- D ld.d
        boolean isLoadWord = dst.asmType().isWord() || sym.asmType().isWord();
        String mnemonic = isLoadWord ? "ld.w" : "ld.d";
        emitInst(mnemonic, dst.reg(), dst.reg(), LA64AsmImmOperand.ZERO);
        return isLoadWord ? AsmType.WORD_OR_DWORD : dst.asmType();
    }

    /**
     * 将寄存器值写入全局符号所指向内存中的位置
     *
     * @param val     写入值所在寄存器，保证写入寄存器值不变，除非 val 与 tmpExt 相同
     * @param sym     全局符号
     * @param tmpExt  临时寄存器，用于可能的符号拓展，调用者保证在调用前后该寄存器值不被使用
     * @param tmpAddr 临时寄存器，用于可能，调用者保证在调用前后该寄存器值不被使用
     */
    private void storeData(Reg val, Data sym, GeneralPurposeRegister tmpExt, GeneralPurposeRegister tmpAddr) {
        panicIfMultiple(sym.asmType());
        // 先加载符号地址
        emitInst("la.pcrel", tmpAddr, new LA64AsmSymOperand(sym.name()));
        // 再将值写入符号地址
        if (sym.asmType().isWord()) {
            // D -> W st.w
            // W -> W st.w
            emitInst("st.w", val.reg(), tmpAddr, LA64AsmImmOperand.ZERO);
        } else {
            if (val.asmType().isDWord()) {
                // D -> D st.d
                emitInst("st.d", val.reg(), tmpAddr, LA64AsmImmOperand.ZERO);
            } else {
                // W -> D ==> W -[signExt]-> D(tmp) -[st.d]-> D
                lowerRegMove(val, new Reg(tmpExt, sym.asmType()));
                emitInst("st.d", tmpExt, tmpAddr, LA64AsmImmOperand.ZERO);
            }
        }
    }

    private void panicIfMultiple(AsmType asmType) {
        if (!asmType.isSpecific()) {
            throw new RuntimeException("Multiple-type operand is not supported in this instruction");
        }
    }
}
