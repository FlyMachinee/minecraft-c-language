package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.AssignmentOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class AstToTacLowerer {

    public AstToTacLowerer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    private final SymbolTable symbolTable;

    public TacProgram lower(ProgramNode program) {
        List<TacTopLevel> topLevels = new ArrayList<>();
        for (ExternalDeclarationNode extDecl : program.declarations) {
            if (extDecl instanceof FunctionDefinitionNode funcDef) {
                topLevels.add(lowerFunc(funcDef));
            }
        }
        // 遍历符号表，将需要本编译单元内初始化的全局变量进行定义
        for (SymbolTable.Entry entry : symbolTable.getEntries()) {
            SymbolTable.Entry.IdentifierAttr attr = entry.attr;
            if (attr instanceof SymbolTable.Entry.StaticAttr staticAttr) {
                SymbolTable.Entry.StaticAttr.InitialValue initialValue = staticAttr.initialValue;
                if (initialValue instanceof SymbolTable.Entry.StaticAttr.Initial initial) {
                    topLevels.add(new TacStaticVariable(entry.id.id, staticAttr.global, initial.value()));
                } else if (initialValue instanceof SymbolTable.Entry.StaticAttr.Tentative) {
                    topLevels.add(new TacStaticVariable(entry.id.id, staticAttr.global, 0));
                }
            }
        }
        return new TacProgram(topLevels);
    }

    private int tempVarCounter = 0;

    private String makeTempVar() {
        return "tmp." + (tempVarCounter++);
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
        lowerStmt(funcDef.body);
        emitTac(new TacReturn(new TacIntConstant(0)));
        boolean global = symbolTable.get(funcDef.identifier.id).attr.isGlobal();
        FunctionTypeNode functionType = (FunctionTypeNode) funcDef.functionType;
        if (functionType.hasNoParameters()) {
            return new TacFunction(funcDef.identifier.id, global, List.of(), instructions);
        } else {
            List<String> parameters = functionType.parameters.stream().map(param -> param.id).toList();
            return new TacFunction(funcDef.identifier.id, global, parameters, instructions);
        }
    }

    private void lowerBlockItem(BlockItemNode blockItem) {
        if (blockItem instanceof StatementBlockItemNode stmt) {
            lowerStmt(stmt.statement);
        } else if (blockItem instanceof DeclarationBlockItemNode decl) {
            lowerDecl(decl.declaration);
        } else {
            throw new RuntimeException("Unknown instruction: " + blockItem.toString());
        }
    }

    private void lowerDecl(DeclarationNode decl) {
        // 一定为块作用域
        // 无存储类且有初始化时，生成初始化三地址码
        if (decl.initializer != null && decl.storageClass == null) {
            TacValue initValue = lowerExp(decl.initializer);
            emitTac(new TacCopy(initValue, new TacVariable(decl.identifier.id)));
        }
    }

    private void lowerStmt(StatementNode stmt) {
        // 为活跃的 goto 标签生成标签
        for (int i = stmt.gotoLabels.size() - 1; i >= 0; i--) {
            StatementNode.GotoLabelInfo info = stmt.gotoLabels.get(i);
            if (info.active) {
                emitTac(new TacLabel(info.label.id));
            }
        }

        // case 和 default
        for (int i = stmt.caseLabels.size() - 1; i >= 0; i--) {
            StatementNode.CaseLabelInfo info = stmt.caseLabels.get(i);
            emitTac(new TacLabel("case_" + info.caseIndex + "_" + info.switchLabel));
        }
        if (!stmt.defaultLabels.isEmpty()) {
            StatementNode.DefaultLabelInfo info = stmt.defaultLabels.get(0);
            emitTac(new TacLabel("default_" + info.switchLabel));
        }

        if (stmt instanceof ReturnNode ret) {
            TacValue returnValue = lowerExp(ret.expression);
            emitTac(new TacReturn(returnValue));
        } else if (stmt instanceof ExpressionStatementNode expStmt) {
            lowerExp(expStmt.expression);
        } else if (stmt instanceof IfStatementNode ifStmt) {
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
                switch (lowerBool(ifStmt.cond, labelElse, true)) {
                    case ALWAYS_JUMP -> {
                        if (!ifStmt.thenStmt.containsActiveLabel()) {
                            // 当 then 子语句没有活跃的 goto 标签，将其优化
                            emitTac(new TacLabel(labelElse));
                            lowerStmt(ifStmt.elseStmt);
                            return;
                        }
                        // 否则生成无条件跳转
                        emitTac(new TacJump(labelElse));
                    }
                    case NEVER_JUMP -> {
                        if (!ifStmt.elseStmt.containsActiveLabel()) {
                            // 当 else 子语句没有活跃的 goto 标签，将其优化
                            lowerStmt(ifStmt.thenStmt);
                            emitTac(new TacLabel(labelElse));
                            return;
                        }
                    }
                }
                String labelEndIf = makeLabel("endif");
                lowerStmt(ifStmt.thenStmt);
                emitTac(new TacJump(labelEndIf));
                emitTac(new TacLabel(labelElse));
                lowerStmt(ifStmt.elseStmt);
                emitTac(new TacLabel(labelEndIf));
            } else {
                // if (cond) thenStmt
                // =>
                // if (!cond) goto end
                // thenStmt
                // end:
                String labelEndIf = makeLabel("endif");
                if (lowerBool(ifStmt.cond, labelEndIf, true) == BoolGenResult.ALWAYS_JUMP) {
                    if (!ifStmt.thenStmt.containsActiveLabel()) {
                        // 当 then 子语句没有活跃的 goto 标签，将其优化
                        emitTac(new TacLabel(labelEndIf));
                        return;
                    }
                    // 否则生成无条件跳转
                    emitTac(new TacJump(labelEndIf));
                }
                lowerStmt(ifStmt.thenStmt);
                emitTac(new TacLabel(labelEndIf));
            }
        } else if (stmt instanceof GotoNode gotoNode) {
            // 为 goto 语句生成无条件跳转
            emitTac(new TacJump(gotoNode.target.id));
        } else if (stmt instanceof CompoundStatementNode compoundStmt) {
            for (BlockItemNode item : compoundStmt.blockItems) {
                lowerBlockItem(item);
            }
        } else if (stmt instanceof BreakNode breakNode) {
            emitTac(new TacJump("break_" + breakNode.loopOrSwitchLabel));
        } else if (stmt instanceof ContinueNode continueNode) {
            emitTac(new TacJump("continue_" + continueNode.loopLabel));
        } else if (stmt instanceof WhileLoopNode whileLoop) {
            // while (cond) body
            // =>
            //   goto continue_label <=====
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
                // while 循环需要在循环前生成跳转到条件判断的指令
                emitTac(new TacJump(labelContinue));
            }
            emitTac(new TacLabel(labelBegin));
            lowerStmt(whileLoop.body);
            emitTac(new TacLabel(labelContinue));
            if (lowerBool(whileLoop.cond, labelBegin, false) == BoolGenResult.ALWAYS_JUMP) {
                // 始终跳转，生成无条件跳转
                emitTac(new TacJump(labelBegin));
            }
            emitTac(new TacLabel(labelBreak));

        } else if (stmt instanceof ForLoopNode forLoop) {
            // for (init; cond; step) body
            // =>
            //   init
            //   goto cond
            // begin:
            //   body
            // continue_label:
            //   step
            // cond:
            //   if (cond) goto begin
            // break_label:
            String labelBegin = makeLabel("for_begin");
            String labelContinue = "continue_" + forLoop.loopLabel;
            String labelBreak = "break_" + forLoop.loopLabel;
            String labelCond = makeLabel("for_cond");

            if (forLoop.init != null) {
                if (forLoop.init instanceof ForInitDeclarationNode decl) {
                    lowerDecl(decl.declaration);
                } else if (forLoop.init instanceof ForInitExpressionNode expr) {
                    lowerExp(expr.expression);
                } else {
                    throw new RuntimeException("unexpected init node in for loop: " + forLoop.init.getClass());
                }
            }
            emitTac(new TacJump(labelCond));
            emitTac(new TacLabel(labelBegin));
            lowerStmt(forLoop.body);
            emitTac(new TacLabel(labelContinue));
            if (forLoop.step != null) {
                lowerExp(forLoop.step);
            }
            emitTac(new TacLabel(labelCond));
            if (forLoop.cond != null) {
                if (lowerBool(forLoop.cond, labelBegin, false) == BoolGenResult.ALWAYS_JUMP) {
                    // 始终跳转，生成无条件跳转
                    emitTac(new TacJump(labelBegin));
                }
            } else {
                // 条件缺省，视为始终为真
                emitTac(new TacJump(labelBegin));
            }
            emitTac(new TacLabel(labelBreak));

        } else if (stmt instanceof SwitchStatementNode switchNode) {
            // switch (exp) body   {case: [0, 1, 2, ...], default=yes/no}
            // =>
            //   tmp = exp
            //   if (tmp == 0) goto case0
            //   if (tmp == 1) goto case1
            //   goto default (if default=yes)
            //   goto break (if default=no)
            //   body
            // break:
            String labelBreak = "break_" + switchNode.switchLabel;
            String defaultLabel = "default_" + switchNode.switchLabel;

            TacValue res = lowerExp(switchNode.exp);
            if (res instanceof TacIntConstant intConstant) {
                // 常量，进行优化
                int value = intConstant.value;
                if (switchNode.caseValues.containsKey(value)) {
                    // 匹配到 case 标签
                    emitTac(new TacJump("case_" + value + "_" + switchNode.switchLabel));
                } else if (switchNode.defaultLabel != null) {
                    // 没有匹配到 case 标签但有 default 标签
                    emitTac(new TacJump(defaultLabel));
                } else {
                    // 没有匹配到 case 标签且没有 default 标签，直接跳转到 break

                    // 如果此时 body 没有可能跳入的 goto 标签，则可以优化掉 body 和 break 标签
                    if (!switchNode.body.containsActiveLabel()) {
                        return;
                    }
                    emitTac(new TacJump(labelBreak));
                }
            } else {
                // 非常量，生成比较指令
                for (SwitchStatementNode.CaseLabelInfo caseInfo : switchNode.caseValues.values()) {
                    String caseLabel = "case_" + caseInfo.caseIndex + "_" + switchNode.switchLabel;
                    emitTac(new TacJumpIfComparison(
                        Comparison.EQUAL, res, new TacIntConstant(caseInfo.caseIndex), caseLabel));
                }
                if (switchNode.defaultLabel != null) {
                    emitTac(new TacJump(defaultLabel));
                } else {
                    emitTac(new TacJump(labelBreak));
                }
            }
            lowerStmt(switchNode.body);
            emitTac(new TacLabel(labelBreak));

        } else if (!(stmt instanceof NullStatementNode)) {
            throw new UnsupportedOperationException(
                "Unsupported statement type: " + stmt.getClass().getSimpleName());
        }
    }

    /**
     * 对表达式进行求值，可能会因为求值而生成求值过程的三地址码，返回表达式的值
     *
     * @param exp 将要求值的表达式
     * @return 表达式的求值结果
     */
    private TacValue lowerExp(ExpressionNode exp) {
        if (exp instanceof IntConstantNode intConstant) {
            return new TacIntConstant(intConstant.value);
        } else if (exp instanceof UnaryExpressionNode unaryExp) {
            TacValue src = lowerExp(unaryExp.exp);
            if (src instanceof TacIntConstant intConstant) {
                int res = switch (unaryExp.op.op) {
                    case COMPLEMENT -> ~intConstant.value;
                    case NEGATE -> -intConstant.value;
                    case NOT -> (intConstant.value == 0) ? 1 : 0;
                };
                return new TacIntConstant(res);
            }
            TacVariable dst = new TacVariable(makeTempVar());
            emitTac(new TacUnaryOperation(unaryExp.op.op, src, dst));
            return dst;
        } else if (exp instanceof BinaryExpressionNode binaryExp) {
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
                    switch (lowerBool(binaryExp.lhs, labelFalse, true)) {
                        case VARIOUS -> {
                            // a 未知
                            if (lowerBool(binaryExp.rhs, labelFalse, true) == BoolGenResult.ALWAYS_JUMP) {
                                // if (!b) goto zero 始终跳转，即 b=0
                                // 推导出值为0
                                emitTac(new TacLabel(labelFalse));
                                return new TacIntConstant(0);
                            }
                            // 其他情况都不能断言结果值
                        }
                        case ALWAYS_JUMP -> {
                            // if (!a) goto zero 始终跳转，即 a=0
                            // 显然值为0，由于短路语义，右操作数永远不求值，可以优化
                            emitTac(new TacLabel(labelFalse));
                            return new TacIntConstant(0);
                        }
                        case NEVER_JUMP -> {
                            // if (!a) goto zero 永不跳转，即 a=1
                            // 得继续求值
                            switch (lowerBool(binaryExp.rhs, labelFalse, true)) {
                                case ALWAYS_JUMP -> {
                                    // b=0 => 推导值为0
                                    emitTac(new TacLabel(labelFalse));
                                    return new TacIntConstant(0);
                                }
                                case NEVER_JUMP -> {
                                    // b=1 => 推导值为1
                                    emitTac(new TacLabel(labelFalse));
                                    return new TacIntConstant(1);
                                }
                            }
                            // b 未知，无法断言
                        }
                    }

                    // 无法断言的情况，需要生成指令来进行求值
                    String labelEvalEnd = makeLabel("eval_end");
                    TacVariable dst = new TacVariable(makeTempVar());
                    emitTac(new TacCopy(new TacIntConstant(1), dst));
                    emitTac(new TacJump(labelEvalEnd));
                    emitTac(new TacLabel(labelFalse));
                    emitTac(new TacCopy(new TacIntConstant(0), dst));
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
                    switch (lowerBool(binaryExp.lhs, labelTrue, false)) {
                        case VARIOUS -> {
                            // a 未知
                            if (lowerBool(binaryExp.rhs, labelTrue, false) == BoolGenResult.ALWAYS_JUMP) {
                                // if (b) goto one 始终跳转，即 b=1
                                // 推导出值为0
                                emitTac(new TacLabel(labelTrue));
                                return new TacIntConstant(1);
                            }
                            // 其他情况都不能断言结果值
                        }
                        case ALWAYS_JUMP -> {
                            // if (a) goto one 始终跳转，即 a=1
                            // 显然值为1，由于短路语义，右操作数永远不求值，可以优化
                            emitTac(new TacLabel(labelTrue));
                            return new TacIntConstant(1);
                        }
                        case NEVER_JUMP -> {
                            // if (a) goto one 永不跳转，即 a=0
                            // 得继续求值
                            switch (lowerBool(binaryExp.rhs, labelTrue, false)) {
                                case ALWAYS_JUMP -> {
                                    // b=1 => 推导值为1
                                    emitTac(new TacLabel(labelTrue));
                                    return new TacIntConstant(1);
                                }
                                case NEVER_JUMP -> {
                                    // b=0 => 推导值为0
                                    emitTac(new TacLabel(labelTrue));
                                    return new TacIntConstant(0);
                                }
                            }
                            // b 未知，无法断言
                        }
                    }

                    // 无法断言的情况，需要生成指令来进行求值
                    String labelEvalEnd = makeLabel("eval_end");
                    TacVariable dst = new TacVariable(makeTempVar());
                    emitTac(new TacCopy(new TacIntConstant(0), dst));
                    emitTac(new TacJump(labelEvalEnd));
                    emitTac(new TacLabel(labelTrue));
                    emitTac(new TacCopy(new TacIntConstant(1), dst));
                    emitTac(new TacLabel(labelEvalEnd));
                    return dst;
                }
            }
            // 普通求值
            TacValue lhs = lowerExp(binaryExp.lhs);
            TacValue rhs = lowerExp(binaryExp.rhs);
            // TODO: 提取公共函数
            if (lhs instanceof TacIntConstant lhsInt && rhs instanceof TacIntConstant rhsInt) {
                int res = switch (binaryExp.op.op) {
                    case ADD -> lhsInt.value + rhsInt.value;
                    case SUBTRACT -> lhsInt.value - rhsInt.value;
                    case MULTIPLY -> lhsInt.value * rhsInt.value;
                    case DIVIDE -> lhsInt.value / rhsInt.value;
                    case MODULO -> lhsInt.value % rhsInt.value;
                    case LEFT_SHIFT -> lhsInt.value << rhsInt.value;
                    case RIGHT_SHIFT -> lhsInt.value >> rhsInt.value;
                    case BITWISE_AND -> lhsInt.value & rhsInt.value;
                    case BITWISE_OR -> lhsInt.value | rhsInt.value;
                    case BITWISE_XOR -> lhsInt.value ^ rhsInt.value;
                    case EQUAL -> (lhsInt.value == rhsInt.value) ? 1 : 0;
                    case NOT_EQUAL -> (lhsInt.value != rhsInt.value) ? 1 : 0;
                    case LESS_THAN -> (lhsInt.value < rhsInt.value) ? 1 : 0;
                    case LESS_OR_EQUAL -> (lhsInt.value <= rhsInt.value) ? 1 : 0;
                    case GREATER_THAN -> (lhsInt.value > rhsInt.value) ? 1 : 0;
                    case GREATER_OR_EQUAL -> (lhsInt.value >= rhsInt.value) ? 1 : 0;
                    default -> throw new IllegalStateException("Control should never reach here");
                };
                return new TacIntConstant(res);
            }
            TacVariable dst = new TacVariable(makeTempVar());
            emitTac(new TacBinaryOperation(binaryExp.op.op, lhs, rhs, dst));
            return dst;
        } else if (exp instanceof AssignmentNode assignment) {
            // 赋值表达式
            TacValue rhs = lowerExp(assignment.rhs);
            TacVariable dst = new TacVariable(((IdentifierNode) assignment.lhs).id);
            if (assignment.op.op == AssignmentOperator.ASSIGN) {
                // 普通赋值
                emitTac(new TacCopy(rhs, dst));
                if (rhs instanceof TacIntConstant intConstant) {
                    return intConstant;
                }
            } else {
                // 复合赋值
                emitTac(new TacBinaryOperation(assignment.op.op.getBinaryOperator(), dst, rhs, dst));
            }
            return dst;
        } else if (exp instanceof IdentifierNode identifier) {
            return new TacVariable(identifier.id);
        } else if (exp instanceof IncrementDecrementNode incrementDecrement) {
            // 自增自减表达式
            BinaryOperator op = incrementDecrement.isIncrement ? BinaryOperator.ADD : BinaryOperator.SUBTRACT;
            TacVariable dst = new TacVariable(((IdentifierNode) incrementDecrement.operand).id);
            if (incrementDecrement.isPrefix) {
                // ++/--a => a = a +/- 1; yield a;
                emitTac(new TacBinaryOperation(op, dst, new TacIntConstant(1), dst));
                return dst;
            } else {
                // a++/-- => temp = a; a = a +/- 1; yield temp;
                TacVariable temp = new TacVariable(makeTempVar());
                emitTac(new TacCopy(dst, temp));
                emitTac(new TacBinaryOperation(op, dst, new TacIntConstant(1), dst));
                return temp;
            }
        } else if (exp instanceof ConditionalExpressionNode condExp) {
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
            switch (lowerBool(condExp.cond, labelCondFalse, true)) {
                case ALWAYS_JUMP -> {
                    // cond=0，只需求假分支即可
                    emitTac(new TacLabel(labelCondFalse));
                    return lowerExp(condExp.elseExpr);
                }
                case NEVER_JUMP -> {
                    // cond=1，只需求真分支即可
                    TacValue ret = lowerExp(condExp.thenExpr);
                    emitTac(new TacLabel(labelCondFalse));
                    return ret;
                }
            }
            String labelCondEnd = makeLabel("cond_end");
            TacValue thenValue = lowerExp(condExp.thenExpr);
            TacVariable dst = new TacVariable(makeTempVar());
            emitTac(new TacCopy(thenValue, dst));
            emitTac(new TacJump(labelCondEnd));
            emitTac(new TacLabel(labelCondFalse));
            TacValue elseValue = lowerExp(condExp.elseExpr);
            emitTac(new TacCopy(elseValue, dst));
            emitTac(new TacLabel(labelCondEnd));
            return dst;
        } else if (exp instanceof FunctionCallNode funcCall) {
            // 函数调用
            // func(arg0, arg1, ...)
            // =>
            // res0 = <eval arg0>
            // res1 = <eval arg1>
            // ...
            // dst = invoke(func, [res0, res1,...])
            // yield dst

            IdentifierNode funcId = (IdentifierNode) funcCall.function;
            List<TacValue> args = new ArrayList<>();
            for (ExpressionNode arg : funcCall.arguments) {
                args.add(lowerExp(arg));
            }
            TacVariable dst = new TacVariable(makeTempVar());
            emitTac(new TacFunctionCall(funcId.id, args, dst));
            return dst;
        }
        throw new UnsupportedOperationException(
            "Unsupported expression type: " + exp.getClass().getSimpleName());
    }

    /**
     * 对表达式进行求值，在求值末尾处生成“若求值结果为真”则跳转的指令
     * <p>
     * 需要保证传入的标签始终有定义，即使该方法最终断言始终跳转或永不跳转也不例外
     *
     * @param exp        要求值的表达式
     * @param jumpTarget 生成跳转指令的跳转目标
     * @param inverse    是否将表达式条件取反
     * @return 如果生成的跳转指令无条件跳转则返回 {@link BoolGenResult#ALWAYS_JUMP}，如果永不跳转则返回
     * {@link BoolGenResult#NEVER_JUMP}，否则返回 {@link BoolGenResult#VARIOUS}。
     * 返回 {@link BoolGenResult#ALWAYS_JUMP} 或 {@link BoolGenResult#NEVER_JUMP} 时将不生成任何跳转指令
     */
    private @NotNull AstToTacLowerer.BoolGenResult lowerBool(ExpressionNode exp, String jumpTarget, boolean inverse) {

        if (exp instanceof IntConstantNode intConstant) {
            // 常量，生成无条件跳转
            if ((intConstant.value != 0) ^ inverse) {
                return BoolGenResult.ALWAYS_JUMP;
            } else {
                return BoolGenResult.NEVER_JUMP;
            }
        }

        if (exp instanceof BinaryExpressionNode binaryExp) {
            switch (binaryExp.op.op) {
                case LOGICAL_AND -> {
                    if (inverse) {
                        // if (!(a && b)) jump => if (!a) jump ; if (!b) jump
                        switch (lowerBool(binaryExp.lhs, jumpTarget, true)) {
                            case VARIOUS -> {
                                // a 未知
                                if (lowerBool(binaryExp.rhs, jumpTarget, true) == BoolGenResult.ALWAYS_JUMP) {
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
                                return lowerBool(binaryExp.rhs, jumpTarget, true);
                            }
                        }
                    } else {
                        // if (a && b) jump => if (!a) jump false ; if (b) jump; false:
                        String label = makeLabel("and_false");
                        BoolGenResult ret = BoolGenResult.VARIOUS;
                        switch (lowerBool(binaryExp.lhs, label, true)) {
                            case VARIOUS -> {
                                // a 未知
                                if (lowerBool(binaryExp.rhs, jumpTarget, false) == BoolGenResult.NEVER_JUMP) {
                                    // b=0 => a && b = 0
                                    ret = BoolGenResult.NEVER_JUMP;
                                }
                                // 无法断言
                            }
                            case ALWAYS_JUMP -> {
                                // a=0 => a && b = 0
                                ret = BoolGenResult.NEVER_JUMP;
                            }
                            case NEVER_JUMP -> {
                                // a=1 => if (b) jump;
                                ret = lowerBool(binaryExp.rhs, jumpTarget, false);
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
                        switch (lowerBool(binaryExp.lhs, label, false)) {
                            case VARIOUS -> {
                                // a 未知
                                if (lowerBool(binaryExp.rhs, jumpTarget, true) == BoolGenResult.NEVER_JUMP) {
                                    // b=1 => !(a || b) = 0
                                    ret = BoolGenResult.NEVER_JUMP;
                                }
                                // 无法断言
                            }
                            case ALWAYS_JUMP -> {
                                // a=1 => !(a || b) = 0
                                ret = BoolGenResult.NEVER_JUMP;
                            }
                            case NEVER_JUMP -> {
                                // a=0 => if (!b) jump;
                                ret = lowerBool(binaryExp.rhs, jumpTarget, true);
                            }
                        }
                        // 保证标签有定义
                        emitTac(new TacLabel(label));
                        return ret;
                    } else {
                        // if (a || b) jump => if (a) jump ; if (b) jump
                        switch (lowerBool(binaryExp.lhs, jumpTarget, false)) {
                            case VARIOUS -> {
                                // a 未知
                                if (lowerBool(binaryExp.rhs, jumpTarget, false) == BoolGenResult.ALWAYS_JUMP) {
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
                                return lowerBool(binaryExp.rhs, jumpTarget, false);
                            }
                        }
                    }
                }
                case EQUAL, NOT_EQUAL, LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL -> {
                    // 直接生成比较跳转指令，而不是比较置位指令
                    Comparison cond = switch (binaryExp.op.op) {
                        case EQUAL -> inverse ? Comparison.NOT_EQUAL : Comparison.EQUAL;
                        case NOT_EQUAL -> inverse ? Comparison.EQUAL : Comparison.NOT_EQUAL;
                        case LESS_THAN -> inverse ? Comparison.GREATER_EQUAL : Comparison.LESS;
                        case LESS_OR_EQUAL -> inverse ? Comparison.GREATER : Comparison.LESS_EQUAL;
                        case GREATER_THAN -> inverse ? Comparison.LESS_EQUAL : Comparison.GREATER;
                        case GREATER_OR_EQUAL -> inverse ? Comparison.LESS : Comparison.GREATER_EQUAL;
                        default -> throw new IllegalStateException("Control should never reach here");
                    };

                    TacValue lhs = lowerExp(binaryExp.lhs);
                    TacValue rhs = lowerExp(binaryExp.rhs);
                    // TODO: 提取函数
                    if (lhs instanceof TacIntConstant lhsInt && rhs instanceof TacIntConstant rhsInt) {
                        boolean isJump = switch (cond) {
                            case EQUAL -> lhsInt.value == rhsInt.value;
                            case NOT_EQUAL -> lhsInt.value != rhsInt.value;
                            case LESS -> lhsInt.value < rhsInt.value;
                            case LESS_EQUAL -> lhsInt.value <= rhsInt.value;
                            case GREATER -> lhsInt.value > rhsInt.value;
                            case GREATER_EQUAL -> lhsInt.value >= rhsInt.value;
                        };
                        if (isJump) {
                            return BoolGenResult.ALWAYS_JUMP;
                        } else {
                            return BoolGenResult.NEVER_JUMP;
                        }
                    }
                    emitTac(new TacJumpIfComparison(cond, lhs, rhs, jumpTarget));
                    return BoolGenResult.VARIOUS;
                }
            }
        } else if (exp instanceof UnaryExpressionNode unaryExp) {
            if (unaryExp.op.op == UnaryOperator.NOT) {
                return lowerBool(unaryExp.exp, jumpTarget, !inverse);
            }
        }

        // 其他表达式，先求值再与 0 比较跳转
        TacValue value = lowerExp(exp);
        if (value instanceof TacIntConstant intConstant) {
            if ((intConstant.value != 0) ^ inverse) {
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

    /**
     * 作为 {@link #lowerBool(ExpressionNode, String, boolean)} 的返回值
     */
    private enum BoolGenResult {
        /**
         * 跳转指令的跳转结果在该阶段无法断言
         */
        VARIOUS,

        /**
         * 断言跳转指令无条件跳转
         */
        ALWAYS_JUMP,

        /**
         * 断言跳转指令永不跳转
         */
        NEVER_JUMP
    }
}
