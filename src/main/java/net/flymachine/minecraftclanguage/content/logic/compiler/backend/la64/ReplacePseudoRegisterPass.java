package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelTopLevel;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Data;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Pseudo;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Stack;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;

import java.util.HashMap;
import java.util.Map;

public final class ReplacePseudoRegisterPass implements HighLevelVisitor<Void> {

    public ReplacePseudoRegisterPass(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    private final SymbolTable symbolTable;

    // 指向当前已使用的元素
    // 相对于 $fp 寻址
    private int stackOffset;

    // 记录每个伪寄存器对应栈上内存偏移量
    // TODO: 暂时将所有伪寄存器都放在栈上
    private final Map<String, Integer> registers = new HashMap<>();

    public void runOnProgram(HighLevelProgram program) {
        for (HighLevelTopLevel topLevel : program.topLevels) {
            if (topLevel instanceof HighLevelFunction func) {
                runOnFunction(func);
            }
        }
    }

    public void runOnFunction(HighLevelFunction function) {
        stackOffset = -function.savedRegisters * 8;
        for (HighLevelInstruction inst : function.insts) {
            inst.accept(this);
        }
        function.variableSize = -stackOffset - function.savedRegisters * 8;
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

    @Override
    public Void visitCall(Call inst) {
        return null;
    }

    private HighLevelOperand replacePseudo(HighLevelOperand operand) {
        if (operand instanceof Pseudo pseudo) {
            String id = pseudo.name();
            if (registers.containsKey(id)) {
                return new Stack(registers.get(id));
            } else {
                SymbolTable.Entry entry = symbolTable.get(id);
                if (entry != null && !(entry.attr instanceof SymbolTable.Entry.LocalAttr)) {
                    return new Data(id);
                } else {
                    registers.put(id, stackOffset -= 4);
                    return new Stack(stackOffset);
                }
            }
        }
        return operand;
    }

}
