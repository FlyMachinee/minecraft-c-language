package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.AssignmentOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.PointerType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.StatementVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.*;

import java.util.ArrayList;
import java.util.List;

public final class AstToTacLowerer implements
    StatementVisitor, ExpressionVisitor<AstToTacLowerer.ExpEvalResult>, ExpressionBoolVisitor {

    public AstToTacLowerer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    private final SymbolTable symbolTable;

    public TacProgram lower(ProgramNode program) {
        List<TacTopLevel> topLevels = new ArrayList<>();
        for (ExternalDeclarationNode extDecl : program.extDecls) {
            if (extDecl instanceof FunctionDefinitionNode funcDef) {
                topLevels.add(lowerFunc(funcDef));
            }
        }
        // 遍历符号表，将需要本编译单元内初始化的全局变量进行定义
        for (SymbolTable.Entry entry : symbolTable.getEntries()) {
            SymbolTable.Entry.IdentifierAttr attr = entry.attr;
            if (attr instanceof SymbolTable.Entry.StaticAttr staticAttr) {
                SymbolTable.Entry.StaticAttr.DefinitionType defType = staticAttr.defType;
                if (defType instanceof SymbolTable.Entry.StaticAttr.Defined defined) {
                    topLevels.add(new TacStaticVariable(
                        entry.id.name, staticAttr.global, entry.type, defined.init()));
                } else if (defType instanceof SymbolTable.Entry.StaticAttr.Tentative) {
                    if (entry.type instanceof BasicType bt) {
                        switch (bt.primitive()) {
                            case INT -> topLevels.add(new TacStaticVariable(
                                entry.id.name, staticAttr.global, BasicType.INT, IntInit.ZERO));
                            case LONG -> topLevels.add(new TacStaticVariable(
                                entry.id.name, staticAttr.global, BasicType.LONG, LongInit.ZERO));
                            case UNSIGNED_INT -> topLevels.add(new TacStaticVariable(
                                entry.id.name, staticAttr.global, BasicType.UNSIGNED_INT, UnsignedIntInit.ZERO));
                            case UNSIGNED_LONG -> topLevels.add(new TacStaticVariable(
                                entry.id.name, staticAttr.global, BasicType.UNSIGNED_LONG, UnsignedLongInit.ZERO));
                            case DOUBLE -> topLevels.add(new TacStaticVariable(
                                entry.id.name, staticAttr.global, BasicType.DOUBLE, DoubleInit.ZERO));
                        }
                    } else if (entry.type instanceof PointerType pt) {
                        topLevels.add(new TacStaticVariable(
                            entry.id.name, staticAttr.global, pt, UnsignedLongInit.ZERO));
                    }
                }
            }
        }
        return new TacProgram(topLevels);
    }

    private int tempVarCounter = 0;

    private TacVariable makeTempVar(Type type) {
        String name = "tmp." + (tempVarCounter++);
        TacVariable var = new TacVariable(name);
        symbolTable.put(
            name, new SymbolTable.Entry(
                new IdentifierNode(null, name), null, type,
                SymbolTable.Entry.AutoAttr.INSTANCE));
        return var;
    }

    private int labelCounter = 0;

    private String makeLabel() {
        return "label_" + (labelCounter++);
    }

    private String makeLabel(String prefix) {
        return prefix + "_" + (labelCounter++);
    }

    // 提取变量，避免传参
    private List<TacInstruction> instructions;

    private void emitTac(TacInstruction instruction) {
        instructions.add(instruction);
    }

    private TacFunction lowerFunc(FunctionDefinitionNode funcDef) {
        instructions = new ArrayList<>();
        funcDef.body.accept(this);
        emitTac(new TacReturn(new TacConstant(ConstantInt.ZERO)));
        boolean global = symbolTable.get(funcDef.id.name).attr.isGlobal();
        FunctionTypeNode functionType = (FunctionTypeNode) funcDef.funcType;
        if (functionType.hasNoParameters()) {
            return new TacFunction(funcDef.id.name, global, List.of(), instructions);
        } else {
            List<String> parameters = functionType.params.stream().map(param -> param.name).toList();
            return new TacFunction(funcDef.id.name, global, parameters, instructions);
        }
    }

    private void lowerBlockItem(BlockItemNode blockItem) {
        if (blockItem instanceof StatementBlockItemNode stmt) {
            lowerStatement(stmt.stmt);
        } else if (blockItem instanceof DeclarationBlockItemNode decl) {
            lowerDecl(decl.decl);
        } else {
            throw new RuntimeException("Unknown instruction: " + blockItem.toString());
        }
    }

    private void lowerDecl(DeclarationNode decl) {
        // 一定为块作用域
        // 无存储类且有初始化时，生成初始化三地址码
        if (decl.storageClass == null) {
            for (InitDeclaratorNode initDecl : decl.initDeclarators) {
                if (initDecl.init != null) {
                    TacValue initValue = evalAndLvalueConvert(initDecl.init);
                    emitTac(new TacCopy(initValue, new TacVariable(initDecl.id.name)));
                }
            }
        }
    }

    private void visitBefore(StatementNode stmt) {
        // 为活跃的 goto 标签生成标签
        for (int i = stmt.gotoLabels.size() - 1; i >= 0; i--) {
            StatementNode.GotoLabelInfo info = stmt.gotoLabels.get(i);
            if (info.active) {
                emitTac(new TacLabel(info.label.name));
            }
        }

        // case 和 default
        for (int i = stmt.caseLabels.size() - 1; i >= 0; i--) {
            StatementNode.CaseLabelInfo info = stmt.caseLabels.get(i);
            emitTac(new TacLabel("case_" + info.caseValue + "_" + info.switchLabel));
        }
        if (!stmt.defaultLabels.isEmpty()) {
            StatementNode.DefaultLabelInfo info = stmt.defaultLabels.get(0);
            emitTac(new TacLabel("default_" + info.switchLabel));
        }
    }

    private void lowerStatement(StatementNode stmt) {
        visitBefore(stmt);
        stmt.accept(this);
    }

    private BoolGenResult lowerBoolean(ExpressionNode exp, String jumpTarget, boolean inverse) {
        return exp.accept(this, jumpTarget, inverse);
    }

    @Override
    public void visit(ReturnNode ret) {
        TacValue returnValue = evalAndLvalueConvert(ret.exp);
        emitTac(new TacReturn(returnValue));
    }

    @Override
    public void visit(ExpressionStatementNode expStmt) {
        eval(expStmt.exp);
    }

    @Override
    public void visit(IfStatementNode ifStmt) {
        if (ifStmt.elseStmt != null) {
            // if (cond) thenStmt else elseStmt
            // =>
            // if (!cond) goto else
            // thenStmt
            // goto end
            // else:
            // elseStmt
            // end:
            String labelElse = makeLabel("else");
            switch (lowerBoolean(ifStmt.cond, labelElse, true)) {
                case ALWAYS_JUMP -> {
                    if (!ifStmt.thenStmt.containsActiveLabel()) {
                        // 当 then 子语句没有活跃的 goto 标签，将其优化
                        emitTac(new TacLabel(labelElse));
                        lowerStatement(ifStmt.elseStmt);
                        return;
                    }
                    // 否则生成无条件跳转
                    emitTac(new TacJump(labelElse));
                }
                case NEVER_JUMP -> {
                    if (!ifStmt.elseStmt.containsActiveLabel()) {
                        // 当 else 子语句没有活跃的 goto 标签，将其优化
                        lowerStatement(ifStmt.thenStmt);
                        emitTac(new TacLabel(labelElse));
                        return;
                    }
                }
            }
            String labelEndIf = makeLabel("endif");
            lowerStatement(ifStmt.thenStmt);
            emitTac(new TacJump(labelEndIf));
            emitTac(new TacLabel(labelElse));
            lowerStatement(ifStmt.elseStmt);
            emitTac(new TacLabel(labelEndIf));
        } else {
            // if (cond) thenStmt
            // =>
            // if (!cond) goto end
            // thenStmt
            // end:
            String labelEndIf = makeLabel("endif");
            if (lowerBoolean(ifStmt.cond, labelEndIf, true) == BoolGenResult.ALWAYS_JUMP) {
                if (!ifStmt.thenStmt.containsActiveLabel()) {
                    // 当 then 子语句没有活跃的 goto 标签，将其优化
                    emitTac(new TacLabel(labelEndIf));
                    return;
                }
                // 否则生成无条件跳转
                emitTac(new TacJump(labelEndIf));
            }
            lowerStatement(ifStmt.thenStmt);
            emitTac(new TacLabel(labelEndIf));
        }
    }

    @Override
    public void visit(GotoNode gotoStmt) {
        // 为 goto 语句生成无条件跳转
        emitTac(new TacJump(gotoStmt.target.name));
    }

    @Override
    public void visit(CompoundStatementNode compoundStmt) {
        for (BlockItemNode item : compoundStmt.blockItems) {
            lowerBlockItem(item);
        }
    }

    @Override
    public void visit(BreakNode breakStmt) {
        emitTac(new TacJump("break_" + breakStmt.loopOrSwitchLabel));
    }

    @Override
    public void visit(ContinueNode continueStmt) {
        emitTac(new TacJump("continue_" + continueStmt.loopLabel));
    }

    @Override
    public void visit(WhileLoopNode whileLoop) {
        // while (cond) body
        // =>
        //   if (!cond) goto break_label <=====
        // begin:
        //   body
        // continue_label:
        //   if (cond) goto begin:
        // break_label:

        // do body while (cond);
        // =>
        // begin:
        //   body
        // continue_label:
        //   if (cond) goto begin:
        // break_label:

        String labelBegin = makeLabel(whileLoop.isDoWhile ? "do_while_begin" : "while_begin");
        String labelContinue = "continue_" + whileLoop.loopLabel;
        String labelBreak = "break_" + whileLoop.loopLabel;

        if (!whileLoop.isDoWhile) {
            // while 循环需要在循环前生成跳转到结尾处的条件测试指令
            if (lowerBoolean(whileLoop.cond, labelBreak, true) == BoolGenResult.ALWAYS_JUMP) {
                // 始终跳转，则条件始终为 0
                if (whileLoop.body.containsActiveLabel()) {
                    // 若含有活跃标签，则循环体不能优化，只能生成无条件跳转
                    emitTac(new TacJump(labelBreak));
                    // 继续处理循环体
                } else {
                    // 直接优化掉循环体，返回即可
                    return;
                }
            }
        }
        emitTac(new TacLabel(labelBegin));
        lowerStatement(whileLoop.body);
        emitTac(new TacLabel(labelContinue));
        if (lowerBoolean(whileLoop.cond, labelBegin, false) == BoolGenResult.ALWAYS_JUMP) {
            // 始终跳转，生成无条件跳转
            emitTac(new TacJump(labelBegin));
        }
        emitTac(new TacLabel(labelBreak));
    }

    @Override
    public void visit(ForLoopNode forLoop) {
        // for (init; cond; step) body
        // =>
        //   init
        //   if (!cond) goto break_label
        // begin:
        //   body
        // continue_label:
        //   step
        //   if (cond) goto begin
        // break_label:
        String labelBegin = makeLabel("for_begin");
        String labelContinue = "continue_" + forLoop.loopLabel;
        String labelBreak = "break_" + forLoop.loopLabel;

        if (forLoop.init != null) {
            if (forLoop.init instanceof ForInitDeclarationNode decl) {
                lowerDecl(decl.decl);
            } else if (forLoop.init instanceof ForInitExpressionNode expr) {
                eval(expr.exp);
            } else {
                throw new RuntimeException("unexpected init node in for loop: " + forLoop.init.getClass());
            }
        }
        if (forLoop.cond != null) {
            if (lowerBoolean(forLoop.cond, labelBreak, true) == BoolGenResult.ALWAYS_JUMP) {
                // 始终跳转，则条件始终为 0
                if (forLoop.body.containsActiveLabel()) {
                    // 若含有活跃标签，则循环体不能优化，只能生成无条件跳转
                    emitTac(new TacJump(labelBreak));
                    // 继续处理循环体
                } else {
                    // 直接优化掉循环体，返回即可
                    return;
                }
            }
        } // 否则条件缺省，始终视为真，则不跳转
        emitTac(new TacLabel(labelBegin));
        lowerStatement(forLoop.body);
        emitTac(new TacLabel(labelContinue));
        if (forLoop.step != null) {
            eval(forLoop.step);
        }
        if (forLoop.cond != null) {
            if (lowerBoolean(forLoop.cond, labelBegin, false) == BoolGenResult.ALWAYS_JUMP) {
                // 始终跳转，生成无条件跳转
                emitTac(new TacJump(labelBegin));
            }
        } else {
            // 条件缺省，视为始终为真
            emitTac(new TacJump(labelBegin));
        }
        emitTac(new TacLabel(labelBreak));
    }

    @Override
    public void visit(SwitchStatementNode switchStmt) {
        // switch (exp) body   {case: [0, 1, 2, ...], default=yes/no}
        // =>
        //   tmp = exp
        //   if (tmp == 0) goto case0
        //   if (tmp == 1) goto case1
        //   goto default (if default=yes)
        //   goto break (if default=no)
        //   body
        // break:
        String labelBreak = "break_" + switchStmt.switchLabel;
        String defaultLabel = "default_" + switchStmt.switchLabel;

        TacValue res = evalAndLvalueConvert(switchStmt.exp);
        if (res instanceof TacConstant constant) {
            // 常量，进行优化
            long value = constant.value.toLong().value();
            if (switchStmt.caseValues.containsKey(value)) {
                // 匹配到 case 标签
                emitTac(new TacJump("case_" + value + "_" + switchStmt.switchLabel));
            } else if (switchStmt.defaultLabel != null) {
                // 没有匹配到 case 标签但有 default 标签
                emitTac(new TacJump(defaultLabel));
            } else {
                // 没有匹配到 case 标签且没有 default 标签，直接跳转到 break

                // 如果此时 body 没有可能跳入的 goto 标签，则可以优化掉 body 和 break 标签
                if (!switchStmt.body.containsActiveLabel()) {
                    return;
                }
                emitTac(new TacJump(labelBreak));
            }
        } else {
            // 非常量，生成比较指令
            for (SwitchStatementNode.CaseLabelInfo caseInfo : switchStmt.caseValues.values()) {
                String caseLabel = "case_" + caseInfo.caseValue + "_" + switchStmt.switchLabel;
                TacValue caseValue = new TacConstant(((ConstantNode) caseInfo.caseValue).value);
                emitTac(new TacJumpIfComparison(Comparison.EQUAL, res, caseValue, caseLabel, false));
            }
            if (switchStmt.defaultLabel != null) {
                emitTac(new TacJump(defaultLabel));
            } else {
                emitTac(new TacJump(labelBreak));
            }
        }
        lowerStatement(switchStmt.body);
        emitTac(new TacLabel(labelBreak));

    }

    @Override
    public void visit(NullStatementNode nullStmt) {
    }

    public sealed interface ExpEvalResult { }

    record PlainOperand(TacValue object) implements ExpEvalResult {
        public PlainOperand(Constant constant) {
            this(new TacConstant(constant));
        }
    }

    record DereferencedPointer(TacValue pointer) implements ExpEvalResult { }

    private ExpEvalResult eval(ExpressionNode exp) {
        return exp.accept(this);
    }

    private TacValue evalAndLvalueConvert(ExpressionNode exp) {
        ExpEvalResult res = eval(exp);
        if (res instanceof PlainOperand plainRes) {
            return plainRes.object();
        }
        if (res instanceof DereferencedPointer derefPtr) {
            TacVariable obj = makeTempVar(exp.expType);
            emitTac(new TacLoad(derefPtr.pointer(), obj));
            return obj;
        }
        throw new IllegalStateException("control should never reach here");
    }

    @Override
    public ExpEvalResult visit(ConstantNode constant) {
        return new PlainOperand(constant.value);
    }

    @Override
    public ExpEvalResult visit(UnaryExpressionNode unaryExp) {
        TacValue src = evalAndLvalueConvert(unaryExp.exp);
        if (src instanceof TacConstant constant) {
            return new PlainOperand(constant.value.apply(unaryExp.op.op));
        }
        TacVariable dst = makeTempVar(unaryExp.expType);
        emitTac(new TacUnaryOperation(unaryExp.op.op, src, dst));
        return new PlainOperand(dst);
    }

    @Override
    public ExpEvalResult visit(BinaryExpressionNode binaryExp) {
        // 短路求值
        switch (binaryExp.op.op) {
            case LOGICAL_AND -> {
                // 短路与求值
                // if (a && b) yield 1; else yield 0;
                // =>
                // if (!a) goto zero
                // if (!b) goto zero
                // tmp = 1
                // goto end
                // zero:
                // tmp = 0
                // end:
                // yield tmp
                String labelFalse = makeLabel("and_false");

                // 注意短路语义，即使右操作数为0，左操作数也要求值
                // 显然左操作数永远都需要求值
                switch (lowerBoolean(binaryExp.lhs, labelFalse, true)) {
                    case VARIOUS -> {
                        // a 未知
                        if (lowerBoolean(binaryExp.rhs, labelFalse, true) == BoolGenResult.ALWAYS_JUMP) {
                            // if (!b) goto zero 始终跳转，即 b=0
                            // 推导出值为0
                            emitTac(new TacLabel(labelFalse));
                            return new PlainOperand(ConstantInt.ZERO);
                        }
                        // 其他情况都不能断言结果值
                    }
                    case ALWAYS_JUMP -> {
                        // if (!a) goto zero 始终跳转，即 a=0
                        // 显然值为0，由于短路语义，右操作数永远不求值，可以优化
                        emitTac(new TacLabel(labelFalse));
                        return new PlainOperand(ConstantInt.ZERO);
                    }
                    case NEVER_JUMP -> {
                        // if (!a) goto zero 永不跳转，即 a=1
                        // 得继续求值
                        switch (lowerBoolean(binaryExp.rhs, labelFalse, true)) {
                            case ALWAYS_JUMP -> {
                                // b=0 => 推导值为0
                                emitTac(new TacLabel(labelFalse));
                                return new PlainOperand(ConstantInt.ZERO);
                            }
                            case NEVER_JUMP -> {
                                // b=1 => 推导值为1
                                emitTac(new TacLabel(labelFalse));
                                return new PlainOperand(ConstantInt.ONE);
                            }
                        }
                        // b 未知，无法断言
                    }
                }

                // 无法断言的情况，需要生成指令来进行求值
                String labelEvalEnd = makeLabel("eval_end");
                TacVariable dst = makeTempVar(binaryExp.expType);
                emitTac(new TacCopy(new TacConstant(ConstantInt.ONE), dst));
                emitTac(new TacJump(labelEvalEnd));
                emitTac(new TacLabel(labelFalse));
                emitTac(new TacCopy(new TacConstant(ConstantInt.ZERO), dst));
                emitTac(new TacLabel(labelEvalEnd));
                return new PlainOperand(dst);
            }
            case LOGICAL_OR -> {
                // 短路或求值
                // if (a || b) yield 1; else yield 0;
                // =>
                // if (a) goto one
                // if (b) goto one
                // tmp = 0
                // goto end
                // one:
                // tmp = 1
                // end:
                // yield tmp
                String labelTrue = makeLabel("or_true");

                // 注意短路语义，即使右操作数为1，左操作数也要求值
                // 显然左操作数永远都需要求值
                switch (lowerBoolean(binaryExp.lhs, labelTrue, false)) {
                    case VARIOUS -> {
                        // a 未知
                        if (lowerBoolean(binaryExp.rhs, labelTrue, false) == BoolGenResult.ALWAYS_JUMP) {
                            // if (b) goto one 始终跳转，即 b=1
                            // 推导出值为0
                            emitTac(new TacLabel(labelTrue));
                            return new PlainOperand(ConstantInt.ONE);
                        }
                        // 其他情况都不能断言结果值
                    }
                    case ALWAYS_JUMP -> {
                        // if (a) goto one 始终跳转，即 a=1
                        // 显然值为1，由于短路语义，右操作数永远不求值，可以优化
                        emitTac(new TacLabel(labelTrue));
                        return new PlainOperand(ConstantInt.ONE);
                    }
                    case NEVER_JUMP -> {
                        // if (a) goto one 永不跳转，即 a=0
                        // 得继续求值
                        switch (lowerBoolean(binaryExp.rhs, labelTrue, false)) {
                            case ALWAYS_JUMP -> {
                                // b=1 => 推导值为1
                                emitTac(new TacLabel(labelTrue));
                                return new PlainOperand(ConstantInt.ONE);
                            }
                            case NEVER_JUMP -> {
                                // b=0 => 推导值为0
                                emitTac(new TacLabel(labelTrue));
                                return new PlainOperand(ConstantInt.ZERO);
                            }
                        }
                        // b 未知，无法断言
                    }
                }

                // 无法断言的情况，需要生成指令来进行求值
                String labelEvalEnd = makeLabel("eval_end");
                TacVariable dst = makeTempVar(binaryExp.expType);
                emitTac(new TacCopy(new TacConstant(ConstantInt.ZERO), dst));
                emitTac(new TacJump(labelEvalEnd));
                emitTac(new TacLabel(labelTrue));
                emitTac(new TacCopy(new TacConstant(ConstantInt.ONE), dst));
                emitTac(new TacLabel(labelEvalEnd));
                return new PlainOperand(dst);
            }
        }
        // 普通求值
        TacValue lhs = evalAndLvalueConvert(binaryExp.lhs);
        TacValue rhs = evalAndLvalueConvert(binaryExp.rhs);
        if (lhs instanceof TacConstant lhsConst && rhs instanceof TacConstant rhsConst) {
            Constant reduced = lhsConst.value.apply(binaryExp.op.op, rhsConst.value);
            return new PlainOperand(reduced);
        }
        TacVariable dst = makeTempVar(binaryExp.expType);
        emitTac(new TacBinaryOperation(binaryExp.op.op, lhs, rhs, dst));
        return new PlainOperand(dst);
    }

    @Override
    public ExpEvalResult visit(AssignmentNode assignment) {
        // 赋值表达式
        TacValue rhs = evalAndLvalueConvert(assignment.rhs);
        ExpEvalResult dst = eval(assignment.lhs);
        if (assignment.op.op == AssignmentOperator.ASSIGN) {
            // 普通赋值
            // 已经在类型检查中进行了可能的 cast 了
            if (dst instanceof PlainOperand objDst) {
                emitTac(new TacCopy(rhs, objDst.object()));
                if (rhs instanceof TacConstant constant) {
                    return new PlainOperand(constant);
                } else {
                    return dst;
                }
            } else if (dst instanceof DereferencedPointer derefPointer) {
                emitTac(new TacStore(rhs, derefPointer.pointer()));
                return new PlainOperand(rhs);
            } else {
                throw new IllegalStateException("Control should never reach here");
            }
        } else {
            // 复合赋值
            TypeCheckingPass.TypeCheckCompoundAssignmentResult checkRes =
                TypeCheckingPass.typeCheckCompoundAssignment(assignment);
            // 表达式 lhs @= rhs 与 lhs = lhs @ (rhs) 完全相同，但只求值一次 lhs
            if (dst instanceof PlainOperand objDst) {
                // 只可能是 Variable
                TacVariable varDst = (TacVariable) objDst.object();
                // 进行可能的 cast
                TacValue lhsCastRes = cast(varDst, checkRes.lhsTargetType(), assignment.lhs.expType);
                TacValue rhsCastRes = cast(rhs, checkRes.rhsTargetType(), assignment.rhs.expType);
                // 发生计算
                TacVariable tmp = makeTempVar(checkRes.tmpType());
                emitTac(new TacBinaryOperation(assignment.op.op.toBinaryOperator(), lhsCastRes, rhsCastRes, tmp));
                // 结果进行 cast
                TacValue castRes = cast(tmp, assignment.expType, checkRes.tmpType());
                // 写回
                emitTac(new TacCopy(castRes, varDst));
                return objDst;
            }
            if (dst instanceof DereferencedPointer derefPtr) {
                // *ptr ?= rhs => *ptr = *ptr ? rhs
                // 保证指针只求值一次
                TacValue ptr = derefPtr.pointer();
                // 加载 *ptr
                TacVariable loadRes = makeTempVar(assignment.lhs.expType);
                emitTac(new TacLoad(ptr, loadRes));
                // 进行可能的 cast
                TacValue lhsCastRes = cast(loadRes, checkRes.lhsTargetType(), assignment.lhs.expType);
                TacValue rhsCastRes = cast(rhs, checkRes.rhsTargetType(), assignment.rhs.expType);
                // 发生计算
                TacVariable tmp = makeTempVar(checkRes.tmpType());
                emitTac(new TacBinaryOperation(assignment.op.op.toBinaryOperator(), lhsCastRes, rhsCastRes, tmp));
                // 结果进行 cast
                TacValue castRes = cast(tmp, assignment.expType, checkRes.tmpType());
                // 写回
                emitTac(new TacStore(castRes, ptr));
                return new PlainOperand(castRes);
            }
            throw new IllegalStateException("Should be adjust to plain assignment earlier");
        }
    }

    @Override
    public ExpEvalResult visit(VariableNode variable) {
        return new PlainOperand(new TacVariable(variable));
    }

    @Override
    public ExpEvalResult visit(IncrementDecrementNode incrementDecrement) {
        // 自增自减表达式
        BinaryOperator op = incrementDecrement.isIncrement ? BinaryOperator.ADD : BinaryOperator.SUBTRACT;
        ExpEvalResult dst = eval(incrementDecrement.operand);

        BasicType bt = (BasicType) incrementDecrement.expType;
        TacConstant one = new TacConstant(switch (bt.primitive()) {
            case INT -> ConstantInt.ONE;
            case LONG -> ConstantLong.ONE;
            case UNSIGNED_INT -> ConstantUnsignedInt.ONE;
            case UNSIGNED_LONG -> ConstantUnsignedLong.ONE;
            case DOUBLE -> ConstantDouble.ONE;
        });

        if (incrementDecrement.isPrefix) {
            if (dst instanceof PlainOperand objDst) {
                // ++/--a => a = a +/- 1; yield a;
                emitTac(new TacBinaryOperation(op, objDst.object(), one, objDst.object()));
                return dst;
            }
            if (dst instanceof DereferencedPointer derefPointer) {
                // ++/--(*ptr)
                // *ptr = *ptr +/- 1; yield *ptr;
                // tmp = *ptr; tmp = tmp +/- 1; *ptr = tmp; yield tmp;
                TacVariable tmp = makeTempVar(incrementDecrement.expType);
                emitTac(new TacLoad(derefPointer.pointer(), tmp));
                emitTac(new TacBinaryOperation(op, tmp, one, tmp));
                emitTac(new TacStore(tmp, derefPointer.pointer()));
                return new PlainOperand(tmp);
            }
        } else {
            if (dst instanceof PlainOperand objDst) {
                // a++/-- => temp = a; a = a +/- 1; yield temp;
                TacVariable temp = makeTempVar(incrementDecrement.expType);
                emitTac(new TacCopy(objDst.object(), temp));
                emitTac(new TacBinaryOperation(op, objDst.object(), one, objDst.object()));
                return new PlainOperand(temp);
            }
            if (dst instanceof DereferencedPointer derefPointer) {
                // (*ptr)++/--
                // old = *ptr; *ptr = old +/- 1; yield old;
                // tmp = *ptr; old = tmp; tmp = tmp +/- 1; *ptr = tmp; yield old;
                TacVariable tmp = makeTempVar(incrementDecrement.expType);
                TacVariable old = makeTempVar(incrementDecrement.expType);
                emitTac(new TacLoad(derefPointer.pointer(), tmp));
                emitTac(new TacCopy(tmp, old));
                emitTac(new TacBinaryOperation(op, tmp, one, tmp));
                emitTac(new TacStore(tmp, derefPointer.pointer()));
                return new PlainOperand(old);
            }
        }
        throw new IllegalStateException("Control should never reach here");
    }

    @Override
    public ExpEvalResult visit(ConditionalExpressionNode condExp) {
        // 条件表达式
        // cond ? a : b
        // =>
        // if (!cond) goto false
        // tmp = a
        // goto end
        // false:
        // tmp = b
        // end:
        // yield tmp
        String labelCondFalse = makeLabel("cond_false");

        // 条件表达式也有求值顺序要求，先求条件值
        switch (lowerBoolean(condExp.cond, labelCondFalse, true)) {
            case ALWAYS_JUMP -> {
                // cond=0，只需求假分支即可
                emitTac(new TacLabel(labelCondFalse));
                return eval(condExp.elseExp);
            }
            case NEVER_JUMP -> {
                // cond=1，只需求真分支即可
                ExpEvalResult ret = eval(condExp.thenExp);
                emitTac(new TacLabel(labelCondFalse));
                return ret;
            }
        }
        String labelCondEnd = makeLabel("cond_end");
        TacValue thenValue = evalAndLvalueConvert(condExp.thenExp);
        TacVariable dst = makeTempVar(condExp.expType);
        emitTac(new TacCopy(thenValue, dst));
        emitTac(new TacJump(labelCondEnd));
        emitTac(new TacLabel(labelCondFalse));
        TacValue elseValue = evalAndLvalueConvert(condExp.elseExp);
        emitTac(new TacCopy(elseValue, dst));
        emitTac(new TacLabel(labelCondEnd));
        return new PlainOperand(dst);
    }

    @Override
    public ExpEvalResult visit(FunctionCallNode funcCall) {
        // 函数调用
        // func(arg0, arg1, ...)
        // =>
        // res0 = <eval arg0>
        // res1 = <eval arg1>
        // ...
        // dst = invoke(func, [res0, res1,...])
        // yield dst

        VariableNode funcId = (VariableNode) funcCall.func;
        List<TacValue> args = new ArrayList<>();
        for (ExpressionNode arg : funcCall.args) {
            args.add(evalAndLvalueConvert(arg));
        }
        TacVariable dst = makeTempVar(funcCall.expType);
        emitTac(new TacFunctionCall(funcId.id.name, args, dst));
        return new PlainOperand(dst);
    }

    private TacValue cast(TacValue toCast, Type targetType, Type originType) {
        if (targetType.isCompatible(originType)) {
            return toCast;
        }
        if (toCast instanceof TacConstant constant) {
            return new TacConstant(constant.value.castTo(targetType));
        }
        TacVariable dst = makeTempVar(targetType);

        BasicType targetBasic = targetType instanceof BasicType ? (BasicType) targetType : BasicType.UNSIGNED_LONG;
        BasicType originBasic = originType instanceof BasicType ? (BasicType) originType : BasicType.UNSIGNED_LONG;

        if (targetBasic == BasicType.DOUBLE) {
            switch (originBasic.primitive()) {
                case INT, LONG -> emitTac(new TacIntToDouble(toCast, dst));
                case UNSIGNED_INT, UNSIGNED_LONG -> emitTac(new TacUnsignedIntToDouble(toCast, dst));
                default -> throw new IllegalStateException("Unexpected value: " + originBasic);
            }
            return dst;
        }
        if (originBasic == BasicType.DOUBLE) {
            switch (targetBasic.primitive()) {
                case INT, LONG -> emitTac(new TacDoubleToInt(toCast, dst));
                case UNSIGNED_INT, UNSIGNED_LONG -> emitTac(new TacDoubleToUnsignedInt(toCast, dst));
                default -> throw new IllegalStateException("Unexpected value: " + targetBasic);
            }
            return dst;
        }

        if (targetBasic.sizeof() == originBasic.sizeof()) {
            emitTac(new TacCopy(toCast, dst));
        } else if (targetBasic.sizeof() < originBasic.sizeof()) {
            emitTac(new TacTruncate(toCast, dst));
        } else if (originBasic.isSigned()) {
            emitTac(new TacSignExtend(toCast, dst));
        } else {
            emitTac(new TacZeroExtend(toCast, dst));
        }
        return dst;
    }

    @Override
    public ExpEvalResult visit(CastExpressionNode castExp) {
        TacValue toCast = evalAndLvalueConvert(castExp.exp);
        Type targetType = castExp.targetType.getType();
        Type originType = castExp.exp.expType;

        return new PlainOperand(cast(toCast, targetType, originType));
    }

    @Override
    public ExpEvalResult visit(AddressOfNode addrOf) {
        ExpEvalResult exp = eval(addrOf.exp);
        if (exp instanceof PlainOperand expObj) {
            TacVariable ptr = makeTempVar(addrOf.expType);
            emitTac(new TacGetAddress(expObj.object(), ptr));
            return new PlainOperand(ptr);
        }
        if (exp instanceof DereferencedPointer expPtr) {
            return new PlainOperand(expPtr.pointer());
        }
        throw new IllegalStateException("Control should never reach here");
    }

    @Override
    public ExpEvalResult visit(DereferenceNode deref) {
        TacValue exp = evalAndLvalueConvert(deref.exp);
        return new DereferencedPointer(exp);
    }

    @Override
    public BoolGenResult visit(ConstantNode constant, String jumpTarget, boolean inverse) {
        // 常量，生成无条件跳转
        if ((!constant.value.isZero()) ^ inverse) {
            return BoolGenResult.ALWAYS_JUMP;
        } else {
            return BoolGenResult.NEVER_JUMP;
        }
    }

    @Override
    public BoolGenResult visit(BinaryExpressionNode binaryExp, String jumpTarget, boolean inverse) {
        switch (binaryExp.op.op) {
            case LOGICAL_AND -> {
                if (inverse) {
                    // if (!(a && b)) jump => if (!a) jump ; if (!b) jump
                    switch (lowerBoolean(binaryExp.lhs, jumpTarget, true)) {
                        case VARIOUS -> {
                            // a 未知
                            if (lowerBoolean(binaryExp.rhs, jumpTarget, true) == BoolGenResult.ALWAYS_JUMP) {
                                // b=0 => !(a && b) = 1
                                return BoolGenResult.ALWAYS_JUMP;
                            } else {
                                // 无法断言
                                return BoolGenResult.VARIOUS;
                            }
                        }
                        case ALWAYS_JUMP -> {
                            // a=0 => !(a && b) = 1
                            return BoolGenResult.ALWAYS_JUMP;
                        }
                        case NEVER_JUMP -> {
                            // a=1 => if (!b) jump;
                            return lowerBoolean(binaryExp.rhs, jumpTarget, true);
                        }
                    }
                } else {
                    // if (a && b) jump => if (!a) jump false ; if (b) jump; false:
                    String label = makeLabel("and_false");
                    BoolGenResult ret = BoolGenResult.VARIOUS;
                    switch (lowerBoolean(binaryExp.lhs, label, true)) {
                        case VARIOUS -> {
                            // a 未知
                            switch (lowerBoolean(binaryExp.rhs, jumpTarget, false)) {
                                case NEVER_JUMP -> {
                                    // b=0 => a && b = 0
                                    ret = BoolGenResult.NEVER_JUMP;
                                }
                                case ALWAYS_JUMP -> {
                                    // b=1
                                    emitTac(new TacJump(jumpTarget));
                                }
                            }
                        }
                        case ALWAYS_JUMP -> {
                            // a=0 => a && b = 0
                            ret = BoolGenResult.NEVER_JUMP;
                        }
                        case NEVER_JUMP -> {
                            // a=1 => if (b) jump;
                            ret = lowerBoolean(binaryExp.rhs, jumpTarget, false);
                        }
                    }
                    // 保证标签有定义
                    emitTac(new TacLabel(label));
                    return ret;
                }
            }
            case LOGICAL_OR -> {
                if (inverse) {
                    // if (!(a || b)) jump => if (a) jump false ; if (!b) jump; false:
                    String label = makeLabel("or_false");
                    BoolGenResult ret = BoolGenResult.VARIOUS;
                    switch (lowerBoolean(binaryExp.lhs, label, false)) {
                        case VARIOUS -> {
                            // a 未知
                            switch (lowerBoolean(binaryExp.rhs, jumpTarget, true)) {
                                case NEVER_JUMP -> {
                                    // b=1 => !(a || b) = 0
                                    ret = BoolGenResult.NEVER_JUMP;
                                }
                                case ALWAYS_JUMP -> {
                                    // b=0
                                    emitTac(new TacJump(jumpTarget));
                                }
                            }
                        }
                        case ALWAYS_JUMP -> {
                            // a=1 => !(a || b) = 0
                            ret = BoolGenResult.NEVER_JUMP;
                        }
                        case NEVER_JUMP -> {
                            // a=0 => if (!b) jump;
                            ret = lowerBoolean(binaryExp.rhs, jumpTarget, true);
                        }
                    }
                    // 保证标签有定义
                    emitTac(new TacLabel(label));
                    return ret;
                } else {
                    // if (a || b) jump => if (a) jump ; if (b) jump
                    switch (lowerBoolean(binaryExp.lhs, jumpTarget, false)) {
                        case VARIOUS -> {
                            // a 未知
                            if (lowerBoolean(binaryExp.rhs, jumpTarget, false) == BoolGenResult.ALWAYS_JUMP) {
                                // b=1 => a || b = 1
                                return BoolGenResult.ALWAYS_JUMP;
                            } else {
                                // 无法断言
                                return BoolGenResult.VARIOUS;
                            }
                        }
                        case ALWAYS_JUMP -> {
                            // a=1 => a || b = 1
                            return BoolGenResult.ALWAYS_JUMP;
                        }
                        case NEVER_JUMP -> {
                            // a=0 => if (b) jump;
                            return lowerBoolean(binaryExp.rhs, jumpTarget, false);
                        }
                    }
                }
            }
            case EQUAL, NOT_EQUAL, LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL -> {
                // 直接生成比较跳转指令，而不是比较置位指令
                Comparison cond = binaryExp.op.op.toComparison();

                TacValue lhs = evalAndLvalueConvert(binaryExp.lhs);
                TacValue rhs = evalAndLvalueConvert(binaryExp.rhs);
                if (lhs instanceof TacConstant lhsConst && rhs instanceof TacConstant rhsConst) {
                    if (lhsConst.value.apply(cond, rhsConst.value).isZero() == inverse) {
                        return BoolGenResult.ALWAYS_JUMP;
                    } else {
                        return BoolGenResult.NEVER_JUMP;
                    }
                }
                emitTac(new TacJumpIfComparison(cond, lhs, rhs, jumpTarget, inverse));
                return BoolGenResult.VARIOUS;
            }
        }
        return visitFallback(binaryExp, jumpTarget, inverse);
    }


    @Override
    public BoolGenResult visit(UnaryExpressionNode unaryExp, String jumpTarget, boolean inverse) {
        if (unaryExp.op.op == UnaryOperator.NOT) {
            return unaryExp.exp.accept(this, jumpTarget, !inverse);
        }
        return visitFallback(unaryExp, jumpTarget, inverse);
    }

    private BoolGenResult visitFallback(ExpressionNode exp, String jumpTarget, boolean inverse) {
        // 其他表达式，先求值再与 0 比较跳转
        TacValue value = evalAndLvalueConvert(exp);
        if (value instanceof TacConstant constant) {
            if (constant.value.isZero() == inverse) {
                return BoolGenResult.ALWAYS_JUMP;
            } else {
                return BoolGenResult.NEVER_JUMP;
            }
        }
        if (inverse) {
            emitTac(new TacJumpIfZero(value, jumpTarget));
        } else {
            emitTac(new TacJumpIfNotZero(value, jumpTarget));
        }
        return BoolGenResult.VARIOUS;
    }

    @Override
    public BoolGenResult visit(VariableNode variable, String jumpTarget, boolean inverse) {
        return visitFallback(variable, jumpTarget, inverse);
    }

    @Override
    public BoolGenResult visit(AssignmentNode assignment, String jumpTarget, boolean inverse) {
        return visitFallback(assignment, jumpTarget, inverse);
    }

    @Override
    public BoolGenResult visit(IncrementDecrementNode incrementDecrement, String jumpTarget, boolean inverse) {
        return visitFallback(incrementDecrement, jumpTarget, inverse);
    }

    @Override
    public BoolGenResult visit(ConditionalExpressionNode condExp, String jumpTarget, boolean inverse) {
        return visitFallback(condExp, jumpTarget, inverse);
    }

    @Override
    public BoolGenResult visit(FunctionCallNode funcCall, String jumpTarget, boolean inverse) {
        return visitFallback(funcCall, jumpTarget, inverse);
    }

    @Override
    public BoolGenResult visit(CastExpressionNode castExp, String jumpTarget, boolean inverse) {
        return visitFallback(castExp, jumpTarget, inverse);
    }

    @Override
    public BoolGenResult visit(AddressOfNode addrOf, String jumpTarget, boolean inverse) {
        return visitFallback(addrOf, jumpTarget, inverse);
    }

    @Override
    public BoolGenResult visit(DereferenceNode deref, String jumpTarget, boolean inverse) {
        return visitFallback(deref, jumpTarget, inverse);
    }
}
