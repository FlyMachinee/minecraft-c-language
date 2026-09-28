package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelTopLevel;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.*;

import java.util.HashMap;
import java.util.Map;

public final class ReplacePseudoOperandPass implements HighLevelVisitor<Void> {

    public ReplacePseudoOperandPass(BackendSymbolTable backendSymbolTable) {
        this.backendSymbolTable = backendSymbolTable;
    }

    private final BackendSymbolTable backendSymbolTable;

    // 指向当前已使用的元素
    // 相对于 $fp 寻址
    private long stackOffset;

    // 记录每个伪寄存器对应栈上内存偏移量
    // TODO: 暂时将所有伪寄存器都放在栈上
    private final Map<String, Long> nameToOffset = new HashMap<>();

    public void runOnProgram(HighLevelProgram program) {
        for (HighLevelTopLevel topLevel : program.topLevels) {
            if (topLevel instanceof HighLevelFunction func) {
                runOnFunction(func);
            }
        }
    }

    private void runOnFunction(HighLevelFunction function) {
        stackOffset = -function.savedRegisters * 8;
        for (HighLevelInstruction inst : function.insts) {
            inst.accept(this);
        }
        function.variableSize = -stackOffset - function.savedRegisters * 8;
    }

    @Override
    public Void visit(Move inst) {
        inst.src = replacePseudo(inst.src);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visit(Ret inst) {
        return null;
    }

    @Override
    public Void visit(Binary inst) {
        inst.lhs = replacePseudo(inst.lhs);
        inst.rhs = replacePseudo(inst.rhs);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visit(AddSi12 inst) {
        inst.src = replacePseudo(inst.src);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visit(Label inst) {
        return null;
    }

    @Override
    public Void visit(Branch inst) {
        return null;
    }

    @Override
    public Void visit(BranchIfZero inst) {
        inst.cond = replacePseudo(inst.cond);
        return null;
    }

    @Override
    public Void visit(BranchIfNotZero inst) {
        inst.cond = replacePseudo(inst.cond);
        return null;
    }

    @Override
    public Void visit(BranchIfComparison inst) {
        inst.lhs = replacePseudo(inst.lhs);
        inst.rhs = replacePseudo(inst.rhs);
        return null;
    }

    @Override
    public Void visit(Call inst) {
        return null;
    }

    @Override
    public Void visit(Compare inst) {
        inst.lhs = replacePseudo(inst.lhs);
        inst.rhs = replacePseudo(inst.rhs);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visit(DivOrMod inst) {
        inst.lhs = replacePseudo(inst.lhs);
        inst.rhs = replacePseudo(inst.rhs);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visit(BitwiseShift inst) {
        inst.lhs = replacePseudo(inst.lhs);
        inst.rhs = replacePseudo(inst.rhs);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visit(Bitwise inst) {
        inst.lhs = replacePseudo(inst.lhs);
        inst.rhs = replacePseudo(inst.rhs);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visit(BstrpickZeroExtend inst) {
        inst.src = replacePseudo(inst.src);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visit(AddSignExtend inst) {
        inst.src = replacePseudo(inst.src);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visit(DoubleFromInt inst) {
        inst.src = replacePseudo(inst.src);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visit(DoubleToIntRoundZero inst) {
        inst.src = replacePseudo(inst.src);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visit(CompareDouble inst) {
        inst.lhs = replacePseudo(inst.lhs);
        inst.rhs = replacePseudo(inst.rhs);
        return null;
    }

    @Override
    public Void visit(GetCC inst) {
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visit(BranchIfCCZero inst) {
        return null;
    }

    @Override
    public Void visit(BranchIfCCNotZero inst) {
        return null;
    }

    @Override
    public Void visit(DoubleNegate inst) {
        inst.src = replacePseudo(inst.src);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visit(LoadAddress inst) {
        inst.obj = replacePseudo(inst.obj);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visit(Load inst) {
        inst.ptr = replacePseudo(inst.ptr);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visit(Store inst) {
        inst.src = replacePseudo(inst.src);
        inst.ptr = replacePseudo(inst.ptr);
        return null;
    }

    @Override
    public Void visit(AddLeftShift inst) {
        inst.base = replacePseudo(inst.base);
        inst.index = replacePseudo(inst.index);
        inst.dst = replacePseudo(inst.dst);
        return null;
    }

    @Override
    public Void visit(CallIndirect inst) {
        inst.funcPtr = replacePseudo(inst.funcPtr);
        return null;
    }

    private HighLevelOperand replacePseudo(HighLevelOperand operand) {
        if (operand instanceof Pseudo pseudo) {
            String id = pseudo.name();
            if (nameToOffset.containsKey(id)) {
                return new Memory(GeneralPurposeRegister.FP, nameToOffset.get(id));
            } else {
                BackendSymbolTable.Entry entry = backendSymbolTable.get(id);
                if (entry == null) {
                    throw new IllegalStateException("Undefined symbol: " + id);
                }
                if (entry instanceof BackendSymbolTable.FuncEntry) {
                    return new Data(id);
                }
                BackendSymbolTable.ObjectEntry objectEntry = (BackendSymbolTable.ObjectEntry) entry;
                if (objectEntry.isStatic()) {
                    return new Data(id);
                } else {
                    allocateStack(id, objectEntry.asmType());
                    return new Memory(GeneralPurposeRegister.FP, stackOffset);
                }
            }
        } else if (operand instanceof PseudoMemory pseudoMemory) {
            String id = pseudoMemory.name();
            long offset = pseudoMemory.offset();
            if (nameToOffset.containsKey(id)) {
                return new Memory(GeneralPurposeRegister.FP, nameToOffset.get(id) + offset);
            } else {
                BackendSymbolTable.Entry entry = backendSymbolTable.get(id);
                if (entry == null || entry instanceof BackendSymbolTable.FuncEntry) {
                    throw new IllegalStateException("Undefined symbol: " + id);
                }
                BackendSymbolTable.ObjectEntry objectEntry = (BackendSymbolTable.ObjectEntry) entry;
                if (objectEntry.isStatic()) {
                    assert offset == 0 : "Static object should not have offset: " + id;
                    return new Data(id);
                } else {
                    allocateStack(id, objectEntry.asmType());
                    return new Memory(GeneralPurposeRegister.FP, stackOffset + offset);
                }
            }
        }
        return operand;
    }

    private void allocateStack(String id, AsmType asmType) {
        long size = asmType.size();
        long alignment = asmType.alignment();
        stackOffset -= size;
        // 向负无穷对齐至 alignment
        stackOffset = Math.floorDiv(stackOffset, alignment) * alignment;
        nameToOffset.put(id, stackOffset);
    }

}
