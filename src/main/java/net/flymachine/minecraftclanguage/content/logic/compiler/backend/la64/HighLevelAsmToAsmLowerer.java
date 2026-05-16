package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import com.mojang.datafixers.util.Pair;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.util.BitMath;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmDirective;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmInstruction;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmLabel;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmStatement;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelStaticVar;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelTopLevel;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister.*;

public final class HighLevelAsmToAsmLowerer implements HighLevelVisitor<Void> {

    public HighLevelAsmToAsmLowerer() { }

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
        if (staticVar.initValue == 0) {
            // 初始化为0，放在bss段
            emitDir("bss");
        } else {
            // 初始化非0，放在data段
            emitDir("data");
        }
        emitDir("balign", new LA64DirectiveNumArg(4));
        emitLabel(staticVar.name);
        if (staticVar.initValue != 0) {
            emitDir("word", new LA64DirectiveNumArg(staticVar.initValue));
        } else {
            emitDir("zero", new LA64DirectiveNumArg(4));
        }
    }

    private HighLevelFunction functionContext;

    private void lowerFunction(HighLevelFunction function) {
        functionContext = function;

        if (function.global) {
            emitDir("global", new LA64DirectiveSymArg(function.name));
        }
        emitDir("text");
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
                loadImm(dstReg, (int) srcImm.value());
            } else if (src instanceof GeneralPurposeRegister srcReg) {
                // 寄存器 -> 寄存器
                emitInst("move", dstReg, srcReg);
            } else if (src instanceof Stack srcStack) {
                // 栈 -> 寄存器
                loadStack(dstReg, srcStack);
            } else if (src instanceof Data srcData) {
                // 全局符号 -> 寄存器
                loadData(dstReg, srcData);
            }
            return null;
        }
        if (src instanceof GeneralPurposeRegister srcReg) {
            // 寄存器 ->
            if (dst instanceof Stack dstStack) {
                // 寄存器 -> 栈
                storeStack(srcReg, dstStack);
            } else if (dst instanceof Data dstData) {
                // 寄存器 -> 全局符号
                storeData(srcReg, dstData, T1);
            }
            return null;
        }

        // 其他情况：将源加载到临时寄存器 T0，再存储到目标
        // 此时 src 为 Immediate/Stack/Data，dst 为 Stack/Data
        GeneralPurposeRegister tmp = T0;

        if (src instanceof Immediate srcImm) {
            loadImm(tmp, (int) srcImm.value());
        } else if (src instanceof Stack srcStack) {
            loadStack(tmp, srcStack);
        } else if (src instanceof Data srcData) {
            loadData(tmp, srcData);
        } else {
            throw new UnsupportedOperationException("Unsupported source type: " + src.getClass().getSimpleName());
        }

        if (dst instanceof Stack dstStack) {
            storeStack(tmp, dstStack);
        } else if (dst instanceof Data dstData) {
            storeData(tmp, dstData, T1);
        } else {
            throw new UnsupportedOperationException("Unsupported destination type: " + dst.getClass().getSimpleName());
        }
        return null;
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
            case NEGATE -> "sub.w";
            case COMPLEMENT -> "nor";
            case NOT -> "sltui"; // 使用 sltui rd, rj, 1 来计算
        };

        // 先加载操作数到寄存器中
        GeneralPurposeRegister operand = loadOperand(src, T0);

        // 计算结果的存放地点
        var destResult = calcDestination(dst, T0);
        GeneralPurposeRegister dstReg = destResult.getFirst();

        // 计算结果
        if (op == UnaryOperator.NOT) {
            // sltui rd, rj, 1
            emitInst(opName, dstReg, operand, new LA64AsmImmOperand(1));
        } else {
            // sub.w/nor rd, zero, rk
            emitInst(opName, dstReg, ZERO, operand);
        }

        storeToDest(destResult, dst, T1);
        return null;
    }

    @Override
    public Void visitBinary(Binary binary) {
        BinaryOperator op = binary.op;
        HighLevelOperand lhs = binary.lhs;
        HighLevelOperand rhs = binary.rhs;
        HighLevelOperand dst = binary.dst;
        testBinaryHighLevelOperand(lhs, rhs, dst);

        // 特殊情况检查
        // 立即数加法
        if (op == BinaryOperator.ADD && (lhs instanceof Immediate || rhs instanceof Immediate)) {
            // 检查立即数是否可用 si12 表示，如果可以，生成 addi.w 指令，否则使用默认处理
            if (lhs instanceof Immediate imm && BitMath.isSi12((int) imm.value())) {
                // 立即数在左操作数
                visitAddSi12(new AddSi12(rhs, (int) imm.value(), dst));
                return null;
            } else if (rhs instanceof Immediate imm && BitMath.isSi12((int) imm.value())) {
                // 立即数在右操作数
                visitAddSi12(new AddSi12(lhs, (int) imm.value(), dst));
                return null;
            }
        }
        // 立即数减法，处理减立即数的情况
        if (op == BinaryOperator.SUBTRACT && rhs instanceof Immediate imm && BitMath.isSi12((int) -imm.value())) {
            visitAddSi12(new AddSi12(lhs, (int) -imm.value(), dst));
            return null;
        }
        // 立即数右移或左移，处理右操作数为立即数的情况
        if ((op == BinaryOperator.LEFT_SHIFT || op == BinaryOperator.RIGHT_SHIFT) && rhs instanceof Immediate imm) {
            String opName = op == BinaryOperator.LEFT_SHIFT ? "slli.w" : "srai.w";
            GeneralPurposeRegister srcReg = loadOperand(lhs, T0);
            Pair<GeneralPurposeRegister, Boolean> result = calcDestination(dst, T0);
            GeneralPurposeRegister dstReg = result.getFirst();
            // slli.w/srai.w rd, rj, ui5
            emitInst(opName, dstReg, srcReg, new LA64AsmImmOperand(BitMath.extractBits((int) imm.value(), 5)));
            storeToDest(result, dst, T1);
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
            if (BitMath.isUi12((int) imm.value())) {
                String opName = switch (op) {
                    case BITWISE_AND -> "andi";
                    case BITWISE_OR -> "ori";
                    case BITWISE_XOR -> "xori";
                    default -> throw new IllegalStateException("Unexpected operator: " + op);
                };
                GeneralPurposeRegister srcReg = loadOperand(lhs, T0);
                Pair<GeneralPurposeRegister, Boolean> result = calcDestination(dst, T0);
                GeneralPurposeRegister dstReg = result.getFirst();
                emitInst(opName, dstReg, srcReg, new LA64AsmImmOperand(imm.value()));
                storeToDest(result, dst, T1);
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
        if (op == BinaryOperator.LESS_THAN && rhs instanceof Immediate imm && BitMath.isSi12((int) imm.value())) {
            GeneralPurposeRegister srcReg = loadOperand(lhs, T0);
            Pair<GeneralPurposeRegister, Boolean> result = calcDestination(dst, T0);
            GeneralPurposeRegister dstReg = result.getFirst();
            // slti rd, rj, si12
            emitInst("slti", dstReg, srcReg, new LA64AsmImmOperand((int) imm.value()));
            storeToDest(result, dst, T1);
            return null;
        }

        // 通用处理
        String opName = switch (op) {
            case ADD -> "add.w";
            case SUBTRACT -> "sub.w";
            case MULTIPLY -> "mul.w";
            case DIVIDE -> "div.w";
            case MODULO -> "mod.w";
            case LEFT_SHIFT -> "sll.w";
            case RIGHT_SHIFT -> "sra.w"; // 算术右移
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
        GeneralPurposeRegister lhsReg = loadOperand(lhs, T0);

        // 加载右操作数至寄存器
        GeneralPurposeRegister rhsReg = loadOperand(rhs, T1);

        // 计算结果的存放地点
        var destResult = calcDestination(dst, T0);
        GeneralPurposeRegister dstReg = destResult.getFirst();

        // 计算结果
        // op rd, rj, rk
        emitInst(opName, dstReg, lhsReg, rhsReg);

        storeToDest(destResult, dst, T1);
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
        GeneralPurposeRegister srcReg = loadOperand(src, T0);

        // 计算结果的存放地点
        var destResult = calcDestination(dst, T0);
        GeneralPurposeRegister dstReg = destResult.getFirst();

        // 计算结果
        // addi.w rd, rj, si12
        emitInst("addi.w", dstReg, srcReg, new LA64AsmImmOperand(si12));

        storeToDest(destResult, dst, T1);
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
        GeneralPurposeRegister reg = loadOperand(cond, T0);

        // 跳转
        // beqz rj, offs21
        emitInst("beqz", reg, new LA64AsmSymOperand(".L" + branchTarget));
        return null;
    }

    @Override
    public Void visitBranchIfNotZero(BranchIfNotZero inst) {
        HighLevelOperand cond = inst.cond;
        String branchTarget = inst.target;
        testUnaryHighLevelOperand(cond, null);

        // 加载比较值至寄存器
        GeneralPurposeRegister reg = loadOperand(cond, T0);

        // 跳转
        // bnez rj, offs21
        emitInst("bnez", reg, new LA64AsmSymOperand(".L" + branchTarget));
        return null;
    }

    @Override
    public Void visitBranchIfComparison(BranchIfComparison inst) {
        Comparison cond = inst.cond;
        HighLevelOperand lhs = inst.lhs;
        HighLevelOperand rhs = inst.rhs;
        String branchTarget = inst.target;
        testBinaryHighLevelOperand(lhs, rhs, null);

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
        GeneralPurposeRegister lhsReg = loadOperand(lhs, T0);
        GeneralPurposeRegister rhsReg = loadOperand(rhs, T1);

        // 跳转
        // beq/bne/blt/bge rj, rd, offs16
        emitInst(opName, lhsReg, rhsReg, new LA64AsmSymOperand(".L" + branchTarget));
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


    private GeneralPurposeRegister loadOperand(HighLevelOperand toLoad, GeneralPurposeRegister fallback) {
        // 加载操作数至寄存器
        if (toLoad instanceof GeneralPurposeRegister reg) {
            // 本身就在寄存器，直接使用
            return reg;
        } else if (toLoad instanceof Stack stack) {
            // 在内存，加载到 fallback 寄存器
            loadStack(fallback, stack);
            return fallback;
        } else if (toLoad instanceof Immediate immediate) {
            // 立即数，加载到 fallback 寄存器
            loadImm(fallback, (int) immediate.value());
            return fallback;
        } else if (toLoad instanceof Data data) {
            // 全局符号，加载到 fallback 寄存器
            loadData(fallback, data);
            return fallback;
        } else {
            throw new UnsupportedOperationException("Unsupported operand type: " + toLoad.getClass().getSimpleName());
        }
    }

    /**
     * 计算将结果存放至 {@code dest} 需要的寄存器和是否需要额外访存指令
     *
     * @param dest     结果存放位置
     * @param fallback 如果需要存储，存放到该寄存器
     * @return 计算结果，包含实际存放结果的寄存器和是否需要额外访存
     */
    private static Pair<GeneralPurposeRegister, Boolean> calcDestination(
        HighLevelOperand dest, GeneralPurposeRegister fallback) {

        // 计算结果的存放地点
        if (dest instanceof GeneralPurposeRegister reg) {
            // 存放至寄存器，直接赋值
            return Pair.of(reg, false);
        } else if (dest instanceof Stack || dest instanceof Data) {
            // 存放至内存，得先存放到 fallback
            return Pair.of(fallback, true);
        } else {
            throw new UnsupportedOperationException("Unsupported destination type: " + dest.getClass().getSimpleName());
        }
    }

    /**
     * 将结果存放至目标地点
     *
     * @param destInfo 结果存放位置信息
     * @param dest     结果位置
     * @param tmp      写入全局符号时使用的临时寄存器，调用者保证在调用前后该寄存器值不被使用
     */
    private void storeToDest(
        Pair<GeneralPurposeRegister, Boolean> destInfo, HighLevelOperand dest, GeneralPurposeRegister tmp) {
        if (destInfo.getSecond()) {
            // 将先前存放在寄存器的结果写回栈/内存
            if (dest instanceof Stack dstStack) {
                storeStack(destInfo.getFirst(), dstStack);
            } else if (dest instanceof Data dstData) {
                storeData(destInfo.getFirst(), dstData, tmp);
            }
        }
    }

    /**
     * 加载立即数至寄存器
     *
     * @param dst 目标寄存器
     * @param imm 加载的立即数
     */
    private void loadImm(GeneralPurposeRegister dst, int imm) {
        emitInst("li.w", dst, new LA64AsmImmOperand(imm));
    }

    /**
     * 加载栈上值至寄存器
     *
     * @param dst 目标寄存器
     * @param src 栈位置
     */
    private void loadStack(GeneralPurposeRegister dst, Stack src) {
        GeneralPurposeRegister regRef = src.fpRelative() ? FP : SP;
        emitInst("ld.w", dst, regRef, new LA64AsmImmOperand(src.offset()));
    }

    /**
     * 将寄存器值写入栈上位置
     *
     * @param val 写入值所在寄存器
     * @param dst 栈位置
     */
    private void storeStack(GeneralPurposeRegister val, Stack dst) {
        GeneralPurposeRegister regRef = dst.fpRelative() ? FP : SP;
        emitInst("st.w", val, regRef, new LA64AsmImmOperand(dst.offset()));
    }

    /**
     * 加载全局符号所指向内存中的值至寄存器
     *
     * @param dst 目标寄存器
     * @param sym 全局符号
     */
    private void loadData(GeneralPurposeRegister dst, Data sym) {
        // 先加载符号地址
        emitInst("la.pcrel", dst, new LA64AsmSymOperand(sym.name()));
        // 再对符号地址访存
        emitInst("ld.w", dst, dst, new LA64AsmImmOperand(0));
    }

    /**
     * 将寄存器值写入全局符号所指向内存中的位置
     *
     * @param val 写入值所在寄存器
     * @param sym 全局符号
     * @param tmp 临时寄存器，调用者保证在调用前后该寄存器值不被使用
     */
    private void storeData(GeneralPurposeRegister val, Data sym, GeneralPurposeRegister tmp) {
        // 先加载符号地址
        emitInst("la.pcrel", tmp, new LA64AsmSymOperand(sym.name()));
        // 再将值写入符号地址
        emitInst("st.w", val, tmp, new LA64AsmImmOperand(0));
    }

}
