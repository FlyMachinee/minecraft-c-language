package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.AssignmentOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.*;

import java.util.ArrayList;
import java.util.List;

public final class AstToTacLowerer {

    public AstToTacLowerer() { }

    public TacProgram lower(ProgramNode program) {
        TacFunction functionDefinition = lowerFunction(program.functionDefinition);
        return new TacProgram(functionDefinition);
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

    private TacFunction lowerFunction(FunctionDefinitionNode functionDefinition) {
        List<TacInstruction> instructions = new ArrayList<>();
        for (BlockItemNode blockItemNode : functionDefinition.body) {
            if (blockItemNode instanceof StatementNode statementNode) {
                lowerStatement(statementNode, instructions);
            } else if (blockItemNode instanceof DeclarationNode declarationNode) {
                if (declarationNode.initializer != null) {
                    TacValue initValue = lowerExpression(declarationNode.initializer, instructions);
                    instructions.add(new TacCopy(initValue, new TacVariable(declarationNode.variable.id)));
                }
            } else {
                throw new RuntimeException("Unknown instruction: " + blockItemNode.toString());
            }
        }
        instructions.add(new TacReturn(new TacIntConstant(0)));
        return new TacFunction(functionDefinition.identifier.id, instructions);
    }

    private void lowerStatement(StatementNode statement, List<TacInstruction> instructions) {
        if (statement instanceof ReturnNode returnNode) {
            TacValue returnValue = lowerExpression(returnNode.expression, instructions);
            instructions.add(new TacReturn(returnValue));
        } else if (statement instanceof ExpressionStatementNode expressionStatementNode) {
            lowerExpression(expressionStatementNode.expression, instructions);
        } else if (statement instanceof IfStatementNode ifStatementNode) {
            if (ifStatementNode.elseStmt != null) {
                String labelElse = makeLabel("else");
                switch (lowerBoolean(ifStatementNode.cond, labelElse, true, instructions)) {
                    case ALWAYS_JUMP -> {
                        instructions.add(new TacLabel(labelElse));
                        lowerStatement(ifStatementNode.elseStmt, instructions);
                        return;
                    }
                    case NEVER_JUMP -> {
                        lowerStatement(ifStatementNode.thenStmt, instructions);
                        instructions.add(new TacLabel(labelElse));
                        return;
                    }
                }
                String labelEndIf = makeLabel("endif");
                lowerStatement(ifStatementNode.thenStmt, instructions);
                instructions.add(new TacJump(labelEndIf));
                instructions.add(new TacLabel(labelElse));
                lowerStatement(ifStatementNode.elseStmt, instructions);
                instructions.add(new TacLabel(labelEndIf));
            } else {
                String labelEndIf = makeLabel("endif");
                if (lowerBoolean(ifStatementNode.cond, labelEndIf, true, instructions) ==
                    BooleanGenerationResult.ALWAYS_JUMP) {
                    instructions.add(new TacLabel(labelEndIf));
                    return;
                }
                lowerStatement(ifStatementNode.thenStmt, instructions);
                instructions.add(new TacLabel(labelEndIf));
            }
        } else if (!(statement instanceof NullStatementNode)) {
            throw new UnsupportedOperationException(
                "Unsupported statement type: " + statement.getClass().getSimpleName());
        }
    }

    /**
     * 对表达式进行求值，可能会因为求值而生成求值过程的三地址码，返回表达式的值
     *
     * @param expression   将要求值的表达式
     * @param instructions 三地址码生成目标
     * @return 表达式的求值结果
     */
    private TacValue lowerExpression(ExpressionNode expression, List<TacInstruction> instructions) {
        // TODO: 进行常量折叠
        if (expression instanceof IntConstantNode intConstantNode) {
            return new TacIntConstant(intConstantNode.value);
        } else if (expression instanceof UnaryExpressionNode unaryExpressionNode) {
            TacValue src = lowerExpression(unaryExpressionNode.exp, instructions);
            if (src instanceof TacIntConstant intConstant) {
                int res = switch (unaryExpressionNode.op.op) {
                    case COMPLEMENT -> ~intConstant.value;
                    case NEGATE -> -intConstant.value;
                    case NOT -> (intConstant.value == 0) ? 1 : 0;
                };
                return new TacIntConstant(res);
            }
            TacVariable dst = new TacVariable(makeTempVar());
            instructions.add(new TacUnaryOperation(unaryExpressionNode.op.op, src, dst));
            return dst;
        } else if (expression instanceof BinaryExpressionNode binaryExpressionNode) {
            // 短路求值
            switch (binaryExpressionNode.op.op) {
                case LOGICAL_AND -> {
                    // 短路与求值
                    String labelFalse = makeLabel("and_false");
                    String labelEvalEnd = makeLabel("eval_end");
                    switch (lowerBoolean(binaryExpressionNode.lhs, labelFalse, true, instructions)) {
                        case VARIOUS -> {
                            if (lowerBoolean(
                                binaryExpressionNode.rhs, labelFalse, true, instructions) ==
                                BooleanGenerationResult.ALWAYS_JUMP) {

                                instructions.add(new TacLabel(labelFalse));
                                return new TacIntConstant(0);
                            }
                        }
                        case ALWAYS_JUMP -> {
                            instructions.add(new TacLabel(labelFalse));
                            return new TacIntConstant(0);
                        }
                        case NEVER_JUMP -> {
                            switch (lowerBoolean(binaryExpressionNode.rhs, labelFalse, true, instructions)) {
                                case ALWAYS_JUMP -> {
                                    instructions.add(new TacLabel(labelFalse));
                                    return new TacIntConstant(0);
                                }
                                case NEVER_JUMP -> {
                                    instructions.add(new TacLabel(labelFalse));
                                    return new TacIntConstant(1);
                                }
                            }
                        }
                    }

                    TacVariable dst = new TacVariable(makeTempVar());
                    instructions.add(new TacCopy(new TacIntConstant(1), dst));
                    instructions.add(new TacJump(labelEvalEnd));
                    instructions.add(new TacLabel(labelFalse));
                    instructions.add(new TacCopy(new TacIntConstant(0), dst));
                    instructions.add(new TacLabel(labelEvalEnd));
                    return dst;
                }
                case LOGICAL_OR -> {
                    // 短路或求值
                    String labelTrue = makeLabel("or_true");
                    switch (lowerBoolean(binaryExpressionNode.lhs, labelTrue, false, instructions)) {
                        case VARIOUS -> {
                            if (lowerBoolean(
                                binaryExpressionNode.rhs, labelTrue, false, instructions) ==
                                BooleanGenerationResult.ALWAYS_JUMP) {

                                instructions.add(new TacLabel(labelTrue));
                                return new TacIntConstant(1);
                            }
                        }
                        case ALWAYS_JUMP -> {
                            instructions.add(new TacLabel(labelTrue));
                            return new TacIntConstant(1);
                        }
                        case NEVER_JUMP -> {
                            switch (lowerBoolean(binaryExpressionNode.rhs, labelTrue, false, instructions)) {
                                case ALWAYS_JUMP -> {
                                    instructions.add(new TacLabel(labelTrue));
                                    return new TacIntConstant(1);
                                }
                                case NEVER_JUMP -> {
                                    instructions.add(new TacLabel(labelTrue));
                                    return new TacIntConstant(0);
                                }
                            }
                        }
                    }

                    String labelEvalEnd = makeLabel("eval_end");
                    TacVariable dst = new TacVariable(makeTempVar());
                    instructions.add(new TacCopy(new TacIntConstant(0), dst));
                    instructions.add(new TacJump(labelEvalEnd));
                    instructions.add(new TacLabel(labelTrue));
                    instructions.add(new TacCopy(new TacIntConstant(1), dst));
                    instructions.add(new TacLabel(labelEvalEnd));
                    return dst;
                }
            }
            // 普通求值
            TacValue lhs = lowerExpression(binaryExpressionNode.lhs, instructions);
            TacValue rhs = lowerExpression(binaryExpressionNode.rhs, instructions);
            if (lhs instanceof TacIntConstant lhsInt && rhs instanceof TacIntConstant rhsInt) {
                int res = switch (binaryExpressionNode.op.op) {
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
            instructions.add(new TacBinaryOperation(binaryExpressionNode.op.op, lhs, rhs, dst));
            return dst;
        } else if (expression instanceof AssignmentNode assignmentNode) {
            // 赋值表达式
            TacValue rhs = lowerExpression(assignmentNode.rhs, instructions);
            TacVariable dst = new TacVariable(((IdentifierNode) assignmentNode.lhs).id);
            if (assignmentNode.op.op == AssignmentOperator.ASSIGN) {
                // 普通赋值
                instructions.add(new TacCopy(rhs, dst));
                if (rhs instanceof TacIntConstant intConstant) {
                    return intConstant;
                }
            } else {
                // 复合赋值
                instructions.add(new TacBinaryOperation(assignmentNode.op.op.getBinaryOperator(), dst, rhs, dst));
            }
            return dst;
        } else if (expression instanceof IdentifierNode identifierNode) {
            return new TacVariable(identifierNode.id);
        } else if (expression instanceof IncrementDecrementNode incrementDecrementNode) {
            // 自增自减表达式
            BinaryOperator op = incrementDecrementNode.isIncrement ? BinaryOperator.ADD : BinaryOperator.SUBTRACT;
            TacVariable dst = new TacVariable(((IdentifierNode) incrementDecrementNode.operand).id);
            if (incrementDecrementNode.isPrefix) {
                // ++/--a => a = a +/- 1; yield a;
                instructions.add(new TacBinaryOperation(op, dst, new TacIntConstant(1), dst));
                return dst;
            } else {
                // a++/-- => temp = a; a = a +/- 1; yield temp;
                TacVariable temp = new TacVariable(makeTempVar());
                instructions.add(new TacCopy(dst, temp));
                instructions.add(new TacBinaryOperation(op, dst, new TacIntConstant(1), dst));
                return temp;
            }
        } else if (expression instanceof ConditionalExpressionNode conditionalExpressionNode) {
            // 条件表达式
            String labelCondFalse = makeLabel("cond_false");
            switch (lowerBoolean(conditionalExpressionNode.cond, labelCondFalse, true, instructions)) {
                case ALWAYS_JUMP -> {
                    instructions.add(new TacLabel(labelCondFalse));
                    return lowerExpression(conditionalExpressionNode.elseExpr, instructions);
                }
                case NEVER_JUMP -> {
                    TacValue ret = lowerExpression(conditionalExpressionNode.thenExpr, instructions);
                    instructions.add(new TacLabel(labelCondFalse));
                    return ret;
                }
            }
            String labelCondEnd = makeLabel("cond_end");
            TacValue thenValue = lowerExpression(conditionalExpressionNode.thenExpr, instructions);
            TacVariable dst = new TacVariable(makeTempVar());
            instructions.add(new TacCopy(thenValue, dst));
            instructions.add(new TacJump(labelCondEnd));
            instructions.add(new TacLabel(labelCondFalse));
            TacValue elseValue = lowerExpression(conditionalExpressionNode.elseExpr, instructions);
            instructions.add(new TacCopy(elseValue, dst));
            instructions.add(new TacLabel(labelCondEnd));
            return dst;
        }
        throw new UnsupportedOperationException(
            "Unsupported expression type: " + expression.getClass().getSimpleName());
    }

    /**
     * 对表达式进行求值，在求值末尾处生成“若求值结果为真”则跳转的指令
     *
     * @param expression   要求值的表达式
     * @param jumpTarget   生成跳转指令的跳转目标
     * @param inverse      是否将表达式条件取反
     * @param instructions 三地址码生成目标
     * @return 如果生成的跳转指令无条件跳转则返回 {@link BooleanGenerationResult#ALWAYS_JUMP}，如果永不跳转则返回
     * {@link BooleanGenerationResult#NEVER_JUMP}，否则返回 {@link BooleanGenerationResult#VARIOUS}。
     * 返回 {@link BooleanGenerationResult#ALWAYS_JUMP} 或 {@link BooleanGenerationResult#NEVER_JUMP} 时将不生成任何跳转指令
     */
    private BooleanGenerationResult lowerBoolean(
        ExpressionNode expression, String jumpTarget, boolean inverse, List<TacInstruction> instructions) {

        if (expression instanceof IntConstantNode intConstantNode) {
            // 常量，生成无条件跳转
            if ((intConstantNode.value != 0) ^ inverse) {
                return BooleanGenerationResult.ALWAYS_JUMP;
            } else {
                return BooleanGenerationResult.NEVER_JUMP;
            }
        }

        if (expression instanceof BinaryExpressionNode binaryExpressionNode) {
            switch (binaryExpressionNode.op.op) {
                case LOGICAL_AND -> {
                    if (inverse) {
                        // if (!(a && b)) jump => if (!a) jump ; if (!b) jump
                        switch (lowerBoolean(binaryExpressionNode.lhs, jumpTarget, true, instructions)) {
                            case VARIOUS -> {
                                if (lowerBoolean(
                                    binaryExpressionNode.rhs, jumpTarget, true, instructions) ==
                                    BooleanGenerationResult.ALWAYS_JUMP) {
                                    return BooleanGenerationResult.ALWAYS_JUMP;
                                } else {
                                    return BooleanGenerationResult.VARIOUS;
                                }
                            }
                            case ALWAYS_JUMP -> {
                                return BooleanGenerationResult.ALWAYS_JUMP;
                            }
                            case NEVER_JUMP -> {
                                return lowerBoolean(binaryExpressionNode.lhs, jumpTarget, true, instructions);
                            }
                        }
                        lowerBoolean(binaryExpressionNode.rhs, jumpTarget, true, instructions);
                    } else {
                        // if (a && b) jump => if (!a) jump false ; if (b) jump
                        String label = makeLabel("and_false");
                        BooleanGenerationResult ret = BooleanGenerationResult.VARIOUS;
                        switch (lowerBoolean(binaryExpressionNode.lhs, label, true, instructions)) {
                            case VARIOUS -> {
                                if (lowerBoolean(
                                    binaryExpressionNode.rhs, jumpTarget, false, instructions) ==
                                    BooleanGenerationResult.NEVER_JUMP) {
                                    ret = BooleanGenerationResult.NEVER_JUMP;
                                }
                            }
                            case ALWAYS_JUMP -> {
                                ret = BooleanGenerationResult.NEVER_JUMP;
                            }
                            case NEVER_JUMP -> {
                                ret = lowerBoolean(binaryExpressionNode.lhs, jumpTarget, false, instructions);
                            }
                        }
                        instructions.add(new TacLabel(label));
                        return ret;
                    }
                }
                case LOGICAL_OR -> {
                    if (inverse) {
                        // if (!(a || b)) jump => if (a) jump false ; if (!b) jump
                        String label = makeLabel("or_false");
                        BooleanGenerationResult ret = BooleanGenerationResult.VARIOUS;
                        switch (lowerBoolean(binaryExpressionNode.lhs, label, false, instructions)) {
                            case VARIOUS -> {
                                if (lowerBoolean(
                                    binaryExpressionNode.rhs, jumpTarget, true, instructions) ==
                                    BooleanGenerationResult.NEVER_JUMP) {
                                    ret = BooleanGenerationResult.NEVER_JUMP;
                                }
                            }
                            case ALWAYS_JUMP -> {
                                ret = BooleanGenerationResult.NEVER_JUMP;
                            }
                            case NEVER_JUMP -> {
                                ret = lowerBoolean(binaryExpressionNode.lhs, jumpTarget, true, instructions);
                            }
                        }
                        instructions.add(new TacLabel(label));
                        return ret;
                    } else {
                        // if (a || b) jump => if (a) jump ; if (b) jump
                        switch (lowerBoolean(binaryExpressionNode.lhs, jumpTarget, false, instructions)) {
                            case VARIOUS -> {
                                if (lowerBoolean(
                                    binaryExpressionNode.rhs, jumpTarget, false, instructions) ==
                                    BooleanGenerationResult.ALWAYS_JUMP) {
                                    return BooleanGenerationResult.ALWAYS_JUMP;
                                } else {
                                    return BooleanGenerationResult.VARIOUS;
                                }
                            }
                            case ALWAYS_JUMP -> {
                                return BooleanGenerationResult.ALWAYS_JUMP;
                            }
                            case NEVER_JUMP -> {
                                return lowerBoolean(binaryExpressionNode.lhs, jumpTarget, false, instructions);
                            }
                        }
                    }
                }
                case EQUAL, NOT_EQUAL, LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL -> {
                    // 直接生成比较跳转指令，而不是比较置位指令
                    Comparison cond = switch (binaryExpressionNode.op.op) {
                        case EQUAL -> inverse ? Comparison.NOT_EQUAL : Comparison.EQUAL;
                        case NOT_EQUAL -> inverse ? Comparison.EQUAL : Comparison.NOT_EQUAL;
                        case LESS_THAN -> inverse ? Comparison.GREATER_EQUAL : Comparison.LESS;
                        case LESS_OR_EQUAL -> inverse ? Comparison.GREATER : Comparison.LESS_EQUAL;
                        case GREATER_THAN -> inverse ? Comparison.LESS_EQUAL : Comparison.GREATER;
                        case GREATER_OR_EQUAL -> inverse ? Comparison.LESS : Comparison.GREATER_EQUAL;
                        default -> throw new IllegalStateException("Control should never reach here");
                    };

                    TacValue lhs = lowerExpression(binaryExpressionNode.lhs, instructions);
                    TacValue rhs = lowerExpression(binaryExpressionNode.rhs, instructions);
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
                            return BooleanGenerationResult.ALWAYS_JUMP;
                        } else {
                            return BooleanGenerationResult.NEVER_JUMP;
                        }
                    }
                    instructions.add(new TacJumpIfComparison(cond, lhs, rhs, jumpTarget));
                    return BooleanGenerationResult.VARIOUS;
                }
            }
        } else if (expression instanceof UnaryExpressionNode unaryExpressionNode) {
            if (unaryExpressionNode.op.op == UnaryOperator.NOT) {
                return lowerBoolean(unaryExpressionNode.exp, jumpTarget, !inverse, instructions);
            }
        }

        // 其他表达式，先求值再与 0 比较跳转
        TacValue value = lowerExpression(expression, instructions);
        if (value instanceof TacIntConstant intConstant) {
            if ((intConstant.value != 0) ^ inverse) {
                return BooleanGenerationResult.ALWAYS_JUMP;
            } else {
                return BooleanGenerationResult.NEVER_JUMP;
            }
        }
        if (inverse) {
            instructions.add(new TacJumpIfZero(value, jumpTarget));
        } else {
            instructions.add(new TacJumpIfNotZero(value, jumpTarget));
        }
        return BooleanGenerationResult.VARIOUS;
    }

    /**
     * 作为 {@link #lowerBoolean(ExpressionNode, String, boolean, List)} 的返回值
     */
    private enum BooleanGenerationResult {
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
