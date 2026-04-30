package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import com.mojang.datafixers.util.Pair;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.util.BitMath;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmDirective;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmInstruction;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmLabel;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmStatement;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64AsmImmOperand;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64DirectiveSymArg;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Immediate;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Pseudo;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Stack;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;

import java.util.ArrayList;
import java.util.List;

public final class HighLevelAsmToAsmLowerer {

    public HighLevelAsmToAsmLowerer(int stackOffset) {
        if (!BitMath.isSi12(stackOffset)) {
            throw new IllegalArgumentException("Stack offset must be a signed 12-bit integer, but got: " + stackOffset);
        }
        this.stackOffset = stackOffset;
    }

    private final int stackOffset;

    public List<LA64AsmStatement> lower(HighLevelProgram highLevelProgram) {
        List<LA64AsmStatement> asmStatements = new ArrayList<>();
        lowerFunction(highLevelProgram.functionDefinition, asmStatements);
        return asmStatements;
    }

    private void lowerFunction(HighLevelFunction function, List<LA64AsmStatement> target) {
        target.add(new LA64AsmDirective("global", List.of(new LA64DirectiveSymArg(function.name))));
        target.add(new LA64AsmLabel(function.name));
        generatePrologue(target);

        for (HighLevelInstruction instruction : function.instructions) {
            lowerInstruction(instruction, target);
        }
    }

    private void generatePrologue(List<LA64AsmStatement> target) {
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

    private void generateEpilogue(List<LA64AsmStatement> target) {
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

    private void lowerInstruction(HighLevelInstruction instruction, List<LA64AsmStatement> target) {
        if (instruction instanceof Move moveInst) {
            lowerMoveInstruction(moveInst, target);
        } else if (instruction instanceof Ret) {
            generateEpilogue(target);
        } else if (instruction instanceof Unary unary) {
            lowerUnaryInstruction(unary, target);
        } else if (instruction instanceof Binary binary) {
            lowerBinaryInstruction(binary, target);
        } else {
            throw new UnsupportedOperationException(
                "Unsupported instruction type: " + instruction.getClass().getSimpleName());
        }
    }

    private void lowerMoveInstruction(Move inst, List<LA64AsmStatement> target) {
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
                return;
            } else if (dst instanceof Stack stack) {
                // 立即数到内存地址
                target.add(createLoadImm(GeneralPurposeRegister.T0, (int) immediate.value()));
                target.add(createStore(GeneralPurposeRegister.T0, stack));
                return;
            }
        } else if (src instanceof GeneralPurposeRegister srcReg) {
            if (dst instanceof GeneralPurposeRegister dstReg) {
                // 寄存器到寄存器
                target.add(new LA64AsmInstruction("move", List.of(dstReg, srcReg)));
                return;
            } else if (dst instanceof Stack stack) {
                // 寄存器到内存地址
                target.add(createStore(srcReg, stack));
                return;
            }
        } else if (src instanceof Stack srcStack) {
            if (dst instanceof GeneralPurposeRegister dstReg) {
                // 内存地址到寄存器
                target.add(createLoad(dstReg, srcStack));
                return;
            } else if (dst instanceof Stack dstStack) {
                // 内存地址到内存地址
                target.add(createLoad(GeneralPurposeRegister.T0, srcStack));
                target.add(createStore(GeneralPurposeRegister.T0, dstStack));
                return;
            }
        }
        throw new UnsupportedOperationException(
            "Unsupported move from " + src.getClass().getSimpleName() + " to " + dst.getClass().getSimpleName());
    }

    private void lowerUnaryInstruction(Unary unary, List<LA64AsmStatement> target) {
        UnaryOperator op = unary.op;
        HighLevelOperand src = unary.src;
        HighLevelOperand dst = unary.dst;
        testUnaryHighLevelOperand(src, dst);

        String opName = switch (op) {
            case NEGATE -> "sub.w";
            case COMPLEMENT -> "nor";
        };

        // 先加载操作数到寄存器中
        GeneralPurposeRegister operand = loadOperand(src, GeneralPurposeRegister.T0, target);

        // 计算结果的存放地点
        var destResult = calcDestination(dst, GeneralPurposeRegister.T0);
        GeneralPurposeRegister dstReg = destResult.getFirst();
        boolean needStore = destResult.getSecond();

        // 计算结果
        // op rd, rj, rk
        target.add(new LA64AsmInstruction(opName, List.of(dstReg, GeneralPurposeRegister.ZERO, operand)));

        if (needStore) {
            // 若存放至内存，得将先前存放在 t0 的结果写回内存
            target.add(createStore(GeneralPurposeRegister.T0, (Stack) dst));
        }
    }

