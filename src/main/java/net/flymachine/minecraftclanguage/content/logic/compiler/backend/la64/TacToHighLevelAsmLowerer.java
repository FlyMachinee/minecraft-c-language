package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.FunctionType;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.*;

import java.util.ArrayList;
import java.util.List;

import static net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister.*;

public final class TacToHighLevelAsmLowerer implements TacVisitor<Void> {

    public TacToHighLevelAsmLowerer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    private final SymbolTable symbolTable;
    private final BackendSymbolTable backendSymbolTable = new BackendSymbolTable();

    public BackendSymbolTable getBackendSymbolTable() {
        return backendSymbolTable;
    }

    public HighLevelProgram lower(TacProgram tacProgram) {
        List<HighLevelTopLevel> topLevels = new ArrayList<>();
        for (TacTopLevel topLevel : tacProgram.topLevels) {
            if (topLevel instanceof TacFunction func) {
                topLevels.add(lowerFunction(func));
            } else if (topLevel instanceof TacStaticVariable staticVar) {
                topLevels.add(
                    new HighLevelStaticVar(staticVar.name, staticVar.global, staticVar.type.sizeof(), staticVar.init));
            }
        }
        // 建立后端符号表
        for (SymbolTable.Entry entry : symbolTable.getEntries()) {
            if (entry.type instanceof FunctionType) {
                backendSymbolTable.put(entry.id.id, new BackendSymbolTable.FuncEntry(entry.attr.isDefinition()));
            } else {
                backendSymbolTable.put(
                    entry.id.id,
                    new BackendSymbolTable.ObjectEntry(entry.attr instanceof SymbolTable.Entry.StaticAttr));
            }
        }
        return new HighLevelProgram(topLevels);
    }

    private List<HighLevelInstruction> target;

    private static final GeneralPurposeRegister[] ARG_REGS = {A0, A1, A2, A3, A4, A5, A6, A7};

    private int maxCallStackArgSize;

    private Pseudo pseudo(String name) {
        return new Pseudo(name, symbolTable.get(name).type.toAsmType());
    }

    private Immediate immediate(Constant constant) {
        return new Immediate(constant.toLong().value(), constant.getType().toAsmType());
    }

    private HighLevelFunction lowerFunction(TacFunction tacFunction) {
        target = new ArrayList<>();
        maxCallStackArgSize = 0;

        // 拷贝参数至栈上
        int argIndex = 0;
        while (argIndex < Math.min(ARG_REGS.length, tacFunction.params.size())) {
            String name = tacFunction.params.get(argIndex);
            AsmType asmType = symbolTable.get(name).type.toAsmType();
            target.add(new Move(new Reg(ARG_REGS[argIndex], asmType), pseudo(name)));
            argIndex++;
        }

        int stackOffset = 0;
        while (argIndex < tacFunction.params.size()) {
            String name = tacFunction.params.get(argIndex);
            AsmType asmType = symbolTable.get(name).type.toAsmType();
            target.add(new Move(new Stack(stackOffset, asmType, true), pseudo(name)));
            stackOffset += 8;
            argIndex++;
        }

        List<TacInstruction> instructions = tacFunction.insts;
        for (TacInstruction instruction : instructions) {
            instruction.accept(this);
        }
        HighLevelFunction function = new HighLevelFunction(tacFunction.name, tacFunction.global, target);
        function.maxCallStackArgSize = maxCallStackArgSize;
        return function;
    }

    @Override
    public Void visit(TacReturn inst) {
        HighLevelOperand src = lowerValue(inst.value);
        target.add(new Move(src, new Reg(A0, src.asmType())));
        target.add(new Ret());
        return null;
    }

    @Override
    public Void visit(TacUnaryOperation inst) {
        if (inst.src instanceof TacConstant tacConstant) {
            // 若源操作数为常量，直接计算结果并生成 Move 指令
            Constant result = tacConstant.value.apply(inst.op);
            target.add(new Move(immediate(result), lowerValue(inst.dst)));
        } else {
            target.add(new Unary(inst.op, lowerValue(inst.src), lowerValue(inst.dst)));
        }
        return null;
    }

