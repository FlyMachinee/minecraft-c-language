package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import com.mojang.datafixers.util.Pair;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.util.BitMath;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmDirective;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmInstruction;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmLabel;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmStatement;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64AsmImmOperand;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64AsmSymOperand;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64DirectiveSymArg;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Immediate;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Pseudo;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Stack;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;

import java.util.ArrayList;
import java.util.List;

public final class HighLevelAsmToAsmLowerer implements HighLevelVisitor<Void> {

    public HighLevelAsmToAsmLowerer(int stackOffset) {
        if (!BitMath.isSi12(stackOffset)) {
            throw new IllegalArgumentException("Stack offset must be a signed 12-bit integer, but got: " + stackOffset);
        }
        this.stackOffset = stackOffset;
    }

    private final int stackOffset;
    private final List<LA64AsmStatement> target = new ArrayList<>();

    public List<LA64AsmStatement> lower(HighLevelProgram highLevelProgram) {
        lowerFunction(highLevelProgram.functionDefinition);
        return target;
    }

    private void lowerFunction(HighLevelFunction function) {
        target.add(new LA64AsmDirective("global", List.of(new LA64DirectiveSymArg(function.name))));
        target.add(new LA64AsmLabel(function.name));
        generatePrologue();

        for (HighLevelInstruction instruction : function.instructions) {
            instruction.accept(this);
        }
    }

    private void generatePrologue() {
        // 申请栈空间
        target.add(new LA64AsmInstruction(
            "addi.d",
            List.of(
                GeneralPurposeRegister.SP,
                GeneralPurposeRegister.SP,
                new LA64AsmImmOperand(stackOffset))));
        // 保存 ra 与 fp
        target.add(new LA64AsmInstruction(
            "st.d",
            List.of(
                GeneralPurposeRegister.RA,
                GeneralPurposeRegister.SP,
                new LA64AsmImmOperand(-stackOffset - 8))));
        target.add(new LA64AsmInstruction(
            "st.d",
            List.of(
                GeneralPurposeRegister.FP,
                GeneralPurposeRegister.SP,
                new LA64AsmImmOperand(-stackOffset - 16))
        ));
        // 设置新的 fp
        target.add(new LA64AsmInstruction(
            "addi.d",
            List.of(
                GeneralPurposeRegister.FP,
                GeneralPurposeRegister.SP,
                new LA64AsmImmOperand(-stackOffset))
        ));
    }

    private void generateEpilogue() {
        // 恢复 ra 与 fp
        target.add(new LA64AsmInstruction(
            "ld.d",
            List.of(
                GeneralPurposeRegister.RA,
                GeneralPurposeRegister.SP,
                new LA64AsmImmOperand(-stackOffset - 8))));
        target.add(new LA64AsmInstruction(
            "ld.d",
            List.of(
                GeneralPurposeRegister.FP,
                GeneralPurposeRegister.SP,
                new LA64AsmImmOperand(-stackOffset - 16))
        ));
        // 释放栈空间
        target.add(new LA64AsmInstruction(
            "addi.d",
            List.of(
                GeneralPurposeRegister.SP,
                GeneralPurposeRegister.SP,
                new LA64AsmImmOperand(-stackOffset))));
        // 返回
        target.add(new LA64AsmInstruction("ret", null));
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

        if (src instanceof Immediate immediate) {
            if (dst instanceof GeneralPurposeRegister register) {
                // 立即数到寄存器
                target.add(createLoadImm(register, (int) immediate.value()));
                return null;
            } else if (dst instanceof Stack stack) {
                // 立即数到内存地址
                target.add(createLoadImm(GeneralPurposeRegister.T0, (int) immediate.value()));
                target.add(createStore(GeneralPurposeRegister.T0, stack));
                return null;
            }
        } else if (src instanceof GeneralPurposeRegister srcReg) {
            if (dst instanceof GeneralPurposeRegister dstReg) {
                // 寄存器到寄存器
                target.add(new LA64AsmInstruction("move", List.of(dstReg, srcReg)));
                return null;
            } else if (dst instanceof Stack stack) {
                // 寄存器到内存地址
                target.add(createStore(srcReg, stack));
                return null;
            }
        } else if (src instanceof Stack srcStack) {
            if (dst instanceof GeneralPurposeRegister dstReg) {
                // 内存地址到寄存器
                target.add(createLoad(dstReg, srcStack));
                return null;
            } else if (dst instanceof Stack dstStack) {
                // 内存地址到内存地址
                target.add(createLoad(GeneralPurposeRegister.T0, srcStack));
                target.add(createStore(GeneralPurposeRegister.T0, dstStack));
                return null;
            }
        }
        throw new UnsupportedOperationException(
            "Unsupported move from " + src.getClass().getSimpleName() + " to " + dst.getClass().getSimpleName());
    }

