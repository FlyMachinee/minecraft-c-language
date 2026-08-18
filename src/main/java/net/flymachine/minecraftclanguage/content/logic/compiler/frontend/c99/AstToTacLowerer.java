package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.AssignmentOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.StatementVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.*;

import java.util.ArrayList;
import java.util.List;

public final class AstToTacLowerer implements StatementVisitor, ExpressionVisitor, ExpressionBoolVisitor {

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
                    BasicType bt = (BasicType) entry.type;
                    switch (bt) {
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
                        default -> throw new IllegalStateException("Unexpected value: " + bt);
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
            stmt.stmt.accept(this);
        } else if (blockItem instanceof DeclarationBlockItemNode decl) {
            lowerDecl(decl.decl);
        } else {
            throw new RuntimeException("Unknown instruction: " + blockItem.toString());
        }
    }

    private void lowerDecl(DeclarationNode decl) {
        // 一定为块作用域
        // 无存储类且有初始化时，生成初始化三地址码
        if (decl.init != null && decl.storageClass == null) {
            TacValue initValue = decl.init.accept(this);
            emitTac(new TacCopy(initValue, new TacVariable(decl.id.name)));
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

    @Override
    public void visit(ReturnNode ret) {
        visitBefore(ret);
        TacValue returnValue = ret.exp.accept(this);
        emitTac(new TacReturn(returnValue));
    }

    @Override
    public void visit(ExpressionStatementNode expStmt) {
        visitBefore(expStmt);
        expStmt.exp.accept(this);
    }

    @Override
    public void visit(IfStatementNode ifStmt) {
        visitBefore(ifStmt);
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
            switch (ifStmt.cond.accept(this, labelElse, true)) {
                case ALWAYS_JUMP -> {
                    if (!ifStmt.thenStmt.containsActiveLabel()) {
                        // 当 then 子语句没有活跃的 goto 标签，将其优化
                        emitTac(new TacLabel(labelElse));
                        ifStmt.elseStmt.accept(this);
                        return;
                    }
                    // 否则生成无条件跳转
                    emitTac(new TacJump(labelElse));
                }
                case NEVER_JUMP -> {
                    if (!ifStmt.elseStmt.containsActiveLabel()) {
                        // 当 else 子语句没有活跃的 goto 标签，将其优化
                        ifStmt.thenStmt.accept(this);
                        emitTac(new TacLabel(labelElse));
                        return;
                    }
                }
            }
            String labelEndIf = makeLabel("endif");
            ifStmt.thenStmt.accept(this);
            emitTac(new TacJump(labelEndIf));
            emitTac(new TacLabel(labelElse));
            ifStmt.elseStmt.accept(this);
            emitTac(new TacLabel(labelEndIf));
        } else {
            // if (cond) thenStmt
            // =>
            // if (!cond) goto end
            // thenStmt
            // end:
            String labelEndIf = makeLabel("endif");
            if (ifStmt.cond.accept(this, labelEndIf, true) == BoolGenResult.ALWAYS_JUMP) {
                if (!ifStmt.thenStmt.containsActiveLabel()) {
                    // 当 then 子语句没有活跃的 goto 标签，将其优化
                    emitTac(new TacLabel(labelEndIf));
                    return;
                }
                // 否则生成无条件跳转
                emitTac(new TacJump(labelEndIf));
            }
            ifStmt.thenStmt.accept(this);
            emitTac(new TacLabel(labelEndIf));
        }
    }

    @Override
    public void visit(GotoNode gotoStmt) {
        visitBefore(gotoStmt);
        // 为 goto 语句生成无条件跳转
        emitTac(new TacJump(gotoStmt.target.name));
    }

    @Override
    public void visit(CompoundStatementNode compoundStmt) {
        visitBefore(compoundStmt);
        for (BlockItemNode item : compoundStmt.blockItems) {
            lowerBlockItem(item);
        }
    }

    @Override
    public void visit(BreakNode breakStmt) {
        visitBefore(breakStmt);
        emitTac(new TacJump("break_" + breakStmt.loopOrSwitchLabel));
    }

    @Override
    public void visit(ContinueNode continueStmt) {
        visitBefore(continueStmt);
        emitTac(new TacJump("continue_" + continueStmt.loopLabel));
    }

    @Override
    public void visit(WhileLoopNode whileLoop) {
        visitBefore(whileLoop);
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
            if (whileLoop.cond.accept(this, labelBreak, true) == BoolGenResult.ALWAYS_JUMP) {
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
        whileLoop.body.accept(this);
        emitTac(new TacLabel(labelContinue));
        if (whileLoop.cond.accept(this, labelBegin, false) == BoolGenResult.ALWAYS_JUMP) {
            // 始终跳转，生成无条件跳转
            emitTac(new TacJump(labelBegin));
        }
        emitTac(new TacLabel(labelBreak));
    }

    @Override
    public void visit(ForLoopNode forLoop) {
        visitBefore(forLoop);
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
                expr.exp.accept(this);
            } else {
                throw new RuntimeException("unexpected init node in for loop: " + forLoop.init.getClass());
            }
        }
        if (forLoop.cond != null) {
            if (forLoop.cond.accept(this, labelBreak, true) == BoolGenResult.ALWAYS_JUMP) {
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
        forLoop.body.accept(this);
        emitTac(new TacLabel(labelContinue));
        if (forLoop.step != null) {
            forLoop.step.accept(this);
        }
        if (forLoop.cond != null) {
            if (forLoop.cond.accept(this, labelBegin, false) == BoolGenResult.ALWAYS_JUMP) {
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
        visitBefore(switchStmt);
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

        TacValue res = switchStmt.exp.accept(this);
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
        switchStmt.body.accept(this);
        emitTac(new TacLabel(labelBreak));

    }

    @Override
    public void visit(NullStatementNode nullStmt) {
        visitBefore(nullStmt);
    }

    @Override
    public TacValue visit(ConstantNode constant) {
        return new TacConstant(constant.value);
    }

    @Override
    public TacValue visit(UnaryExpressionNode unaryExp) {
        TacValue src = unaryExp.exp.accept(this);
        if (src instanceof TacConstant constant) {
            return new TacConstant(constant.value.apply(unaryExp.op.op));
        }
        TacVariable dst = makeTempVar(unaryExp.expType);
        emitTac(new TacUnaryOperation(unaryExp.op.op, src, dst));
        return dst;
    }

    @Override
    public TacValue visit(BinaryExpressionNode binaryExp) {
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
                switch (binaryExp.lhs.accept(this, labelFalse, true)) {
                    case VARIOUS -> {
                        // a 未知
                        if (binaryExp.rhs.accept(this, labelFalse, true) == BoolGenResult.ALWAYS_JUMP) {
                            // if (!b) goto zero 始终跳转，即 b=0
                            // 推导出值为0
                            emitTac(new TacLabel(labelFalse));
                            return new TacConstant(ConstantInt.ZERO);
                        }
                        // 其他情况都不能断言结果值
                    }
                    case ALWAYS_JUMP -> {
                        // if (!a) goto zero 始终跳转，即 a=0
                        // 显然值为0，由于短路语义，右操作数永远不求值，可以优化
                        emitTac(new TacLabel(labelFalse));
                        return new TacConstant(ConstantInt.ZERO);
                    }
                    case NEVER_JUMP -> {
                        // if (!a) goto zero 永不跳转，即 a=1
                        // 得继续求值
                        switch (binaryExp.rhs.accept(this, labelFalse, true)) {
                            case ALWAYS_JUMP -> {
                                // b=0 => 推导值为0
                                emitTac(new TacLabel(labelFalse));
                                return new TacConstant(ConstantInt.ZERO);
                            }
                            case NEVER_JUMP -> {
                                // b=1 => 推导值为1
                                emitTac(new TacLabel(labelFalse));
                                return new TacConstant(ConstantInt.ONE);
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
                return dst;
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
                switch (binaryExp.lhs.accept(this, labelTrue, false)) {
                    case VARIOUS -> {
                        // a 未知
                        if (binaryExp.rhs.accept(this, labelTrue, false) == BoolGenResult.ALWAYS_JUMP) {
                            // if (b) goto one 始终跳转，即 b=1
                            // 推导出值为0
                            emitTac(new TacLabel(labelTrue));
                            return new TacConstant(ConstantInt.ONE);
                        }
                        // 其他情况都不能断言结果值
                    }
                    case ALWAYS_JUMP -> {
                        // if (a) goto one 始终跳转，即 a=1
                        // 显然值为1，由于短路语义，右操作数永远不求值，可以优化
                        emitTac(new TacLabel(labelTrue));
                        return new TacConstant(ConstantInt.ONE);
                    }
                    case NEVER_JUMP -> {
                        // if (a) goto one 永不跳转，即 a=0
                        // 得继续求值
                        switch (binaryExp.rhs.accept(this, labelTrue, false)) {
                            case ALWAYS_JUMP -> {
                                // b=1 => 推导值为1
                                emitTac(new TacLabel(labelTrue));
                                return new TacConstant(ConstantInt.ONE);
                            }
                            case NEVER_JUMP -> {
                                // b=0 => 推导值为0
                                emitTac(new TacLabel(labelTrue));
                                return new TacConstant(ConstantInt.ZERO);
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
                return dst;
            }
        }
        // 普通求值
        TacValue lhs = binaryExp.lhs.accept(this);
        TacValue rhs = binaryExp.rhs.accept(this);
        if (lhs instanceof TacConstant lhsConst && rhs instanceof TacConstant rhsConst) {
            Constant reduced = lhsConst.value.apply(binaryExp.op.op, rhsConst.value);
            return new TacConstant(reduced);
        }
        TacVariable dst = makeTempVar(binaryExp.expType);
        emitTac(new TacBinaryOperation(binaryExp.op.op, lhs, rhs, dst));
        return dst;
    }

    @Override
    public TacValue visit(AssignmentNode assignment) {
        // 赋值表达式
        TacValue rhs = assignment.rhs.accept(this);
        TacVariable dst = new TacVariable((VariableNode) assignment.lhs);
        if (assignment.op.op == AssignmentOperator.ASSIGN) {
            // 普通赋值
            emitTac(new TacCopy(rhs, dst));
            if (rhs instanceof TacConstant intConstant) {
                return intConstant;
            }
        } else {
            // 复合赋值
            emitTac(new TacBinaryOperation(assignment.op.op.getBinaryOperator(), dst, rhs, dst));
        }
        return dst;
    }

    @Override
    public TacValue visit(VariableNode variable) {
        return new TacVariable(variable);
    }

    @Override
    public TacValue visit(IncrementDecrementNode incrementDecrement) {
        // 自增自减表达式
        BinaryOperator op = incrementDecrement.isIncrement ? BinaryOperator.ADD : BinaryOperator.SUBTRACT;
        TacVariable dst = new TacVariable((VariableNode) incrementDecrement.operand);

        BasicType bt = (BasicType) incrementDecrement.expType;
        Constant one = switch (bt) {
            case INT -> ConstantInt.ONE;
            case LONG -> ConstantLong.ONE;
            case UNSIGNED_INT -> ConstantUnsignedInt.ONE;
            case UNSIGNED_LONG -> ConstantUnsignedLong.ONE;
            case DOUBLE -> ConstantDouble.ONE;
            default -> throw new IllegalStateException("Unexpected value: " + bt);
        };

        if (incrementDecrement.isPrefix) {
            // ++/--a => a = a +/- 1; yield a;
            emitTac(new TacBinaryOperation(op, dst, new TacConstant(one), dst));
            return dst;
        } else {
            // a++/-- => temp = a; a = a +/- 1; yield temp;
            TacVariable temp = makeTempVar(incrementDecrement.expType);
            emitTac(new TacCopy(dst, temp));
            emitTac(new TacBinaryOperation(op, dst, new TacConstant(one), dst));
            return temp;
        }
    }

    @Override
    public TacValue visit(ConditionalExpressionNode condExp) {
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
        switch (condExp.cond.accept(this, labelCondFalse, true)) {
            case ALWAYS_JUMP -> {
                // cond=0，只需求假分支即可
                emitTac(new TacLabel(labelCondFalse));
                return condExp.elseExp.accept(this);
            }
            case NEVER_JUMP -> {
                // cond=1，只需求真分支即可
                TacValue ret = condExp.thenExp.accept(this);
                emitTac(new TacLabel(labelCondFalse));
                return ret;
            }
        }
        String labelCondEnd = makeLabel("cond_end");
        TacValue thenValue = condExp.thenExp.accept(this);
        TacVariable dst = makeTempVar(condExp.expType);
        emitTac(new TacCopy(thenValue, dst));
        emitTac(new TacJump(labelCondEnd));
        emitTac(new TacLabel(labelCondFalse));
        TacValue elseValue = condExp.elseExp.accept(this);
        emitTac(new TacCopy(elseValue, dst));
        emitTac(new TacLabel(labelCondEnd));
        return dst;
    }

    @Override
    public TacValue visit(FunctionCallNode funcCall) {
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
            args.add(arg.accept(this));
        }
        TacVariable dst = makeTempVar(funcCall.expType);
        emitTac(new TacFunctionCall(funcId.id.name, args, dst));
        return dst;
    }

    @Override
    public TacValue visit(CastExpressionNode castExp) {
        TacValue toCast = castExp.exp.accept(this);
        Type targetType = castExp.targetType.getType();
        Type originType = castExp.exp.expType;

        if (targetType.isCompatible(originType)) {
            return toCast;
        }
        if (toCast instanceof TacConstant constant) {
            return new TacConstant(constant.value.castTo((BasicType) targetType));
        }
        TacVariable dst = makeTempVar(targetType);

        BasicType targetBasic = (BasicType) targetType;
        BasicType originBasic = (BasicType) originType;

        if (targetBasic == BasicType.DOUBLE) {
            switch (originBasic) {
                case INT, LONG -> emitTac(new TacIntToDouble(toCast, dst));
                case UNSIGNED_INT, UNSIGNED_LONG -> emitTac(new TacUnsignedIntToDouble(toCast, dst));
                default -> throw new IllegalStateException("Unexpected value: " + originBasic);
            }
            return dst;
        }
        if (originBasic == BasicType.DOUBLE) {
            switch (targetBasic) {
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
    public TacValue visit(AddressOfNode addrOf) {
        return null;
    }

    @Override
    public TacValue visit(DereferenceNode deref) {
        return null;
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
                    switch (binaryExp.lhs.accept(this, jumpTarget, true)) {
                        case VARIOUS -> {
                            // a 未知
                            if (binaryExp.rhs.accept(this, jumpTarget, true) == BoolGenResult.ALWAYS_JUMP) {
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
                            return binaryExp.rhs.accept(this, jumpTarget, true);
                        }
                    }
                } else {
                    // if (a && b) jump => if (!a) jump false ; if (b) jump; false:
                    String label = makeLabel("and_false");
                    BoolGenResult ret = BoolGenResult.VARIOUS;
                    switch (binaryExp.lhs.accept(this, label, true)) {
                        case VARIOUS -> {
                            // a 未知
                            switch (binaryExp.rhs.accept(this, jumpTarget, false)) {
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
                            ret = binaryExp.rhs.accept(this, jumpTarget, false);
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
                    switch (binaryExp.lhs.accept(this, label, false)) {
                        case VARIOUS -> {
                            // a 未知
                            switch (binaryExp.rhs.accept(this, jumpTarget, true)) {
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
                            ret = binaryExp.rhs.accept(this, jumpTarget, true);
                        }
                    }
                    // 保证标签有定义
                    emitTac(new TacLabel(label));
                    return ret;
                } else {
                    // if (a || b) jump => if (a) jump ; if (b) jump
                    switch (binaryExp.lhs.accept(this, jumpTarget, false)) {
                        case VARIOUS -> {
                            // a 未知
                            if (binaryExp.rhs.accept(this, jumpTarget, false) == BoolGenResult.ALWAYS_JUMP) {
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
                            return binaryExp.rhs.accept(this, jumpTarget, false);
                        }
                    }
                }
            }
            case EQUAL, NOT_EQUAL, LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL -> {
                // 直接生成比较跳转指令，而不是比较置位指令
                Comparison cond = binaryExp.op.op.toComparison();

                TacValue lhs = binaryExp.lhs.accept(this);
                TacValue rhs = binaryExp.rhs.accept(this);
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
        TacValue value = exp.accept(this);
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
        return null;
    }

    @Override
    public BoolGenResult visit(DereferenceNode deref, String jumpTarget, boolean inverse) {
        return null;
    }
}
