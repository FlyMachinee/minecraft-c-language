package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.constexprHelper;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantDouble;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ConstantEvalVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class ConstantEvaluator implements ConstantEvalVisitor {

    private final SymbolTable symbolTable;

    public ConstantEvaluator(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * 尝试计算表达式的整数常量值，如果无法计算则返回第一次出错的源位置
     * <p>
     * ISO C99 6.6p3: 常量表达式不得包含赋值、自增、自减、函数调用或逗号运算符，除非它们包含在未被求值的子表达式中。
     * <p>
     * ISO C99 6.6p4: 每个常量表达式求值得到的常量应在其类型可表示的值范围内。
     * <p>
     * ISO C99 6.6p6: 整数常量表达式应具有整数类型，并且其操作数只能为整数常量、枚举常量、字符常量、结果为整数常量的 sizeof 表达式，以及作为强制转换的直接操作数的浮点常量。整数常量表达式中的强制转换运算符只能将算术类型转换为整数类型，除非它是 sizeof 运算符操作数的一部分
     *
     * @param exp 表达式节点
     * @return 如果计算成功返回左值，失败返回右值
     */
    public Either<Constant, SourceLocation> tryEvalIntegerConstant(ExpressionNode exp) {
        return tryEvaluate(exp, ConstantCategory.INTEGER);
    }

    /**
     * 尝试计算表达式的算术常量值，如果无法计算则返回第一次出错的源位置
     * <p>
     * ISO C99 6.6p3: 常量表达式不得包含赋值、自增、自减、函数调用或逗号运算符，除非它们包含在未被求值的子表达式中。
     * <p>
     * ISO C99 6.6p4: 每个常量表达式求值得到的常量应在其类型可表示的值范围内。
     * <p>
     * ISO C99 6.6p8: 算术常量表达式应具有算术类型，并且其操作数只能为整数常量、浮点常量、枚举常量、字符常量和 sizeof 表达式。算术常量表达式中的强制转换运算符只能将算术类型转换为算术类型，除非它是某个 sizeof 运算符操作数的一部分，且该 sizeof 运算符的结果为整数常量。
     *
     * @param exp 表达式节点
     * @return 如果计算成功返回左值，失败返回右值
     */
    public Either<Constant, SourceLocation> tryEvalArithmeticConstant(ExpressionNode exp) {
        return tryEvaluate(exp, ConstantCategory.ARITHMETIC);
    }

    public Either<Constant, SourceLocation> tryEvaluate(ExpressionNode exp, ConstantCategory category) {
        if (exp.expType == null || exp.expType.isError()) {
            return Either.right(exp.wholeLoc);
        }
        return exp.accept(this, category);
    }

    @Override
    public Either<Constant, SourceLocation> visit(ConstantNode node, ConstantCategory category) {
        Type t = node.expType;
        return switch (category) {
            case INTEGER -> t.isInteger() ? Either.left(node.value) : Either.right(node.wholeLoc);
            case ARITHMETIC -> t.isArithmetic() ? Either.left(node.value) : Either.right(node.wholeLoc);
        };
    }

    @Override
    public Either<Constant, SourceLocation> visit(VariableNode node, ConstantCategory category) {
        return Either.right(node.wholeLoc);
    }

    private boolean typeAllowed(ConstantCategory category, Type type) {
        return switch (category) {
            case INTEGER -> type.isInteger();
            case ARITHMETIC -> type.isArithmetic();
        };
    }

    @Override
    public Either<Constant, SourceLocation> visit(UnaryExpressionNode node, ConstantCategory category) {
        Either<Constant, SourceLocation> operand = tryEvaluate(node.exp, category);
        if (operand.right().isPresent()) {
            return operand;
        }
        Constant result = operand.orThrow().apply(node.op.op);
        return typeAllowed(category, result.getType()) ? Either.left(result) : Either.right(node.wholeLoc);
    }

    @Override
    public Either<Constant, SourceLocation> visit(BinaryExpressionNode node, ConstantCategory category) {
        Either<Constant, SourceLocation> lhs = tryEvaluate(node.lhs, category);
        if (lhs.right().isPresent()) {
            return lhs;
        }

        Either<Constant, SourceLocation> rhs = tryEvaluate(node.rhs, category);
        if (rhs.right().isPresent()) {
            return rhs;
        }

        Constant result = lhs.orThrow().apply(node.op.op, rhs.orThrow());
        return typeAllowed(category, result.getType()) ? Either.left(result) : Either.right(node.wholeLoc);
    }

    @Override
    public Either<Constant, SourceLocation> visit(CastExpressionNode node, ConstantCategory category) {
        Type targetType = node.expType;

        switch (category) {
            case INTEGER -> {
                // 转换运算符只能转换算术类型为整数类型
                if (!targetType.isInteger()) {
                    return Either.right(node.wholeLoc);
                }
                if (!node.exp.expType.isArithmetic()) {
                    return Either.right(node.exp.wholeLoc);
                }

                Constant toCast;
                // 浮点数常量，但仅当其作为向整数类型转换的直接操作数
                if (node.exp instanceof ConstantNode constNode &&
                    constNode.value instanceof ConstantDouble constDouble) {
                    toCast = constDouble;
                } else {
                    Either<Constant, SourceLocation> res = tryEvaluate(node.exp, ConstantCategory.INTEGER);
                    if (res.right().isPresent()) {
                        return res;
                    }
                    toCast = res.orThrow();
                }
                return Either.left(toCast.castTo(targetType));
            }
            case ARITHMETIC -> {
                // 转换运算符只能转换算术类型为算术类型
                if (!targetType.isArithmetic()) {
                    return Either.right(node.wholeLoc);
                }
                if (!node.exp.expType.isArithmetic()) {
                    return Either.right(node.exp.wholeLoc);
                }
                Either<Constant, SourceLocation> res = tryEvaluate(node.exp, ConstantCategory.ARITHMETIC);
                if (res.right().isPresent()) {
                    return res;
                }
                return Either.left(res.orThrow().castTo(targetType));
            }
        }
        return Either.right(node.wholeLoc);
    }

    @Override
    public Either<Constant, SourceLocation> visit(ConditionalExpressionNode node, ConstantCategory category) {
        Either<Constant, SourceLocation> cond = switch (category) {
            case INTEGER -> tryEvaluate(node.cond, ConstantCategory.INTEGER);
            case ARITHMETIC -> tryEvaluate(node.cond, ConstantCategory.ARITHMETIC);
        };
        if (cond.right().isPresent()) {
            return cond;
        }
        return tryEvaluate(cond.orThrow().isZero() ? node.elseExp : node.thenExp, category);
    }

    @Override
    public Either<Constant, SourceLocation> visit(AddressOfNode node, ConstantCategory category) {
        return Either.right(node.wholeLoc);
    }

    @Override
    public Either<Constant, SourceLocation> visit(DereferenceNode node, ConstantCategory category) {
        return Either.right(node.wholeLoc);
    }

    @Override
    public Either<Constant, SourceLocation> visit(SubscriptNode node, ConstantCategory category) {
        return Either.right(node.wholeLoc);
    }
}