    @Override
    public Void visitRet(Ret inst) {
        generateEpilogue();
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
        GeneralPurposeRegister operand = loadOperand(src, GeneralPurposeRegister.T0);

        // 计算结果的存放地点
        var destResult = calcDestination(dst, GeneralPurposeRegister.T0);
        GeneralPurposeRegister dstReg = destResult.getFirst();
        boolean needStore = destResult.getSecond();

        // 计算结果
        if (op == UnaryOperator.NOT) {
            // sltui rd, rj, 1
            target.add(new LA64AsmInstruction(opName, List.of(dstReg, operand, new LA64AsmImmOperand(1))));
        } else {
            // sub.w/nor rd, zero, rk
            target.add(new LA64AsmInstruction(opName, List.of(dstReg, GeneralPurposeRegister.ZERO, operand)));
        }

        if (needStore) {
            // 若存放至内存，得将先前存放在 t0 的结果写回内存
            target.add(createStore(GeneralPurposeRegister.T0, (Stack) dst));
        }
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
            GeneralPurposeRegister srcReg = loadOperand(lhs, GeneralPurposeRegister.T0);
            Pair<GeneralPurposeRegister, Boolean> result = calcDestination(dst, GeneralPurposeRegister.T0);
            GeneralPurposeRegister dstReg = result.getFirst();
            boolean needStore = result.getSecond();
            // slli.w/srai.w rd, rj, ui5
            target.add(new LA64AsmInstruction(
                opName, List.of(dstReg, srcReg, new LA64AsmImmOperand(BitMath.extractBits((int) imm.value(), 5)))));
            if (needStore) {
                target.add(createStore(dstReg, (Stack) dst));
            }
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
                GeneralPurposeRegister srcReg = loadOperand(lhs, GeneralPurposeRegister.T0);
                Pair<GeneralPurposeRegister, Boolean> result = calcDestination(dst, GeneralPurposeRegister.T0);
                GeneralPurposeRegister dstReg = result.getFirst();
                boolean needStore = result.getSecond();
                target.add(new LA64AsmInstruction(
                    opName, List.of(dstReg, srcReg, new LA64AsmImmOperand(imm.value()))));
                if (needStore) {
                    target.add(createStore(dstReg, (Stack) dst));
                }
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
            GeneralPurposeRegister srcReg = loadOperand(lhs, GeneralPurposeRegister.T0);
            Pair<GeneralPurposeRegister, Boolean> result = calcDestination(dst, GeneralPurposeRegister.T0);
            GeneralPurposeRegister dstReg = result.getFirst();
            boolean needStore = result.getSecond();
            // slti rd, rj, si12
            target.add(new LA64AsmInstruction(
                "slti", List.of(dstReg, srcReg, new LA64AsmImmOperand((int) imm.value()))));
            if (needStore) {
                target.add(createStore(dstReg, (Stack) dst));
            }
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
        GeneralPurposeRegister lhsReg = loadOperand(lhs, GeneralPurposeRegister.T0);

        // 加载右操作数至寄存器
        GeneralPurposeRegister rhsReg = loadOperand(rhs, GeneralPurposeRegister.T1);

        // 计算结果的存放地点
        var destResult = calcDestination(dst, GeneralPurposeRegister.T0);
        GeneralPurposeRegister dstReg = destResult.getFirst();
        boolean needStore = destResult.getSecond();

        // 计算结果
        // op rd, rj, rk
        target.add(new LA64AsmInstruction(opName, List.of(dstReg, lhsReg, rhsReg)));

        if (needStore) {
            // 若存放至内存，得将先前存放在 t0 的结果写回内存
            target.add(createStore(GeneralPurposeRegister.T0, (Stack) dst));
        }
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
        GeneralPurposeRegister srcReg = loadOperand(src, GeneralPurposeRegister.T0);

        // 计算结果的存放地点
        var destResult = calcDestination(dst, GeneralPurposeRegister.T0);
        GeneralPurposeRegister dstReg = destResult.getFirst();
        boolean needStore = destResult.getSecond();

        // 计算结果
        // addi.w rd, rj, si12
        target.add(new LA64AsmInstruction("addi.w", List.of(dstReg, srcReg, new LA64AsmImmOperand(si12))));

        if (needStore) {
            // 若存放至内存，得将先前存放在 t0 的结果写回内存
            target.add(createStore(GeneralPurposeRegister.T0, (Stack) dst));
        }
        return null;
    }

