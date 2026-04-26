package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmDirective;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmInstruction;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmLabel;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmStatement;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64AsmImmOperand;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64AsmOperand;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64AsmRegOperand;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64DirectiveSymArg;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.HighLevelInstruction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.Move;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.Ret;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Immediate;

import java.util.ArrayList;
import java.util.List;

public final class HighLevelAsmToAsmLowerer {

    public HighLevelAsmToAsmLowerer() { }

    public List<LA64AsmStatement> lower(HighLevelProgram highLevelProgram) {
        List<LA64AsmStatement> asmStatements = new ArrayList<>();
        lowerFunction(highLevelProgram.functionDefinition, asmStatements);
        return asmStatements;
    }

    private void lowerFunction(HighLevelFunction function, List<LA64AsmStatement> target) {
        target.add(new LA64AsmDirective("global", List.of(new LA64DirectiveSymArg(function.name))));
        target.add(new LA64AsmLabel(function.name));
        for (HighLevelInstruction instruction : function.instructions) {
            lowerInstruction(instruction, target);
        }
    }

    private void lowerInstruction(HighLevelInstruction instruction, List<LA64AsmStatement> target) {
        if (instruction instanceof Move moveInst) {
            LA64AsmOperand dst = lowerOperand(moveInst.dst);
            LA64AsmOperand src = lowerOperand(moveInst.src);

            if (dst instanceof LA64AsmImmOperand) {
                throw new UnsupportedOperationException("Cannot move to an immediate operand");
            }

            if (src instanceof LA64AsmImmOperand) {
                target.add(new LA64AsmInstruction("li.w", List.of(dst, src)));
            } else {
                throw new UnsupportedOperationException("Cannot move from reg to reg");
            }

        } else if (instruction instanceof Ret) {
            target.add(new LA64AsmInstruction("ret", null));

        } else {
            throw new UnsupportedOperationException(
                "Unsupported instruction type: " + instruction.getClass().getSimpleName());
        }
    }

    private LA64AsmOperand lowerOperand(HighLevelOperand operand) {
        if (operand instanceof GeneralPurposeRegister reg) {
            return new LA64AsmRegOperand(reg);
        } else if (operand instanceof Immediate imm) {
            return new LA64AsmImmOperand(imm);
        }
        throw new UnsupportedOperationException("Unsupported operand type: " + operand.getClass().getSimpleName());
    }

}
