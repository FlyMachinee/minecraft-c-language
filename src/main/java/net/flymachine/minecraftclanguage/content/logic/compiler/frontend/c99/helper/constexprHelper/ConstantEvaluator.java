package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.constexprHelper;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantDouble;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantInt;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantSymbolPointer;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.PointerType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ConstantEvalVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.util.Optional;

public final class ConstantEvaluator implements ConstantEvalVisitor {

    private final SymbolTable symbolTable;
    private final DiagnosticReporter reporter;
    private final LValuePathEvaluator lValuePathEvaluator;

    public ConstantEvaluator(SymbolTable symbolTable, DiagnosticReporter reporter) {
        this.symbolTable = symbolTable;
        this.reporter = reporter;
        this.lValuePathEvaluator = new LValuePathEvaluator(symbolTable, this);
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

    /**
     * 尝试计算表达式的地址常量值，如果无法计算则返回第一次出错的源位置
     * <p>
     * ISO C99 6.6p3: 常量表达式不得包含赋值、自增、自减、函数调用或逗号运算符，除非它们包含在未被求值的子表达式中。
     * <p>
     * ISO C99 6.6p4: 每个常量表达式求值得到的常量应在其类型可表示的值范围内。
     * <p>
     * ISO C99 6.6p9: 地址常量是空指针、指向静态存储期对象左值的指针，或指向函数指代符的指针；它应通过一元 & 运算符，或通过强制转换为指针类型的整数常量来显式创建，也可以通过使用数组类型或函数类型的表达式隐式创建。数组下标 [] 和成员访问 . 与 -> 运算符、取地址 & 和间接寻址 * 一元运算符，以及指针强制转换，均可用于创建地址常量，但不得通过使用这些运算符来访问对象的值。
     *
     * @param exp 表达式节点
     * @return 如果计算成功返回左值，失败返回右值
     */
    public Either<Constant, SourceLocation> tryEvalAddressConstant(ExpressionNode exp) {
        return tryEvaluate(exp, ConstantCategory.ADDRESS);
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
            case INTEGER -> t.isInteger();
            case ARITHMETIC -> t.isArithmetic();
            case ADDRESS -> node.value.isNullPointer();
        } ? Either.left(node.value) : Either.right(node.wholeLoc);
    }

    @Override
    public Either<Constant, SourceLocation> visit(VariableNode node, ConstantCategory category) {
        return Either.right(node.wholeLoc);
    }

    private boolean typeAllowed(ConstantCategory category, Type type) {
        return switch (category) {
            case INTEGER -> type.isInteger();
            case ARITHMETIC -> type.isArithmetic();
            case ADDRESS -> type.isPointer();
        };
    }

    @Override
    public Either<Constant, SourceLocation> visit(UnaryExpressionNode node, ConstantCategory category) {
        Either<Constant, SourceLocation> operand = tryEvaluate(node.exp, category);
        if (operand.right().isPresent()) {
            return operand;
        }
        if (category == ConstantCategory.ADDRESS) {
            return Either.right(node.wholeLoc);
        }
        Either<Constant, String> result = operand.orThrow().tryApply(node.op.op, reporter);
        if (result.right().isPresent()) {
            reporter.warning(node.op.wholeLoc, result.right().get());
            return Either.right(node.wholeLoc);
        }
        Constant res = result.orThrow();
        return typeAllowed(category, res.getType()) ? Either.left(res) : Either.right(node.wholeLoc);
    }

    @Override
    public Either<Constant, SourceLocation> visit(BinaryExpressionNode node, ConstantCategory category) {
        if (category == ConstantCategory.ADDRESS) {
            return visitAddressBinary(node);
        }

        Either<Constant, SourceLocation> lhs = tryEvaluate(node.lhs, category);
        if (lhs.right().isPresent()) {
            return lhs;
        }

        if (node.op.op == BinaryOperator.LOGICAL_AND || node.op.op == BinaryOperator.LOGICAL_OR) {
            // 短路求值
            if (lhs.orThrow().isZero() && node.op.op == BinaryOperator.LOGICAL_AND) {
                return Either.left(ConstantInt.ZERO);
            }
            if (!lhs.orThrow().isZero() && node.op.op == BinaryOperator.LOGICAL_OR) {
                return Either.left(ConstantInt.ONE);
            }
        }

        Either<Constant, SourceLocation> rhs = tryEvaluate(node.rhs, category);
        if (rhs.right().isPresent()) {
            return rhs;
        }

        if (node.op.op == BinaryOperator.LOGICAL_AND || node.op.op == BinaryOperator.LOGICAL_OR) {
            return Either.left(rhs.orThrow().isZero() ? ConstantInt.ZERO : ConstantInt.ONE);
        }

        Either<Constant, String> result = lhs.orThrow().tryApply(node.op.op, rhs.orThrow(), reporter);
        if (result.right().isPresent()) {
            reporter.warning(node.op.wholeLoc, result.right().get());
            return Either.right(node.wholeLoc);
        }
        Constant res = result.orThrow();
        return typeAllowed(category, res.getType()) ? Either.left(res) : Either.right(node.wholeLoc);
    }

