package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

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
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.HighLevelInstruction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.Move;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.Ret;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.Unary;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Immediate;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Pseudo;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Stack;
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

        // if (src instanceof Immediate immediate) {
        //     if (dst instanceof GeneralPurposeRegister register) {
        //         // 立即数到寄存器
        //         target.add(createLoadImm(register, (int) immediate.value()));
        //         return;
        //     } else if (dst instanceof Stack stack) {
        //         // 立即数到内存地址
        //         target.add(createLoadImm(GeneralPurposeRegister.T0, (int) immediate.value()));
        //         target.add(createStore(GeneralPurposeRegister.T0, stack));
        //         return;
        //     }
        // } else if (src instanceof GeneralPurposeRegister srcReg) {
        //     if (dst instanceof GeneralPurposeRegister dstReg) {
        //         // 寄存器到寄存器
        //         target.add(new LA64AsmInstruction("move", List.of(dstReg, srcReg)));
        //         return;
        //     } else if (dst instanceof Stack stack) {
        //         // 寄存器到内存地址
        //         target.add(createStore(srcReg, stack));
        //         return;
        //     }
        // } else if (src instanceof Stack srcStack) {
        //     if (dst instanceof GeneralPurposeRegister dstReg) {
        //         // 内存地址到寄存器
        //         target.add(createLoad(dstReg, srcStack));
        //         return;
        //     } else if (dst instanceof Stack dstStack) {
        //         target.add(createLoad(GeneralPurposeRegister.T0, srcStack));
        //         target.add(createStore(GeneralPurposeRegister.T0, dstStack));
        //         return;
        //     }
        // }
        // throw new UnsupportedOperationException(
        //     "Unsupported move from " + src.getClass().getSimpleName() + " to " + dst.getClass().getSimpleName());

        // 将源操作数加载到寄存器中（立即数稍后特殊处理）
        GeneralPurposeRegister srcReg = null;
        if (src instanceof GeneralPurposeRegister reg) {
            // 已经在寄存器中
            srcReg = reg;
        } else if (src instanceof Stack stack) {
            // 从内存加载到 t0
            srcReg = GeneralPurposeRegister.T0;
            target.add(createLoad(srcReg, stack));
        } else if (!(src instanceof Immediate)) {
            throw new UnsupportedOperationException("Unsupported source type: " + src.getClass().getSimpleName());
        }
        // 立即数不在此处加载，后面生成 li.w

        GeneralPurposeRegister dstReg;
        boolean needStore = false;
        if (dst instanceof GeneralPurposeRegister reg) {
            // 直接存入目标寄存器
            dstReg = reg;
        } else if (dst instanceof Stack) {
            // 先存入临时寄存器，稍后写回内存
            dstReg = GeneralPurposeRegister.T0;
            needStore = true;
        } else {
            throw new UnsupportedOperationException("Unsupported destination type: " + dst.getClass().getSimpleName());
        }

        if (src instanceof Immediate imm) {
            // 立即数加载，li.w 直接加载到目标寄存器
            target.add(createLoadImm(dstReg, (int) imm.value()));
        } else {
            // 寄存器之间的移动，move
            target.add(new LA64AsmInstruction("move", List.of(dstReg, srcReg)));
        }

        if (needStore) {
            // 若存放至内存，得将先前存放在 t0 的结果写回内存
            target.add(createStore(GeneralPurposeRegister.T0, (Stack) dst));
        }

    }

    private void lowerUnaryInstruction(Unary unary, List<LA64AsmStatement> target) {
        UnaryOperator op = unary.op;
        HighLevelOperand src = unary.src;
        HighLevelOperand dst = unary.dst;

        if (dst instanceof Immediate) {
            throw new UnsupportedOperationException("Cannot store result in an immediate");
        }

        if (src instanceof Immediate) {
            throw new UnsupportedOperationException("Refuse to generate instruction to calculate constant, should be " +
                                                    "replaced earlier");
        }

        if (src instanceof Pseudo || dst instanceof Pseudo) {
            throw new UnsupportedOperationException(
                "Cannot operate on pseudo register, should be replaced earlier");
        }

        String opName = switch (op) {
            case NEGATE -> "sub.w";
            case COMPLEMENT -> "nor";
        };

        // 先加载操作数到寄存器中
        GeneralPurposeRegister operand;
        if (src instanceof GeneralPurposeRegister reg) {
            // 本身就在寄存器，直接使用
            operand = reg;
        } else if (src instanceof Stack stack) {
            // 在内存，加载到 t0 寄存器
            operand = GeneralPurposeRegister.T0;
            target.add(createLoad(operand, stack));
        } else {
            throw new UnsupportedOperationException("Unsupported source type: " + src.getClass().getSimpleName());
        }

        // 计算结果的存放地点
        GeneralPurposeRegister dstReg;
        boolean needStore = false;
        if (dst instanceof GeneralPurposeRegister reg) {
            // 存放至寄存器，直接赋值
            dstReg = reg;
        } else if (dst instanceof Stack) {
            // 存放至内存，得先存放到 t0
            dstReg = GeneralPurposeRegister.T0;
            needStore = true;
        } else {
            throw new UnsupportedOperationException("Unsupported destination type: " + dst.getClass().getSimpleName());
        }

        // 计算结果
        // op rd, rj, rk
        target.add(new LA64AsmInstruction(opName, List.of(dstReg, GeneralPurposeRegister.ZERO, operand)));

        if (needStore) {
            // 若存放至内存，得将先前存放在 t0 的结果写回内存
            target.add(createStore(GeneralPurposeRegister.T0, (Stack) dst));
        }
    }

    private LA64AsmInstruction createLoadImm(GeneralPurposeRegister dst, int imm) {
        return new LA64AsmInstruction(
            "li.w",
            List.of(dst, new LA64AsmImmOperand(imm))
        );
    }

    private LA64AsmInstruction createLoad(GeneralPurposeRegister dst, Stack src) {
        return new LA64AsmInstruction(
            "ld.w",
            List.of(dst, GeneralPurposeRegister.FP, new LA64AsmImmOperand(src.offset()))
        );
    }

    private LA64AsmInstruction createStore(GeneralPurposeRegister val, Stack src) {
        return new LA64AsmInstruction(
            "st.w",
            List.of(val, GeneralPurposeRegister.FP, new LA64AsmImmOperand(src.offset()))
        );
    }

}
