package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.HighLevelInstruction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.Move;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.Unary;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Pseudo;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Stack;

import java.util.HashMap;
import java.util.Map;

public final class ReplacePseudoRegisterPass {

    public ReplacePseudoRegisterPass() { }

    // 指向当前的栈顶元素
    // 已经包含了 ra（-8）与 fp（-16）两个寄存器
    private int stackOffset = -16;

    // 记录每个伪寄存器对应栈上内存偏移量
    // TODO: 暂时将所有伪寄存器都放在栈上
    private final Map<String, Integer> registers = new HashMap<>();

    public int runOnProgram(HighLevelProgram program) {
        return runOnFunction(program.functionDefinition);
    }

    public int runOnFunction(HighLevelFunction function) {
        for (HighLevelInstruction inst : function.instructions) {
            if (inst instanceof Move move) {
                move.src = replacePseudo(move.src);
                move.dst = replacePseudo(move.dst);
            } else if (inst instanceof Unary unary) {
                unary.src = replacePseudo(unary.src);
                unary.dst = replacePseudo(unary.dst);
            }
        }
        return stackOffset;
    }

    private HighLevelOperand replacePseudo(HighLevelOperand operand) {
        if (operand instanceof Pseudo pseudo) {
            String id = pseudo.identifier();
            int offset = registers.computeIfAbsent(
                id, k -> {
                    stackOffset -= 4;
                    return stackOffset;
                });
            return new Stack(offset);
        }
        return operand;
    }

}