    private void lowerBinaryInstruction(Binary binary, List<LA64AsmStatement> target) {
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
                lowerAddSi12Instruction(new AddSi12(rhs, (int) imm.value(), dst), target);
                return;
            } else if (rhs instanceof Immediate imm && BitMath.isSi12((int) imm.value())) {
                // 立即数在右操作数
                lowerAddSi12Instruction(new AddSi12(lhs, (int) imm.value(), dst), target);
                return;
            }
        }
        // 立即数减法，处理减立即数的情况
        if (op == BinaryOperator.SUBTRACT && rhs instanceof Immediate imm && BitMath.isSi12((int) -imm.value())) {
            lowerAddSi12Instruction(new AddSi12(lhs, (int) -imm.value(), dst), target);
            return;
        }

        String opName = switch (op) {
            case ADD -> "add.w";
            case SUBTRACT -> "sub.w";
            case MULTIPLY -> "mul.w";
            case DIVIDE -> "div.w";
            case MODULO -> "mod.w";
        };

        // 加载左操作数至寄存器
        GeneralPurposeRegister lhsReg = loadOperand(lhs, GeneralPurposeRegister.T0, target);

        // 加载右操作数至寄存器
        GeneralPurposeRegister rhsReg = loadOperand(rhs, GeneralPurposeRegister.T1, target);

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
    }

    private void lowerAddSi12Instruction(AddSi12 addSi12, List<LA64AsmStatement> target) {
        HighLevelOperand src = addSi12.src;
        int si12 = addSi12.si12;
        HighLevelOperand dst = addSi12.dst;
        testUnaryHighLevelOperand(src, dst);

        if (!BitMath.isSi12(si12)) {
            throw new UnsupportedOperationException("The immediate value must be a si12");
        }

        // 加载操作数至寄存器
        GeneralPurposeRegister srcReg = loadOperand(src, GeneralPurposeRegister.T0, target);

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
    }

    private static void testUnaryHighLevelOperand(HighLevelOperand src, HighLevelOperand dst) {
        if (dst instanceof Immediate) {
            throw new UnsupportedOperationException("Cannot store result in an immediate");
        }
        if (src instanceof Immediate) {
            throw new UnsupportedOperationException("Refuse to generate instruction to calculate constant, should be " +
                                                    "replaced earlier");
        }
        if (src instanceof Pseudo || dst instanceof Pseudo) {
            throw new UnsupportedOperationException("Cannot operate on pseudo register, should be replaced earlier");
        }
    }

    private static void testBinaryHighLevelOperand(HighLevelOperand lhs, HighLevelOperand rhs, HighLevelOperand dst) {
        if (dst instanceof Immediate) {
            throw new UnsupportedOperationException("Cannot store result in an immediate");
        }
        if (lhs instanceof Immediate && rhs instanceof Immediate) {
            throw new UnsupportedOperationException("Refuse to generate instruction to calculate constant, should be " +
                                                    "replaced earlier");
        }
        if (lhs instanceof Pseudo || rhs instanceof Pseudo || dst instanceof Pseudo) {
            throw new UnsupportedOperationException("Cannot operate on pseudo register, should be replaced earlier");
        }
    }


    private static GeneralPurposeRegister loadOperand(
        HighLevelOperand toLoad,
        GeneralPurposeRegister fallback,
        List<LA64AsmStatement> target) {

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

    private static LA64AsmInstruction createStore(GeneralPurposeRegister val, Stack src) {
        return new LA64AsmInstruction(
            "st.w",
            List.of(val, GeneralPurposeRegister.FP, new LA64AsmImmOperand(src.offset()))
        );
    }

}