    private Either<Constant, SourceLocation> visitAddressBinary(BinaryExpressionNode node) {
        // 指针 + 整数 或 整数 + 指针
        if (node.op.op == BinaryOperator.ADD) {
            var lhs = tryEvaluate(node.lhs, ConstantCategory.ADDRESS);
            if (lhs.left().isPresent()) {
                var rhs = tryEvaluate(node.rhs, ConstantCategory.ARITHMETIC);
                if (rhs.left().isPresent()) {
                    return Either.left(lhs.orThrow().apply(BinaryOperator.ADD, rhs.orThrow()));
                }
                return rhs;
            }
            var rhs = tryEvaluate(node.rhs, ConstantCategory.ADDRESS);
            if (rhs.left().isPresent()) {
                var lhs2 = tryEvaluate(node.lhs, ConstantCategory.ARITHMETIC);
                if (lhs2.left().isPresent()) {
                    return Either.left(rhs.orThrow().apply(BinaryOperator.ADD, lhs2.orThrow()));
                }
                return lhs2;
            }
        }
        // 指针 - 整数
        else if (node.op.op == BinaryOperator.SUBTRACT) {
            var lhs = tryEvaluate(node.lhs, ConstantCategory.ADDRESS);
            if (lhs.left().isPresent()) {
                var rhs = tryEvaluate(node.rhs, ConstantCategory.ARITHMETIC);
                if (rhs.left().isPresent()) {
                    return Either.left(lhs.orThrow().apply(BinaryOperator.SUBTRACT, rhs.orThrow()));
                }
                return rhs;
            }
        }
        return Either.right(node.wholeLoc);
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
                    var res = tryEvaluate(node.exp, ConstantCategory.INTEGER);
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
                var res = tryEvaluate(node.exp, ConstantCategory.ARITHMETIC);
                if (res.right().isPresent()) {
                    return res;
                }
                return Either.left(res.orThrow().castTo(targetType));
            }
            case ADDRESS -> {
                // 必须是转换至指针类型
                if (!targetType.isPointer()) {
                    return Either.right(node.wholeLoc);
                }
                // 由类型检查保证肯定为标量类型
                if (node.exp.expType.isPointer()) {
                    // 指针转指针
                    var res = tryEvaluate(node.exp, ConstantCategory.ADDRESS);
                    if (res.right().isPresent()) {
                        return res;
                    }
                    return Either.left(res.orThrow().castTo(targetType));
                } else {
                    // 由类型检查保证肯定为整数类型
                    assert node.exp.expType.isInteger();
                    var res = tryEvaluate(node.exp, ConstantCategory.ARITHMETIC);
                    if (res.right().isPresent()) {
                        return res;
                    }
                    // 不可能是浮点类型，从而该 cast 是安全的
                    return Either.left(res.orThrow().castTo(targetType));
                }
            }
        }
        return Either.right(node.wholeLoc);
    }

    @Override
    public Either<Constant, SourceLocation> visit(ConditionalExpressionNode node, ConstantCategory category) {
        Either<Constant, SourceLocation> cond = switch (category) {
            case INTEGER -> tryEvaluate(node.cond, ConstantCategory.INTEGER);
            case ARITHMETIC -> tryEvaluate(node.cond, ConstantCategory.ARITHMETIC);
            case ADDRESS -> {
                var tmp = tryEvaluate(node.cond, ConstantCategory.ARITHMETIC);
                if (tmp.left().isPresent()) {
                    yield tmp;
                }
                yield tryEvaluate(node.cond, ConstantCategory.ADDRESS);
            }
        };
        if (cond.right().isPresent()) {
            return cond;
        }
        return tryEvaluate(cond.orThrow().isZero() ? node.elseExp : node.thenExp, category);
    }

    @Override
    public Either<Constant, SourceLocation> visit(AddressOfNode node, ConstantCategory category) {
        if (category != ConstantCategory.ADDRESS) {
            return Either.right(node.wholeLoc);
        }

        PointerType pt = (PointerType) node.expType;

        // &*p -> p
        if (node.exp instanceof DereferenceNode deref) {
            return deref.exp.accept(this, ConstantCategory.ADDRESS);
        }

        // &name
        if (node.exp instanceof VariableNode variable) {
            SymbolTable.Entry entry = symbolTable.get(variable.id.name);
            if (entry == null) {
                return Either.right(variable.wholeLoc);
            }
            if (entry.attr instanceof SymbolTable.Entry.StaticAttr ||
                entry.attr instanceof SymbolTable.Entry.FuncAttr) {
                // 不能直接用符号本身的类型，由于有数组到指向其元素的衰减
                return Either.left(new ConstantSymbolPointer(variable.id.name, pt.referencedType()));
            }
            return Either.right(variable.wholeLoc);
        }

        // &左值对象
        Optional<LValuePath> path = lValuePathEvaluator.tryEvalPath(node.exp);
        return path.<Either<Constant, SourceLocation>>map(
                       lValuePath -> Either.left(
                           lValuePathEvaluator.pathToAddress(lValuePath).toPointer(pt.referencedType())))
                   .orElseGet(() -> Either.right(node.exp.wholeLoc));
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