    @Override
    public Void visitLabel(Label inst) {
        target.add(new LA64AsmLabel(".L" + inst.identifier));
        return null;
    }

    @Override
    public Void visitBranch(Branch inst) {
        target.add(new LA64AsmInstruction("b", List.of(new LA64AsmSymOperand(".L" + inst.target))));
        return null;
    }

    @Override
    public Void visitBranchIfZero(BranchIfZero inst) {
        HighLevelOperand cond = inst.cond;
        String branchTarget = inst.target;
        testUnaryHighLevelOperand(cond, null);

        // 加载比较值至寄存器
        GeneralPurposeRegister reg = loadOperand(cond, GeneralPurposeRegister.T0);

        // 跳转
        // beqz rj, offs21
        target.add(new LA64AsmInstruction("beqz", List.of(reg, new LA64AsmSymOperand(".L" + branchTarget))));
        return null;
    }

    @Override
    public Void visitBranchIfNotZero(BranchIfNotZero inst) {
        HighLevelOperand cond = inst.cond;
        String branchTarget = inst.target;
        testUnaryHighLevelOperand(cond, null);

        // 加载比较值至寄存器
        GeneralPurposeRegister reg = loadOperand(cond, GeneralPurposeRegister.T0);

        // 跳转
        // bnez rj, offs21
        target.add(new LA64AsmInstruction("bnez", List.of(reg, new LA64AsmSymOperand(".L" + branchTarget))));
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
        GeneralPurposeRegister lhsReg = loadOperand(lhs, GeneralPurposeRegister.T0);
        GeneralPurposeRegister rhsReg = loadOperand(rhs, GeneralPurposeRegister.T1);

        // 跳转
        // beq/bne/blt/bge rj, rd, offs16
        target.add(new LA64AsmInstruction(opName, List.of(lhsReg, rhsReg, new LA64AsmSymOperand(".L" + branchTarget))));
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
            target.add(createLoad(fallback, stack));
            return fallback;
        } else if (toLoad instanceof Immediate immediate) {
            // 立即数，加载到 fallback 寄存器
            target.add(createLoadImm(fallback, (int) immediate.value()));
            return fallback;
        } else {
            throw new UnsupportedOperationException("Unsupported operand type: " + toLoad.getClass().getSimpleName());
        }
    }

    private static Pair<GeneralPurposeRegister, Boolean> calcDestination(
        HighLevelOperand dest, GeneralPurposeRegister fallback) {

        // 计算结果的存放地点
        if (dest instanceof GeneralPurposeRegister reg) {
            // 存放至寄存器，直接赋值
            return Pair.of(reg, false);
        } else if (dest instanceof Stack) {
            // 存放至内存，得先存放到 fallback
            return Pair.of(fallback, true);
        } else {
            throw new UnsupportedOperationException("Unsupported destination type: " + dest.getClass().getSimpleName());
        }
    }

    private static LA64AsmInstruction createLoadImm(GeneralPurposeRegister dst, int imm) {
        return new LA64AsmInstruction(
            "li.w",
            List.of(dst, new LA64AsmImmOperand(imm))
        );
    }

    private static LA64AsmInstruction createLoad(GeneralPurposeRegister dst, Stack src) {
        return new LA64AsmInstruction(
            "ld.w",
            List.of(dst, GeneralPurposeRegister.FP, new LA64AsmImmOperand(src.offset()))
        );
    }

    private static LA64AsmInstruction createStore(GeneralPurposeRegister val, Stack dst) {
        return new LA64AsmInstruction(
            "st.w",
            List.of(val, GeneralPurposeRegister.FP, new LA64AsmImmOperand(dst.offset()))
        );
    }

}
