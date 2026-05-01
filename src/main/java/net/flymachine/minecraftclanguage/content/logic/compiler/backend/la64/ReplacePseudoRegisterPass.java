package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Pseudo;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Stack;

import java.util.HashMap;
import java.util.Map;

public final class ReplacePseudoRegisterPass implements HighLevelVisitor<Void> {

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
            inst.accept(this);
        }
        return stackOffset;
    }

    @Override
    public Void visitMove(Move inst) {
        inst.src = replacePseudo(inst.src);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visitRet(Ret inst) {
        return null;
    }

    @Override
    public Void visitUnary(Unary inst) {
        inst.src = replacePseudo(inst.src);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visitBinary(Binary inst) {
        inst.lhs = replacePseudo(inst.lhs);
        inst.rhs = replacePseudo(inst.rhs);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visitAddSi12(AddSi12 inst) {
        inst.src = replacePseudo(inst.src);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visitLabel(Label inst) {
        return null;
    }

    @Override
    public Void visitBranch(Branch inst) {
        return null;
    }

    @Override
    public Void visitBranchIfZero(BranchIfZero inst) {
        inst.cond = replacePseudo(inst.cond);
        return null;
    }

    @Override
    public Void visitBranchIfNotZero(BranchIfNotZero inst) {
        inst.cond = replacePseudo(inst.cond);
        return null;
    }

    @Override
    public Void visitBranchIfComparison(BranchIfComparison inst) {
        inst.lhs = replacePseudo(inst.lhs);
        inst.rhs = replacePseudo(inst.rhs);
        return null;
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