    @Override
    public Void visit(TacBinaryOperation inst) {
        if (inst.lhs instanceof TacConstant tacLhsConstant &&
            inst.rhs instanceof TacConstant tacRhsConstant) {
            // 若左、右操作数均为常量，直接计算结果并生成 Move 指令
            Constant result = tacLhsConstant.value.apply(inst.op, tacRhsConstant.value);
            target.add(new Move(immediate(result), lowerValue(inst.dst)));
        } else {
            target.add(new Binary(inst.op, lowerValue(inst.lhs), lowerValue(inst.rhs), lowerValue(inst.dst)));
        }
        return null;
    }

    @Override
    public Void visit(TacCopy inst) {
        target.add(new Move(lowerValue(inst.src), lowerValue(inst.dst)));
        return null;
    }

    @Override
    public Void visit(TacLabel inst) {
        target.add(new Label(inst.name));
        return null;
    }

    @Override
    public Void visit(TacJump inst) {
        target.add(new Branch(inst.target));
        return null;
    }

    @Override
    public Void visit(TacJumpIfZero inst) {
        if (inst.cond instanceof TacConstant tacConstant) {
            // 常量检查
            if (tacConstant.value.isZero()) {
                target.add(new Branch(inst.target));
            }
        } else {
            target.add(new BranchIfZero(lowerValue(inst.cond), inst.target));
        }
        return null;
    }

    @Override
    public Void visit(TacJumpIfNotZero inst) {
        if (inst.cond instanceof TacConstant tacConstant) {
            // 常量检查
            if (!tacConstant.value.isZero()) {
                target.add(new Branch(inst.target));
            }
        } else {
            target.add(new BranchIfNotZero(lowerValue(inst.cond), inst.target));
        }
        return null;
    }

    @Override
    public Void visit(TacJumpIfComparison inst) {
        if (inst.lhs instanceof TacConstant lhsConstant && inst.rhs instanceof TacConstant rhsConstant) {
            // 常量检查
            boolean conditionMet = !lhsConstant.value.apply(inst.cond, rhsConstant.value).isZero();
            if (conditionMet) {
                target.add(new Branch(inst.target));
            }
        } else {
            target.add(new BranchIfComparison(inst.cond, lowerValue(inst.lhs), lowerValue(inst.rhs), inst.target));
        }
        return null;
    }

    @Override
    public Void visit(TacFunctionCall inst) {
        // 参数传递
        // 前八个使用 a0 -> a1 -> ... -> a7
        // 接下来使用 0(sp) -> 8(sp) -> ...
        // 不需要分配或回收栈空间，由函数序言和尾声进行处理，函数序言中会分配好足够使用的栈空间

        // 寄存器传递参数
        int argIndex = 0;
        while (argIndex < Math.min(ARG_REGS.length, inst.args.size())) {
            HighLevelOperand arg = lowerValue(inst.args.get(argIndex));
            target.add(new Move(arg, new Reg(ARG_REGS[argIndex], arg.asmType())));
            argIndex++;
        }

        // 栈传递参数
        int stackOffset = 0;
        while (argIndex < inst.args.size()) {
            HighLevelOperand arg = lowerValue(inst.args.get(argIndex));
            target.add(new Move(arg, new Stack(stackOffset, arg.asmType(), false)));
            stackOffset += 8;
            argIndex++;
        }

        maxCallStackArgSize = Math.max(maxCallStackArgSize, stackOffset);

        // 调用函数
        target.add(new Call(inst.funcName));

        // 获取返回值
        HighLevelOperand dst = lowerValue(inst.dst);
        target.add(new Move(new Reg(A0, dst.asmType()), dst));
        return null;
    }

    @Override
    public Void visit(TacSignExtend inst) {
        // 符号拓展，使用 move
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);
        target.add(new Move(src.changeAsmType(AsmType.WORD), dst.changeAsmType(AsmType.DWORD)));
        return null;
    }

    @Override
    public Void visit(TacTruncate inst) {
        // 截断至 32 位，使用 move
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);
        target.add(new Move(src.changeAsmType(AsmType.DWORD), dst.changeAsmType(AsmType.WORD)));
        return null;
    }

    private HighLevelOperand lowerValue(TacValue tacValue) {
        if (tacValue instanceof TacConstant tacConstant) {
            return immediate(tacConstant.value);
        } else if (tacValue instanceof TacVariable tacVariable) {
            return pseudo(tacVariable.name);
        }
        throw new UnsupportedOperationException("Unsupported value type: " + tacValue.getClass().getSimpleName());
    }
}
