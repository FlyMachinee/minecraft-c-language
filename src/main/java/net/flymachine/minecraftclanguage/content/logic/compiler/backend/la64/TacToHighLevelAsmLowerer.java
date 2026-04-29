package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.HighLevelInstruction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.Move;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.Ret;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.Unary;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Immediate;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Pseudo;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.*;

import java.util.ArrayList;
import java.util.List;

public final class TacToHighLevelAsmLowerer {

    public TacToHighLevelAsmLowerer() { }

    public HighLevelProgram lower(TacProgram tacProgram) {
        HighLevelFunction highLevelFunction = lowerFunction(tacProgram.functionDefinition);
        return new HighLevelProgram(highLevelFunction);
    }

    private HighLevelFunction lowerFunction(TacFunction tacFunction) {
        List<TacInstruction> instructions = tacFunction.instructions;
        List<HighLevelInstruction> highLevelInstructions = new ArrayList<>();
        for (TacInstruction instruction : instructions) {
            lowerInstruction(instruction, highLevelInstructions);
        }
        return new HighLevelFunction(tacFunction.name, highLevelInstructions);
    }

    private void lowerInstruction(TacInstruction tacInstruction, List<HighLevelInstruction> target) {
        if (tacInstruction instanceof TacReturn tacReturn) {
            target.add(new Move(lowerValue(tacReturn.value), GeneralPurposeRegister.A0));
            target.add(new Ret());
        } else if (tacInstruction instanceof TacUnaryOperation tacUnaryOperation) {
            if (tacUnaryOperation.src instanceof TacIntConstant tacIntConstant) {
                // 若源操作数为常量，直接计算结果并生成一个 Move 指令
                int result = switch (tacUnaryOperation.op) {
                    case NEGATE -> -tacIntConstant.value;
                    case COMPLEMENT -> ~tacIntConstant.value;
                };
                target.add(new Move(new Immediate(result), lowerValue(tacUnaryOperation.dst)));
            } else {
                target.add(new Unary(
                    tacUnaryOperation.op,
                    lowerValue(tacUnaryOperation.src),
                    lowerValue(tacUnaryOperation.dst)));
            }
        } else {
            throw new UnsupportedOperationException(
                "Unsupported instruction type: " + tacInstruction.getClass().getSimpleName());
        }

    }

    private HighLevelOperand lowerValue(TacValue tacValue) {
        if (tacValue instanceof TacIntConstant tacIntConstant) {
            return new Immediate(tacIntConstant.value);
        } else if (tacValue instanceof TacVariable tacVariable) {
            return new Pseudo(tacVariable.identifier);
        }
        throw new UnsupportedOperationException("Unsupported value type: " + tacValue.getClass().getSimpleName());
    }

}
