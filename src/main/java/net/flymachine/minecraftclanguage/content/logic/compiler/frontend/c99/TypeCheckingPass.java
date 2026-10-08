package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.AssignmentOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.StorageClassSpecifier;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantLong;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantUnsignedLong;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.constexprHelper.ConstantEvaluator;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.initHelper.InitializerHelper;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * 进行类型检查等工作
 * <p>
 * 需要先进行 {@link IdentifierResolutionPass}
 */
public final class TypeCheckingPass implements AstVisitor<Void> {

    private final DiagnosticReporter reporter;
    private final InitializerHelper initializerHelper;
    private final ConstantEvaluator constantEvaluator;

    public TypeCheckingPass(DiagnosticReporter reporter) {
        this.reporter = reporter;
        this.initializerHelper = new InitializerHelper(reporter, this);
        this.constantEvaluator = new ConstantEvaluator(symbolTable, reporter);
    }

    private final SymbolTable symbolTable = new SymbolTable();
    private FunctionDefinitionNode functionContext = null;

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    @Override
    public Void visit(ProgramNode node) {
        for (ExternalDeclarationNode externalDeclaration : node.extDecls) {
            externalDeclaration.accept(this);
        }
        // 处理未知长度数组
        for (SymbolTable.Entry entry : symbolTable.getEntries()) {
            if (!(entry.attr instanceof SymbolTable.Entry.StaticAttr staticAttr)) {
                continue;
            }

            if (staticAttr.defType instanceof SymbolTable.Entry.StaticAttr.Tentative && staticAttr.global) {
                if (entry.type instanceof ArrayType arrayType && arrayType.size().isZero()) {
                    entry.type = arrayType.withSize(ConstantUnsignedLong.ONE);
                    String msg = "array '" + reporter.white(entry.id.name) + "' assumed to have one element";
                    reporter.warning(entry.id.wholeLoc, msg);
                }
            }
        }
        return null;
    }

    @Override
    public Void visit(FunctionDefinitionNode node) {
        if (!(node.funcType instanceof FunctionTypeNode funcType)) {
            // 不是函数类型
            String msg = "name declared in a function definition shall have a function type; have '" +
                         reporter.white(node.funcType.typename()) + "'";
            reporter.error(node.id.wholeLoc, msg);
        } else {
            visitFunctionDeclaration(node.id, funcType, node.storageClass, true);
        }
        // 检查函数体
        functionContext = node;
        visit(node.body);
        functionContext = null;
        return null;
    }

    private void checkStatementLabel(StatementNode statement) {
        for (var caseLabel : statement.caseLabels) {
            caseLabel.caseValue = checkExpressionAndDecay(caseLabel.caseValue);
            if (caseLabel.caseValue.expType.isError()) { continue; }

            Either<Constant, SourceLocation> evalResult = constantEvaluator.tryEvalIntegerConstant(caseLabel.caseValue);
            if (evalResult.right().isPresent()) {
                reporter.error(evalResult.right().get(), "case label does not reduce to an integer constant");
                continue;
            }

            caseLabel.caseValue = new ConstantNode(caseLabel.caseValue.wholeLoc, evalResult.orThrow());
        }
    }

    @Override
    public Void visit(ReturnNode node) {
        checkStatementLabel(node);

        // 有可能经过 merge，需要从符号表获取最终类型
        // e.g.
        //
        // int (**foo(int))[26];
        // int (**foo(int))[] { ... } 类型为 int (**(int))[26]
        Type ft = symbolTable.get(functionContext.id.name).type;
        if (!(ft instanceof FunctionType funcType)) {
            // 函数定义的类型不是函数类型，先前已报错
            if (node.exp != null) {
                checkExpression(node.exp);
            }
            return null;
        }

        Type retType = funcType.returnType();
        if (retType.isVoid()) {
            // 从无返回值的函数返回时，不能返回表达式
            if (node.exp != null) {
                String msg = "'" + reporter.white("return") + "' with a value, in function returning void";
                reporter.error(node.exp.wholeLoc, msg);
                checkExpression(node.exp);
            }
        } else {
            // 从有返回值的函数返回时，必须返回一个表达式
            if (node.exp == null) {
                String msg = "'" + reporter.white("return") + "' with no value, in function returning non-void";
                reporter.error(node.wholeLoc, msg);
                return null;
            }

            node.exp = checkExpressionAndDecay(node.exp);
            if (node.exp.expType.isError()) { return null; }
            node.exp.expType = node.exp.expType.removeConst();
            // 若表达式的类型与函数的返回类型不同，则如同赋值给该函数返回类型的对象一般对其值进行转换
            if (!validConvertAsIfByAssignment(node.exp, retType)) {
                String msg =
                    "incompatible types when returning type '" + reporter.white(node.exp.expType.typename()) +
                    "' but '" + reporter.white(retType.typename()) + "' was expected";
                reporter.error(node.exp.wholeLoc, msg);
                return null;
            }
            node.exp = convertTo(node.exp, retType);
        }
        return null;
    }

    public ExpressionNode convertTo(ExpressionNode exp, Type type) {
        if (exp.expType.isCompatible(type)) {
            return exp;
        }
        if (exp.expType.isError() || type.isError()) {
            exp.expType = ErrorType.INSTANCE;
            return exp;
        }

        ExpressionNode ret = new CastExpressionNode(exp.wholeLoc, TypeNode.fromType(type), exp);
        ret.expType = type;
        return ret;
    }

    public void checkExpression(ExpressionNode exp) {
        exp.accept(this);
    }

    /**
     * 检查表达式，并进行数组到指针、函数到指针的衰减
     * <p>
     * 数组到指针转换：
     * <p>
     * 任何数组类型的表达式(C99 起)，在用于下列语境之外时
     * <p>
     * - 作为取址运算符的操作数
     * <p>
     * - 作为 sizeof 的操作数
     * <p>
     * - 作为用于数组初始化的字符串字面量
     * <p>
     * 会经历到指向其首元素的非左值指针的转换
     * <p>
     * 函数到指针转换
     * <p>
     * 任何函数指代器表达式，在用于异于下列语境时
     * <p>
     * - 作为取址运算符的操作
     * <p>
     * - 作为 sizeof 的操作数
     * <p>
     * 会经历到指向表达式所指代函数的指针的转换
     *
     * @param exp 表达式
     * @return 衰减后的表达式
     */
    public ExpressionNode checkExpressionAndDecay(ExpressionNode exp) {
        checkExpression(exp);

        if (exp.expType instanceof ArrayType at) {
            // 数组类型衰减为指针类型
            AddressOfNode addrExp = new AddressOfNode(null, exp);
            addrExp.expType = new PointerType(at.elementType());
            return addrExp;
        }

        if (exp.expType instanceof FunctionType ft) {
            // 函数类型衰减为指针类型
            AddressOfNode addrExp = new AddressOfNode(null, exp);
            addrExp.expType = new PointerType(ft);
            return addrExp;
        }
        return exp;
    }

    @Override
    public Void visit(UnaryExpressionNode node) {
        node.exp = checkExpressionAndDecay(node.exp);
        if (node.exp.expType.isError()) {
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        node.exp.expType = node.exp.expType.removeConst();

        node.expType = switch (node.op.op) {
            case POSITIVE, NEGATE -> {
                if (!node.exp.expType.isArithmetic()) {
                    String type = node.op.op == UnaryOperator.POSITIVE ? "plus" : "minus";
                    String msg = "operand of unary " + type + " must have arithmetic type; have '" +
                                 reporter.white(node.exp.expType.typename()) + "'";
                    reporter.error(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }
                // 一元加和一元减都首先在其操作数上应用整数提升
                // 表达式类型为提升后的类型
                Type operandType = node.exp.expType;
                if (operandType.isCharacter()) {
                    node.exp = convertTo(node.exp, BasicType.INT);
                    yield BasicType.INT;
                }
                yield node.exp.expType;
            }
            case COMPLEMENT -> {
                if (!node.exp.expType.isInteger()) {
                    String msg = "operand of bitwise complement must have integer type; have '" +
                                 reporter.white(node.exp.expType.typename()) + "'";
                    reporter.error(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }
                // 运算符 ~ 在其唯一的操作数上进行整数提升
                Type operandType = node.exp.expType;
                if (operandType.isCharacter()) {
                    node.exp = convertTo(node.exp, BasicType.INT);
                    yield BasicType.INT;
                }
                yield node.exp.expType;
            }
            case NOT -> {
                if (!node.exp.expType.isScalar()) {
                    String msg = "operand of logical negation must have scalar type; have '" +
                                 reporter.white(node.exp.expType.typename()) + "'";
                    reporter.error(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }
                yield BasicType.INT;
            }
        };
        return null;
    }

    @Override
    public Void visit(BinaryExpressionNode node) {
        node.lhs = checkExpressionAndDecay(node.lhs);
        node.rhs = checkExpressionAndDecay(node.rhs);
        typeCheckBinaryExp(node);
        return null;
    }

    public boolean isNullPointerConstant(ExpressionNode exp) {
        Either<Constant, SourceLocation> evalResult = constantEvaluator.tryEvalArithmeticConstant(exp);
        if (evalResult.right().isPresent()) {
            return false;
        }
        return evalResult.orThrow().isNullPointer();
    }

    // private Type getCommonPointerType(ExpressionNode lhs, ExpressionNode rhs) {
    //     Type lhsType = lhs.expType;
    //     Type rhsType = rhs.expType;
    //     if (lhsType.isCompatible(rhsType)) {
    //         return lhsType;
    //     }
    //     if (isNullPointerConstant(lhs)) {
    //         return rhsType;
    //     }
    //     if (isNullPointerConstant(rhs)) {
    //         return lhsType;
    //     }
    //     return ErrorType.INSTANCE;
    // }

    private void typeCheckBinaryExp(BinaryExpressionNode node) {
        if (node.lhs.expType.isError() || node.rhs.expType.isError()) {
            node.expType = ErrorType.INSTANCE;
            return;
        }
        // 更新时，需同时更新 typeCheckCompoundAssignment 中检查
        Type lhsType = node.lhs.expType = node.lhs.expType.removeConst();
        Type rhsType = node.rhs.expType = node.rhs.expType.removeConst();

        node.expType = switch (node.op.op) {
            case ADD -> {
                // lhs 与 rhs 必须为下列之一
                // 都拥有算术类型，包含复数和虚数
                if (lhsType.isArithmetic() && rhsType.isArithmetic()) {
                    BasicType commonType = Type.commonRealType((BasicType) lhsType, (BasicType) rhsType);
                    node.lhs = convertTo(node.lhs, commonType);
                    node.rhs = convertTo(node.rhs, commonType);
                    yield commonType;
                }
                // 一个是指向完整对象的指针类型，另一个拥有整数类型
                PointerType ptr;
                if (lhsType instanceof PointerType lhsPt && rhsType.isInteger()) {
                    node.rhs = convertTo(node.rhs, BasicType.LONG);
                    ptr = lhsPt;
                } else if (lhsType.isInteger() && rhsType instanceof PointerType rhsPt) {
                    node.lhs = convertTo(node.lhs, BasicType.LONG);
                    ptr = rhsPt;
                } else {
                    String msg = "invalid operands to binary operator + (have '" +
                                 reporter.white(lhsType.typename()) + "' and '" +
                                 reporter.white(rhsType.typename()) + "')";
                    reporter.error(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }

                Type referencedType = ptr.referencedType();
                if (!referencedType.isComplete()) {
                    String msg = "pointer to incomplete type '" + reporter.white(referencedType.typename()) +
                                 "' used in addition";
                    reporter.error(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }
                if (referencedType.isFunction()) {
                    String msg = "pointer to function type '" + reporter.white(referencedType.typename()) +
                                 "' used in addition";
                    reporter.error(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }

                yield ptr;
            }
            case SUBTRACT -> {
                // lhs 与 rhs 必须为下列之一
                // 都拥有算术类型，包含复数和虚数
                if (lhsType.isArithmetic() && rhsType.isArithmetic()) {
                    BasicType commonType = Type.commonRealType((BasicType) lhsType, (BasicType) rhsType);
                    node.lhs = convertTo(node.lhs, commonType);
                    node.rhs = convertTo(node.rhs, commonType);
                    yield commonType;
                }

                Function<PointerType, Boolean> check = (pt) -> {
                    Type referencedType = pt.referencedType();
                    if (!referencedType.isComplete()) {
                        String msg = "pointer to incomplete type '" + reporter.white(referencedType.typename()) +
                                     "' used in subtraction";
                        reporter.error(node.op.wholeLoc, msg);
                        return false;
                    }
                    if (referencedType.isFunction()) {
                        String msg = "pointer to function type '" + reporter.white(referencedType.typename()) +
                                     "' used in subtraction";
                        reporter.error(node.op.wholeLoc, msg);
                        return false;
                    }
                    return true;
                };

                // lhs 拥有指向完整对象的指针类型，rhs 拥有整数类型
                if (lhsType instanceof PointerType lhsPt && rhsType.isInteger()) {
                    node.rhs = convertTo(node.rhs, BasicType.LONG);
                    yield check.apply(lhsPt) ? lhsPt : ErrorType.INSTANCE;
                }
                // 都是指向拥有兼容类型的完整对象指针，忽略限定符
                if (lhsType instanceof PointerType lhsPt && rhsType instanceof PointerType rhsPt) {
                    if (lhsPt.referencedType().removeQualifiers()
                             .isCompatible(rhsPt.referencedType().removeQualifiers())) {
                        yield check.apply(lhsPt) && check.apply(rhsPt) ? BasicType.LONG : ErrorType.INSTANCE;
                    }
                }

                String msg = "invalid operands to binary operator - (have '" +
                             reporter.white(lhsType.typename()) + "' and '" +
                             reporter.white(rhsType.typename()) + "')";
                reporter.error(node.op.wholeLoc, msg);
                yield ErrorType.INSTANCE;
            }
            case MULTIPLY, DIVIDE -> {
                if (!lhsType.isArithmetic() || !rhsType.isArithmetic()) {
                    String msg = "operands of binary operator " + node.op.op.getSymbol() +
                                 " must have arithmetic type; have '" +
                                 reporter.white(lhsType.typename()) + "' and '" +
                                 reporter.white(rhsType.typename()) + "'";
                    reporter.error(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }
                BasicType commonType = Type.commonRealType((BasicType) lhsType, (BasicType) rhsType);
                node.lhs = convertTo(node.lhs, commonType);
                node.rhs = convertTo(node.rhs, commonType);
                yield commonType;
            }
            case MODULO, BITWISE_AND, BITWISE_OR, BITWISE_XOR -> {
                if (!lhsType.isInteger() || !rhsType.isInteger()) {
                    String msg = "operands of binary operator " + node.op.op.getSymbol() +
                                 " must have integer type; have '" +
                                 reporter.white(lhsType.typename()) + "' and '" +
                                 reporter.white(rhsType.typename()) + "'";
                    reporter.error(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }
                BasicType commonType = Type.commonRealType((BasicType) lhsType, (BasicType) rhsType);
                node.lhs = convertTo(node.lhs, commonType);
                node.rhs = convertTo(node.rhs, commonType);
                yield commonType;
            }
            case LEFT_SHIFT, RIGHT_SHIFT -> {
                if (!lhsType.isInteger() || !rhsType.isInteger()) {
                    String msg = "operands of binary operator " + node.op.op.getSymbol() +
                                 " must have integer type; have '" +
                                 reporter.white(lhsType.typename()) + "' and '" +
                                 reporter.white(rhsType.typename()) + "'";
                    reporter.error(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }
                // 进行整数提升
                if (lhsType.isCharacter()) {
                    node.lhs = convertTo(node.lhs, BasicType.INT);
                }
                if (rhsType.isCharacter()) {
                    node.rhs = convertTo(node.rhs, BasicType.INT);
                }
                yield node.lhs.expType;
            }
            case LOGICAL_AND, LOGICAL_OR -> {
                if (!lhsType.isScalar() || !rhsType.isScalar()) {
                    String msg = "operands of logical operator " + node.op.op.getSymbol() +
                                 " must have scalar type; have '" +
                                 reporter.white(lhsType.typename()) + "' and '" +
                                 reporter.white(rhsType.typename()) + "'";
                    reporter.error(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }
                yield BasicType.INT;
            }
            case LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL -> {
                // 若 lhs 和 rhs 是任何实数类型的表达式，则
                if (lhsType.isReal() && rhsType.isReal()) {
                    // 进行一般算术转换
                    BasicType commonType = Type.commonRealType((BasicType) lhsType, (BasicType) rhsType);
                    node.lhs = convertTo(node.lhs, commonType);
                    node.rhs = convertTo(node.rhs, commonType);
                    yield BasicType.INT;
                }
                // 若 lhs 和 rhs 是指针类型的表达式
                if (lhsType instanceof PointerType lhsPt && rhsType instanceof PointerType rhsPt) {
                    Type lhsPointee = lhsPt.referencedType().removeQualifiers();
                    Type rhsPointee = rhsPt.referencedType().removeQualifiers();
                    // 要么都指向忽略限定的兼容对象类型
                    // 要么都指向忽略限定的兼容不完整类型
                    // 也就是指向非函数类型的兼容类型
                    if (!lhsPointee.isFunction() && lhsPointee.isCompatible(rhsPointee)) {
                        yield BasicType.INT;
                    }
                }
                String msg = "cannot compare between '" + reporter.white(lhsType.typename()) + "' and '" +
                             reporter.white(rhsType.typename()) + "'";
                reporter.error(node.op.wholeLoc, msg);
                yield ErrorType.INSTANCE;
            }
            case EQUAL, NOT_EQUAL -> {
                Type commonType = ErrorType.INSTANCE;
                if (lhsType.isArithmetic() && rhsType.isArithmetic()) {
                    // 若两个运算数都拥有算术类型，则进行一般算术转换，而以通常数学意义比较所得值
                    commonType = Type.commonRealType((BasicType) lhsType, (BasicType) rhsType);
                } else if (lhsType.isPointer() || rhsType.isPointer()) {
                    // 若一个操作数为指针而另一空指针常量，则首先转换空指针常量为该指针的类型（给出空指针值），并以后述方式比较两个指针
                    if (isNullPointerConstant(node.lhs)) {
                        commonType = rhsType;
                    } else if (isNullPointerConstant(node.rhs)) {
                        commonType = lhsType;
                    } else if (lhsType instanceof PointerType lhsPt && rhsType instanceof PointerType rhsPt) {
                        Type lhsPointee = lhsPt.referencedType().removeQualifiers();
                        Type rhsPointee = rhsPt.referencedType().removeQualifiers();
                        // 一个指向对象或不完整类型（即非函数）的指针，另一个指向（可有限定的）void
                        // 前者将会被转换为后者的类型
                        if (!lhsPointee.isFunction() && rhsPointee.isVoid()) {
                            commonType = rhsType;
                        } else if (!rhsPointee.isFunction() && lhsPointee.isVoid()) {
                            commonType = lhsType;
                        } else if (lhsPointee.isCompatible(rhsPointee)) {
                            // 都是指向兼容类型的指针，忽略所指向类型的限定符
                            commonType = lhsPt;
                        }
                    }
                }
                if (commonType.isError()) {
                    String msg = "cannot compare between '" + reporter.white(lhsType.typename()) + "' and '" +
                                 reporter.white(rhsType.typename()) + "'";
                    reporter.error(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }
                node.lhs = convertTo(node.lhs, commonType);
                node.rhs = convertTo(node.rhs, commonType);
                yield BasicType.INT;
            }
        };
    }

    public record TypeCheckCompoundAssignmentResult(Type lhsTargetType, Type rhsTargetType, Type tmpType) { }

    public static TypeCheckCompoundAssignmentResult typeCheckCompoundAssignment(AssignmentNode node) {
        // 逻辑需要与 typeCheckBinaryExp 保持一致
        Type lhsType = node.lhs.expType;
        Type rhsType = node.rhs.expType;

        return switch (node.op.op) {
            case ASSIGN -> throw new IllegalArgumentException("Cannot handle simple assignment");
            case MULTIPLY_ASSIGN, DIVIDE_ASSIGN, ADD_ASSIGN, SUBTRACT_ASSIGN, MODULO_ASSIGN, BITWISE_AND_ASSIGN,
                 BITWISE_OR_ASSIGN, BITWISE_XOR_ASSIGN -> {
                BasicType commonType = Type.commonRealType((BasicType) lhsType, (BasicType) rhsType);
                yield new TypeCheckCompoundAssignmentResult(commonType, commonType, commonType);
            }
            case LEFT_SHIFT_ASSIGN, RIGHT_SHIFT_ASSIGN -> {
                if (lhsType.isCharacter()) {
                    lhsType = BasicType.INT;
                }
                if (rhsType.isCharacter()) {
                    rhsType = BasicType.INT;
                }
                yield new TypeCheckCompoundAssignmentResult(lhsType, rhsType, lhsType);
            }
        };
    }

    /**
     * 检查对象类型(非函数类型)是否符合要求
     *
     * @param t                     类型，非函数类型
     * @param id                    标识符，若该类型为某个标识符的声明类型
     * @param requireCompleteItself 是否要求自身类型完整，为否时，允许 {@code int[][6]} 这样的类型
     */
    private void checkObjectType(TypeNode t, @Nullable IdentifierNode id, boolean requireCompleteItself) {
        // 先检查子类型

        // 指针类型，检查其指向的类型，但不要求其完整
        if (t instanceof PointerTypeNode pt) {
            checkType(pt.referencedType, null, false);
        }

        // 数组类型
        if (t instanceof ArrayTypeNode at) {
            // 递归检查
            checkType(at.elementType, null, false);

            // 元素类型检查
            TypeNode elementType = at.elementType;
            // 不能是函数类型
            if (elementType instanceof FunctionTypeNode) {
                String msg = "declaration of " + (id == null ? "type name" : "'" + reporter.white(id.name) + "'") +
                             " as array of functions";
                reporter.error(id == null ? at.wholeLoc : id.wholeLoc, msg);
            }
            // 不能是不完整类型
            if (!elementType.getType().isComplete()) {
                String msg = "array type has incomplete element type '" + reporter.white(elementType.typename()) + "'";
                reporter.error(id == null ? at.wholeLoc : id.wholeLoc, msg);
            }

            // 维度检查
            // 检查是否其下标处有 cvr 限定符，这些限定符仅在函数形参中可出现
            if (at.containsConst()) {
                String msg = "static or type qualifiers in non-parameter array declarator";
                reporter.error(at.constLoc, msg);
            }
            // 检查其维度是否为整数常量，且要求为正数
            if (at.size != null) {
                reporter.unsuppressDiagnostics();
                at.size = checkExpressionAndDecay(at.size);
                Either<Constant, SourceLocation> evalResult = constantEvaluator.tryEvalIntegerConstant(at.size);
                if (evalResult.right().isPresent()) {
                    String msg = "size of array is not an integer constant";
                    reporter.error(evalResult.right().get(), msg);
                    at.size = null;
                } else {
                    long sizeValue = evalResult.orThrow().toLong().value();
                    if (sizeValue <= 0) {
                        String msg = "size of array is not a positive integer constant";
                        reporter.error(at.size.wholeLoc, msg);
                        at.size = null;
                    } else {
                        at.size = new ConstantNode(at.size.wholeLoc, new ConstantUnsignedLong(sizeValue));
                    }
                }
                reporter.clearSuppressDiagnostics();
            }
        }

        // 检查自身类型是否完整
        if (requireCompleteItself && !t.getType().isComplete()) {
            // 不完整类型
            String typename = t.typename();
            if (id == null) {
                String msg = "storage size of object isn't known; have type '" + reporter.white(typename) + "'";
                reporter.error(t.wholeLoc, msg);
            } else {
                // 可能被重命名，使用 location 获取
                String msg =
                    "storage size of '" + reporter.white(reporter.byLocation(id.wholeLoc)) +
                    "' isn't known; have type '" + reporter.white(typename) + "'";
                reporter.error(id.wholeLoc, msg);
            }
        }
    }

    private void checkType(TypeNode t, @Nullable IdentifierNode id, boolean requireCompleteItself) {
        if (t instanceof FunctionTypeNode ft) {
            checkFunctionType(ft, id, false);
        } else {
            checkObjectType(t, id, requireCompleteItself);
        }
    }


    @Override
    public Void visit(DeclarationNode node) {
        for (InitDeclaratorNode initDecl : node.initDeclarators) {
            if (initDecl.finalType instanceof FunctionTypeNode funcType) {
                // 函数声明
                visitFunctionDeclaration(initDecl.id, funcType, node.storageClass, false);

                if (initDecl.init != null) {
                    // 函数类型不能使用赋值初始化
                    String msg = "function '" + reporter.white(initDecl.id.name) +
                                 "' is initialized like a variable";
                    reporter.error(initDecl.init.getWholeLocation(), msg);
                    initDecl.init.accept(this);
                }
                return null;
            }

            // 变量声明
            // 注意要在内部对类型进行检查
            boolean isFileScope = functionContext == null;
            if (isFileScope) {
                visitFileScopeVariableDeclaration(node.storageClass, initDecl);
            } else {
                visitBlockScopeVariableDeclaration(node.storageClass, initDecl);
            }
        }
        return null;
    }

    private void panicWithPreviousRef(
        String msg, IdentifierNode id, SymbolTable.Entry previous, boolean defined) {
        reporter.error(id.wholeLoc, msg);
        msg = "previous " + (defined ? "definition" : "declaration") + " of '" +
              reporter.white(id.name) + "' with type '" + reporter.white(previous.type.typename()) +
              "'";
        reporter.note(previous.id.wholeLoc, msg);
    }

    public void visitFunctionDeclaration(
        IdentifierNode id, FunctionTypeNode funcType, @Nullable StorageClassSpecifierNode storageClass,
        boolean isDefinition) {

        // 类型检查
        checkFunctionType(funcType, id, isDefinition);
        Type type = funcType.getType();

        SymbolTable.Entry previous = symbolTable.get(id.name);
        if (previous == null) {
            // 第一次
            boolean global = storageClass == null || !storageClass.storageClass.equals(StorageClassSpecifier.STATIC);
            SymbolTable.Entry.IdentifierAttr attr = new SymbolTable.Entry.FuncAttr(isDefinition, global);
            symbolTable.put(id.name, new SymbolTable.Entry(id, funcType, type, attr));
            return;
        }

        // 如果已经声明/定义，检查类型是否匹配
        boolean alreadyDefined = previous.attr.isDefinition();
        if (!previous.type.isCompatible(type)) {
            // 类型不匹配
            panicConflictType(id, funcType, previous, alreadyDefined);
            return;
        } else {
            // 类型匹配，进行 merge
            previous.type = type.merge(previous.type);
        }
        // 类型匹配，一定是函数的属性
        SymbolTable.Entry.FuncAttr funcAttr = (SymbolTable.Entry.FuncAttr) previous.attr;

        if (alreadyDefined && isDefinition) {
            // 重定义函数
            String msg = "redefinition of '" + reporter.white(id.name) + "'";
            panicWithPreviousRef(msg, id, previous, true);
            return;
        }
        if (funcAttr.isGlobal() && storageClass != null &&
            storageClass.storageClass.equals(StorageClassSpecifier.STATIC)) {
            // 之前是全局的（External linkage），现在是静态的（Internal linkage），链接冲突
            String msg = "static declaration of '" + reporter.white(id.name) +
                         "' follows non-static declaration";
            panicWithPreviousRef(msg, id, previous, alreadyDefined);
            return;
        }
        // 链接不冲突，不需要修改 global
        if (!alreadyDefined) {
            // 先前未定义，更新声明/定义行，仅用于错误信息打印
            previous.id = id;
            previous.typeNode = funcType;
        }
        funcAttr.defined = alreadyDefined || isDefinition;
    }

    /**
     * 检查该函数类型本身，不考虑符号表中的最终类型
     * <p>
     * 若为函数定义，则于符号表定义其参数
     *
     * @param funcType     函数类型
     * @param id           标识符，若该类型为某个标识符的声明类型
     * @param isDefinition 是否为定义
     */
    private void checkFunctionType(FunctionTypeNode funcType, @Nullable IdentifierNode id, boolean isDefinition) {

        // 检查返回类型
        TypeNode retType = funcType.retType;
        if (retType instanceof FunctionTypeNode || retType instanceof ArrayTypeNode) {
            // 返回值类型不合法
            String typename = funcType.retType.typename();
            if (id == null) {
                String msg = "function has invalid return type '" + reporter.white(typename) + "'";
                reporter.error(funcType.retType.wholeLoc, msg);
            } else {
                String msg = "function '" + reporter.white(id.name) + "' has invalid return type '" +
                             reporter.white(typename) + "'";
                reporter.error(id.wholeLoc, msg);
            }
        } else if (isDefinition && !(retType instanceof VoidTypeNode) && !retType.getType().isComplete()) {
            // 若函数声明不是定义，则返回类型可以不完整
            // 返回值类型不完整
            String typename = funcType.retType.typename();
            if (id == null) {
                String msg = "function has incomplete return type '" + reporter.white(typename) + "'";
                reporter.error(funcType.retType.wholeLoc, msg);
            } else {
                String msg = "function '" + reporter.white(id.name) + "' has incomplete return type '" +
                             reporter.white(typename) + "'";
                reporter.error(id.wholeLoc, msg);
            }
        }

        // 递归检查返回类型
        checkType(funcType.retType, id, false);

        // 检查参数类型
        if (funcType.hasNoParameters()) {
            return;
        }
        for (int i = 0; i < funcType.paramTypes.size(); i++) {
            TypeNode paramType = funcType.paramTypes.get(i);
            IdentifierNode param = funcType.params.get(i);

            // 形参类型衰减
            if (paramType instanceof ArrayTypeNode at) {
                // 任何数组类型的形参都被调整到对应的指针类型，若数组声明符的方括号内有限定符，则它具有限定
                paramType = new PointerTypeNode(at.wholeLoc, at.elementType);
                if (at.containsConst()) {
                    paramType.constQualifier = new ConstQualifierNode(at.constLoc);
                    at.constLoc = null;
                }
                funcType.paramTypes.set(i, paramType);
                checkType(at, param, false);
            } else if (paramType instanceof FunctionTypeNode ft) {
                // 任何函数类型的形参都被调整到对应的指针类型
                paramType = new PointerTypeNode(ft.wholeLoc, ft);
                funcType.paramTypes.set(i, paramType);
                checkFunctionType(ft, param, false);
            } else {
                // 函数定义中，要求类型为完整类型
                if (paramType instanceof VoidTypeNode || isDefinition && !paramType.getType().isComplete()) {
                    // 不完整类型
                    String typename = paramType.typename();
                    if (param != null) {
                        // 可能被重命名，通过 location 获取
                        String msg =
                            "parameter '" + reporter.white(reporter.byLocation(param.wholeLoc)) +
                            "' has incomplete type '" + reporter.white(typename) + "'";
                        reporter.error(param.wholeLoc, msg);
                    } else {
                        String msg =
                            "unnamed parameter " + (i + 1) + " has incomplete type '" + reporter.white(typename) + "'";
                        reporter.error(paramType.getWholeLocation(), msg);
                    }
                }

                // 递归检查
                checkType(paramType, param, false);
            }
        }

        if (isDefinition) {
            defineFunctionParameters(funcType);
        }
    }

    /**
     * 将函数形参定义在作用域中
     *
     * @param funcTypeNode 函数类型节点
     */
    private void defineFunctionParameters(FunctionTypeNode funcTypeNode) {
        for (int i = 0; i < funcTypeNode.paramTypes.size(); i++) {
            IdentifierNode paramId = funcTypeNode.params.get(i);
            Type paramType = funcTypeNode.paramTypes.get(i).getType();
            symbolTable.put(
                paramId.name,
                new SymbolTable.Entry(paramId, funcTypeNode.paramTypes.get(i), paramType,
                                      SymbolTable.Entry.AutoAttr.INSTANCE));
        }
    }

    public void visitFileScopeVariableDeclaration(StorageClassSpecifierNode storageClass, InitDeclaratorNode initDecl) {
        IdentifierNode id = initDecl.id;
        TypeNode typeNode = initDecl.finalType;
        Type type;

        // 获取定义类型
        SymbolTable.Entry.StaticAttr.DefinitionType defType;
        if (initDecl.init == null) {
            // 有链接，非定义，可暂时要求不完整
            // 无初始化
            if (storageClass != null && storageClass.storageClass.equals(StorageClassSpecifier.EXTERN)) {
                // 来自其他编译单元，外部定义，未定义
                defType = SymbolTable.Entry.StaticAttr.NoDefinition.INSTANCE;
                // 可要求不完整，但 void 除外（非标准，特别实现）
                checkObjectType(typeNode, id, typeNode instanceof VoidTypeNode);
            } else {
                // 本编译单元内定义，试探性定义
                defType = SymbolTable.Entry.StaticAttr.Tentative.INSTANCE;
                // 若为试探性定义+内部链接，要求必须完整 (ISO C99 6.9.2.3)
                // 同样，void 除外（非标准，特别实现）
                boolean requireComplete =
                    typeNode instanceof VoidTypeNode ||
                    storageClass != null && storageClass.storageClass.equals(StorageClassSpecifier.STATIC);
                checkObjectType(typeNode, id, requireComplete);
            }
            type = typeNode.getType();
        } else {
            // 对于数组类型，可能需要通过其初始化器确定其第一维的大小
            if (typeNode instanceof ArrayTypeNode atn) {
                if (atn.size == null) {
                    reporter.suppressDiagnostics();
                    checkObjectType(atn, id, false);
                    reporter.clearSuppressDiagnostics();

                    // 更新类型节点的第一维大小
                    long size = initializerHelper.determineArraySize(atn.getType(), initDecl.init);
                    if (size != 0) {
                        atn.size = new ConstantNode(null, new ConstantLong(size));
                    }
                }
            }
            // 有初始化器，为定义，要求类型必须完整
            checkObjectType(typeNode, id, true);
            type = typeNode.getType();

            if (type.isComplete()) {
                InitializerNode fullInit = initializerHelper.normalize(type, initDecl.init);
                if (fullInit != null) {
                    initDecl.init = fullInit;
                    defType = new SymbolTable.Entry.StaticAttr.Defined(initializerHelper.toStaticInit(type, fullInit));
                } else {
                    // 初始化器错误，给一个 dummy 类型以继续后续检查
                    defType = SymbolTable.Entry.StaticAttr.NoDefinition.INSTANCE;
                }
            } else {
                // 类型不完整，给一个 dummy 类型以继续后续检查
                defType = SymbolTable.Entry.StaticAttr.NoDefinition.INSTANCE;
                initDecl.init.accept(this);
            }
        }

        boolean global = storageClass == null || !storageClass.storageClass.equals(StorageClassSpecifier.STATIC);

        SymbolTable.Entry previous = symbolTable.get(id.name);
        if (previous == null) {
            // 第一次
            SymbolTable.Entry.IdentifierAttr attr = new SymbolTable.Entry.StaticAttr(defType, global);
            symbolTable.put(id.name, new SymbolTable.Entry(id, typeNode, type, attr));
            return;
        }

        // 先前有声明/定义
        boolean alreadyDefined = previous.attr.isDefinition();
        if (!previous.type.isCompatible(type)) {
            // 类型不匹配
            panicConflictType(id, typeNode, previous, alreadyDefined);
            return;
        } else {
            // 类型匹配，进行 merge
            previous.type = type.merge(previous.type);
        }

        // 类型匹配，且在全局作用域，一定是全局变量
        SymbolTable.Entry.StaticAttr prevAttr = (SymbolTable.Entry.StaticAttr) previous.attr;

        if (storageClass != null && storageClass.storageClass.equals(StorageClassSpecifier.EXTERN)) {
            // 当前为 extern，链接属性跟随先前定义/声明的属性
            global = prevAttr.isGlobal();
        } else if (prevAttr.isGlobal() != global) {
            // 链接属性不同，冲突
            String msg;
            if (global) {
                // 当前 global（External Linkage），先前非 global（Internal Linkage）
                msg = "non-static declaration of '" + reporter.white(id.name) + "' follows static declaration";
            } else {
                // 当前非 global（Internal Linkage），先前 global（External Linkage）
                msg = "static declaration of '" + reporter.white(id.name) + "' follows non-static declaration";
            }
            panicWithPreviousRef(msg, id, previous, alreadyDefined);
            return;
        }

        if (prevAttr.defType instanceof SymbolTable.Entry.StaticAttr.Defined prevDef) {
            if (defType instanceof SymbolTable.Entry.StaticAttr.Defined) {
                // 定义了两次，且都有初始化，冲突
                String msg = "redefinition of '" + reporter.white(id.name) + "'";
                panicWithPreviousRef(msg, id, previous, true);
            } else {
                // 当前无定义，先前有初始化，使用先前的初始化信息
                defType = prevDef;
            }
        } else if (!(defType instanceof SymbolTable.Entry.StaticAttr.Defined) &&
                   prevAttr.defType instanceof SymbolTable.Entry.StaticAttr.Tentative) {
            // 当前无初始化（NoInitializer 或 Tentative），先前为 Tentative，则为 Tentative
            defType = SymbolTable.Entry.StaticAttr.Tentative.INSTANCE;
        }
        // 其他情况使用当前的初始化信息

        if (!alreadyDefined) {
            // 先前未定义，更新声明/定义行
            previous.id = id;
        }
        // 更新定义属性
        prevAttr.defType = defType;
        prevAttr.global = global;
    }

    public void visitBlockScopeVariableDeclaration(
        StorageClassSpecifierNode storageClass, InitDeclaratorNode initDecl) {
        IdentifierNode id = initDecl.id;
        TypeNode typeNode = initDecl.finalType;

        if (storageClass == null || !storageClass.storageClass.equals(StorageClassSpecifier.EXTERN)) {
            // 对于数组类型，可能需要通过其初始化器确定其第一维的大小
            if (typeNode instanceof ArrayTypeNode atn) {
                if (atn.size == null) {
                    reporter.suppressDiagnostics();
                    checkObjectType(atn, id, false);
                    reporter.clearSuppressDiagnostics();

                    // 更新类型节点的第一维大小
                    long size = initializerHelper.determineArraySize(atn.getType(), initDecl.init);
                    if (size != 0) {
                        atn.size = new ConstantNode(null, new ConstantLong(size));
                    }
                }
            }
            // 检查类型，无链接从而要求完整
            checkObjectType(typeNode, id, true);
        } else {
            // 有链接，可以暂时不完整
            // 但 void 除外（非标准，特别实现）
            checkObjectType(typeNode, id, typeNode instanceof VoidTypeNode);
        }
        Type type = typeNode.getType();

        if (storageClass == null) {
            // 无存储类说明符，不可能重复定义
            // 进行定义
            SymbolTable.Entry.AutoAttr attr = SymbolTable.Entry.AutoAttr.INSTANCE;
            symbolTable.put(id.name, new SymbolTable.Entry(id, typeNode, type, attr));

            if (initDecl.init == null) { return; }

            // 初始化器处理
            if (type.isComplete()) {
                InitializerNode newInit = initializerHelper.normalize(type, initDecl.init);
                if (newInit != null) {
                    initDecl.init = newInit;
                }
            } else {
                initDecl.init.accept(this);
            }
            return;
        }

        if (storageClass.storageClass.equals(StorageClassSpecifier.EXTERN)) {
            // 块作用域的 extern 声明不允许有初始化
            if (initDecl.init != null) {
                String msg =
                    "'" + reporter.white(id.name) + "' has both '" + reporter.white("extern") +
                    "' and initializer";
                reporter.error(initDecl.init.getWholeLocation(), msg);
                // 这里不 return，继续处理下面的检查与定义
            }
            SymbolTable.Entry previous = symbolTable.get(id.name);
            if (previous == null) {
                // 第一次
                SymbolTable.Entry.IdentifierAttr attr = new SymbolTable.Entry.StaticAttr(
                    SymbolTable.Entry.StaticAttr.NoDefinition.INSTANCE, true);
                symbolTable.put(id.name, new SymbolTable.Entry(id, typeNode, type, attr));
                return;
            }

            // 先前有声明/定义
            boolean alreadyDefined = previous.attr.isDefinition();
            if (!previous.type.isCompatible(type)) {
                // 类型不匹配
                panicConflictType(id, typeNode, previous, alreadyDefined);
            } else {
                // 类型匹配，进行 merge
                previous.type = type.merge(previous.type);
            }
            if (!alreadyDefined) {
                // 更新声明/定义行
                previous.id = id;
            }
            return;
        }

        // static
        SymbolTable.Entry.StaticAttr.DefinitionType initialValue = null;
        if (initDecl.init == null) {
            // 块作用域 static 无初始化器
            // 若未提供初始化式
            // 拥有静态及线程局域存储期的对象被空初始化
            initialValue = new SymbolTable.Entry.StaticAttr.Defined(InitializerHelper.zeroStaticInit(type));
        } else if (type.isComplete()) {
            InitializerNode fullInit = initializerHelper.normalize(type, initDecl.init);
            if (fullInit != null) {
                initDecl.init = fullInit;
                initialValue = new SymbolTable.Entry.StaticAttr.Defined(initializerHelper.toStaticInit(type, fullInit));
            }
        }

        // static 块作用域变量为 No Linkage，不可能重复定义（在 Identifier Resolution 中已检查）
        SymbolTable.Entry.IdentifierAttr attr = new SymbolTable.Entry.StaticAttr(initialValue, false);
        symbolTable.put(id.name, new SymbolTable.Entry(id, typeNode, type, attr));
    }

    private void panicConflictType(
        IdentifierNode id, TypeNode type, SymbolTable.Entry previous, boolean alreadyDefined) {
        String msg;
        if (previous.type.isFunction() != type instanceof FunctionTypeNode) {
            msg = "'" + reporter.white(id.name) + "' redeclared as different kind of symbol";
        } else {
            msg = "conflicting types for '" + reporter.white(id.name) + "'; have '" +
                  reporter.white(type.getType().typename()) + "'";
        }
        panicWithPreviousRef(msg, id, previous, alreadyDefined);
    }

    @Override
    public Void visit(ExpressionStatementNode node) {
        checkStatementLabel(node);
        checkExpression(node.exp);
        node.exp.expType = node.exp.expType.removeConst();
        return null;
    }

    @Override
    public Void visit(NullStatementNode node) {
        checkStatementLabel(node);
        return null;
    }

    @Override
    public Void visit(VariableNode node) {
        // 始终有定义
        SymbolTable.Entry entry = symbolTable.get(node.id.name);
        node.expType = entry.type;
        return null;
    }

    public boolean validConvertAsIfByAssignment(ExpressionNode rhs, Type lhsType) {
        // rhs 与 lhs 必须满足下列条件之一
        // lhs 与 rhs 拥有兼容的 struct 或 union 类型，或……
        // rhs 必须可隐式转换成 lhs，这表示
        // lhs 与 rhs 均拥有算术类型
        if (lhsType.isArithmetic() && rhs.expType.isArithmetic()) {
            return true;
        }
        if (lhsType instanceof PointerType lhsPtrType && rhs.expType instanceof PointerType rhsPtrType) {
            Type lhsPointee = lhsPtrType.referencedType();
            Type rhsPointee = rhsPtrType.referencedType();

            // lhs 与 rhs 均拥有指向兼容类型（忽略限定符）的指针类型
            // 一个指向对象或不完整类型（即非函数）的指针，另一个指向（可有限定的）void
            boolean cond1 = lhsPointee.removeQualifiers().isCompatible(rhsPointee.removeQualifiers());
            boolean cond2 = !lhsPointee.isFunction() && rhsPointee.isVoid();
            boolean cond3 = !rhsPointee.isFunction() && lhsPointee.isVoid();
            if (cond1 || cond2 || cond3) {
                // 左侧指向的类型至少拥有所有右侧指向类型的限定符
                int lhsQualifiers = lhsPointee.isConst() ? 1 : 0;
                int rhsQualifiers = rhsPointee.isConst() ? 1 : 0;
                if (lhsQualifiers >= rhsQualifiers) {
                    return true;
                }
            }
        }
        // lhs 是指针，而 rhs 是空指针常量
        if (lhsType.isPointer() && isNullPointerConstant(rhs)) {
            return true;
        }
        return false;
    }

    private boolean isLvalueExpression(ExpressionNode exp) {
        // 下列表达式是左值
        // 标识符，含具名函数形参，只要声明它们为指代对象（而非函数或枚举常量）
        if (exp instanceof VariableNode var && !(var.expType.isFunction())) {
            return true;
        }
        // 字符串字面量
        if (exp instanceof StringLiteralNode) {
            return true;
        }
        // 对指向对象指针运用间接使用（一元 *）运算符的结果
        if (exp instanceof DereferenceNode deref && deref.exp.expType instanceof PointerType pt
            && pt.referencedType().isObject()) {
            return true;
        }
        // 下标运算符的结果
        if (exp instanceof SubscriptNode) {
            return true;
        }
        return false;
    }

    private boolean isModifiableLvalueExpression(ExpressionNode exp) {
        // 一个可修改左值是任何完整的非数组类型的、非 const 限定的左值表达式
        if (!exp.expType.isComplete()) {
            return false;
        }
        if (exp.expType.isArray()) {
            return false;
        }
        return !exp.expType.isConst() && isLvalueExpression(exp);
    }

    @Override
    public Void visit(AssignmentNode node) {
        boolean error = false;

        // 检查左侧
        node.lhs = checkExpressionAndDecay(node.lhs);
        if (node.lhs.expType.isError()) {
            error = true;
        } else if (!isModifiableLvalueExpression(node.lhs)) {
            String msg = "modifiable lvalue required as left operand of assignment; has type '" +
                         reporter.white(node.lhs.expType.typename()) + "'";
            reporter.error(node.op.wholeLoc, msg);
            error = true;
        }

        // 检查右侧
        node.rhs = checkExpressionAndDecay(node.rhs);
        if (error || node.rhs.expType.isError()) {
            node.expType = ErrorType.INSTANCE;
            return null;
        }

        node.rhs.expType = node.rhs.expType.removeConst();

        // 二者都正常，继续检查
        if (node.op.op == AssignmentOperator.ASSIGN) {
            // 简单赋值
            if (!validConvertAsIfByAssignment(node.rhs, node.lhs.expType)) {
                String msg =
                    "incompatible types when assigning type '" + reporter.white(node.lhs.expType.typename()) +
                    "' using type '" + reporter.white(node.rhs.expType.typename()) + "'";
                reporter.error(node.rhs.wholeLoc, msg);
                node.expType = ErrorType.INSTANCE;
                return null;
            }

            node.rhs = convertTo(node.rhs, node.lhs.expType);
            node.expType = node.lhs.expType;
            return null;
        }

        // 复合赋值
        //  lhs, rhs - 拥有算术类型的表达式
        //  除非 op 是 += 或 -=，此情况允许接受指针类型并具有与 + 和 - 相同的限制

        // 表达式 lhs @= rhs 与 lhs = lhs @ (rhs) 完全相同，但只求值一次 lhs
        // 复用检查逻辑
        BinaryExpressionNode binaryExp =
            new BinaryExpressionNode(
                new BinaryOperatorNode(node.op.wholeLoc, node.op.op.toBinaryOperator()), node.lhs, node.rhs);
        typeCheckBinaryExp(binaryExp);
        if (binaryExp.expType.isError()) {
            node.expType = ErrorType.INSTANCE;
            return null;
        }

        // 检查是否能 cast
        if (!validConvertAsIfByAssignment(binaryExp, node.lhs.expType)) {
            String msg =
                "incompatible types when assigning type '" + reporter.white(node.lhs.expType.typename()) +
                "' using type '" + reporter.white(node.rhs.expType.typename()) + "'";
            reporter.error(node.op.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }

        // 推迟到生成 TAC 时再进行 cast
        node.expType = node.lhs.expType;
        return null;
    }

    @Override
    public Void visit(IncrementDecrementNode node) {
        node.operand = checkExpressionAndDecay(node.operand);

        if (node.operand.expType.isError()) {
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        if (!isModifiableLvalueExpression(node.operand)) {
            String msg = node.isIncrement ?
                "modifiable lvalue required as increment operand" :
                "modifiable lvalue required as decrement operand";
            msg += "; has type '" + reporter.white(node.operand.expType.typename()) + "'";
            reporter.error(node.operatorLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        // 前缀和后缀自增或自减的操作数表达式 必须为整数类型、实浮点数类型或指针类型的可修改左值
        if (!node.operand.expType.isArithmetic() && !node.operand.expType.isPointer()) {
            String msg = "operand of " + (node.isIncrement ? "increment" : "decrement") +
                         " operator must have arithmetic or pointer type; have '" +
                         reporter.white(node.operand.expType.typename()) + "'";
            reporter.error(node.operatorLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        if (node.operand.expType instanceof PointerType pt && !pt.referencedType().isObject()) {
            String msg = (node.isIncrement ? "increment" : "decrement") +
                         " of pointer to " + (pt.referencedType().isFunction() ? "a function" : "an incomplete") +
                         " type '" + reporter.white(pt.referencedType().typename()) + "'";
            reporter.error(node.operatorLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }

        node.expType = node.operand.expType;
        return null;
    }

    @Override
    public Void visit(IfStatementNode node) {
        checkStatementLabel(node);
        node.cond = checkExpressionAndDecay(node.cond);
        node.cond.expType = node.cond.expType.removeConst();
        if (!(node.cond.expType.isError()) && !node.cond.expType.isScalar()) {
            String msg = "condition of if statement must have scalar type; have '" +
                         reporter.white(node.cond.expType.typename()) + "'";
            reporter.error(node.cond.wholeLoc, msg);
        }
        node.thenStmt.accept(this);
        if (node.elseStmt != null) {
            node.elseStmt.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ConditionalExpressionNode node) {
        node.cond = checkExpressionAndDecay(node.cond);
        node.thenExp = checkExpressionAndDecay(node.thenExp);
        node.elseExp = checkExpressionAndDecay(node.elseExp);
        node.cond.expType = node.cond.expType.removeConst();
        node.thenExp.expType = node.thenExp.expType.removeConst();
        node.elseExp.expType = node.elseExp.expType.removeConst();

        Type thenExpType = node.thenExp.expType;
        Type elseExpType = node.elseExp.expType;

        // 条件 - 标量类型的表达式
        if (node.cond.expType.isError()) {
            node.expType = ErrorType.INSTANCE;
            return null;
        } else if (!node.cond.expType.isScalar()) {
            String msg = "condition of conditional operator must have scalar type; have '" +
                         reporter.white(node.cond.expType.typename()) + "'";
            reporter.error(node.cond.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }

        if (thenExpType.isError() || elseExpType.isError()) {
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        // 仅允许下列表达式为 表达式真 和 表达式假
        // 两个任何算术类型的表达式
        if (thenExpType.isArithmetic() && elseExpType.isArithmetic()) {
            // 若表达式拥有算术类型，则公共类型为一般算术转换后的类型
            BasicType commonType = Type.commonRealType((BasicType) thenExpType, (BasicType) elseExpType);
            node.thenExp = convertTo(node.thenExp, commonType);
            node.elseExp = convertTo(node.elseExp, commonType);
            node.expType = commonType;
            return null;
        }
        // 两个 void 类型的表达式
        if (thenExpType.isVoid() && elseExpType.isVoid()) {
            node.expType = VoidType.INSTANCE;
            return null;
        }
        if (thenExpType.isPointer() || elseExpType.isPointer()) {
            Type commonType = ErrorType.INSTANCE;

            // 若一个表达式为指针而另一个是空指针常量，则类型为该指针的类型
            if (isNullPointerConstant(node.thenExp)) {
                commonType = elseExpType;
            } else if (isNullPointerConstant(node.elseExp)) {
                commonType = thenExpType;
            } else if (thenExpType instanceof PointerType thenPt && elseExpType instanceof PointerType elsePt) {
                Type thenPointee = thenPt.referencedType();
                Type elsePointee = elsePt.referencedType();
                boolean mergedConst = thenPointee.isConst() || elsePointee.isConst();
                // 一个是指向对象或不完整类型（即非函数）的指针，一个是指向（可有限定的）void
                // 若一个表达式是指向 void 指针，则结果为指向具有合并的 cvr 限定符的 void 的指针
                if (!thenPointee.isFunction() && elsePointee.isVoid()) {
                    commonType = new PointerType(VoidType.getInstance(mergedConst));
                } else if (thenPointee.isVoid() && !elsePointee.isFunction()) {
                    commonType = new PointerType(VoidType.getInstance(mergedConst));
                } else if (thenPointee.removeQualifiers().isCompatible(elsePointee.removeQualifiers())) {
                    // 两个指针类型的表达式，指向兼容的类型，忽略 cvr 限定符
                    // 若两个表达式都是指针，则结果为指向合并两个被指向类型的 cvr 限定符的类型的指针
                    commonType = new PointerType(thenPointee.removeQualifiers().setConst(mergedConst));
                }
            }

            if (!(commonType.isError())) {
                node.thenExp = convertTo(node.thenExp, commonType);
                node.elseExp = convertTo(node.elseExp, commonType);
                node.expType = commonType;
                return null;
            }
        }

        // 其他情况非法
        String msg =
            "invalid operands to conditional operator; have '" + reporter.white(thenExpType.typename()) +
            "' and '" + reporter.white(elseExpType.typename()) + "'";
        reporter.error(SourceLocation.concat(node.thenExp.wholeLoc, node.elseExp.wholeLoc), msg);
        node.expType = ErrorType.INSTANCE;
        return null;
    }

    @Override
    public Void visit(GotoNode node) {
        checkStatementLabel(node);
        return null;
    }

    @Override
    public Void visit(CompoundStatementNode node) {
        checkStatementLabel(node);
        for (BlockItemNode blockItem : node.blockItems) {
            blockItem.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(BreakNode node) {
        checkStatementLabel(node);
        return null;
    }

    @Override
    public Void visit(ContinueNode node) {
        checkStatementLabel(node);
        return null;
    }

    @Override
    public Void visit(WhileLoopNode node) {
        checkStatementLabel(node);
        if (node.isDoWhile) {
            node.body.accept(this);
            node.cond = checkExpressionAndDecay(node.cond);
            node.cond.expType = node.cond.expType.removeConst();
        } else {
            node.cond = checkExpressionAndDecay(node.cond);
            node.cond.expType = node.cond.expType.removeConst();
            node.body.accept(this);
        }
        if (!(node.cond.expType.isError()) && !node.cond.expType.isScalar()) {
            String msg = "condition of " + (node.isDoWhile ? "'do-while'" : "'while'") +
                         " statement must have scalar type; have '" +
                         reporter.white(node.cond.expType.typename()) + "'";
            reporter.error(node.cond.wholeLoc, msg);
        }
        return null;
    }

    @Override
    public Void visit(ForLoopNode node) {
        checkStatementLabel(node);
        if (node.init != null) {
            if (node.init instanceof ForInitDeclarationNode forInitDecl) {
                DeclarationNode decl = forInitDecl.decl;
                for (InitDeclaratorNode initDecl : decl.initDeclarators) {
                    if (initDecl.finalType instanceof FunctionTypeNode) {
                        // for 初始化语句中不允许声明函数类型
                        String msg = "declaration of non-variable '" + reporter.white(initDecl.id.name) +
                                     "' in for loop initial declaration";
                        reporter.error(decl.wholeLoc, msg);
                    }
                    if (decl.storageClass != null) {
                        // for 初始化语句中不允许有存储类说明符
                        // 可能被重命名，通过 location 获取
                        String msg = "declaration of " + decl.storageClass.storageClass + " variable '" +
                                     reporter.white(reporter.byLocation(initDecl.id.wholeLoc)) +
                                     "' in for loop initial declaration";
                        reporter.error(initDecl.id.wholeLoc, msg);
                    }
                }
            }
            node.init.accept(this);
            if (node.init instanceof ForInitExpressionNode exp) {
                exp.exp.expType = exp.exp.expType.removeConst();
            }
        }
        if (node.cond != null) {
            node.cond = checkExpressionAndDecay(node.cond);
            node.cond.expType = node.cond.expType.removeConst();
            if (!(node.cond.expType.isError()) && !node.cond.expType.isScalar()) {
                String msg = "condition of for statement must have scalar type; have '" +
                             reporter.white(node.cond.expType.typename()) + "'";
                reporter.error(node.cond.wholeLoc, msg);
            }
        }
        if (node.step != null) {
            checkExpression(node.step);
            node.step.expType = node.step.expType.removeConst();
        }
        node.body.accept(this);
        return null;
    }

    @Override
    public Void visit(SwitchStatementNode node) {
        checkStatementLabel(node);
        node.exp = checkExpressionAndDecay(node.exp);
        if (!(node.exp.expType.isError()) && !node.exp.expType.isInteger()) {
            String msg = "condition of switch statement must have integer type; have '" +
                         reporter.white(node.exp.expType.typename()) + "'";
            reporter.error(node.exp.wholeLoc, msg);
        }
        node.exp.expType = node.exp.expType.removeConst();
        if (node.exp.expType.isCharacter()) {
            // 进行整数提升
            node.exp = convertTo(node.exp, BasicType.INT);
        }
        node.body.accept(this);
        return null;
    }

    @Override
    public Void visit(FunctionCallNode node) {
        node.func = checkExpressionAndDecay(node.func);
        if (node.func.expType.isError()) {
            node.expType = ErrorType.INSTANCE;
            // 检查参数
            node.args = node.args.stream().map(this::checkExpressionAndDecay).toList();
            return null;
        }

        if (!(node.func.expType instanceof PointerType pt && pt.referencedType() instanceof FunctionType funcType)) {
            // 不是函数类型
            String msg = "called object '" + reporter.white(reporter.byLocation(node.func.wholeLoc)) +
                         "' is not a function or function pointer; have type '" +
                         reporter.white(node.func.expType.typename()) + "'";
            reporter.error(node.func.wholeLoc, msg);
            if (node.func instanceof VariableNode variable) {
                SymbolTable.Entry entry = symbolTable.get(variable.id.name);
                msg = "declared here";
                reporter.note(entry.id.wholeLoc, msg);
            }
            node.expType = ErrorType.INSTANCE;
            // 检查参数
            node.args = node.args.stream().map(this::checkExpressionAndDecay).toList();
            return null;
        }

        // 检查调用是否合法
        if (node.args.size() > funcType.parameterCount()) {
            // 参数过多
            String msg = "too many arguments to function '" + reporter.white(node.func.wholeLoc) +
                         "' (" + funcType.parameterCount() + " expected)";
            reporter.error(node.func.wholeLoc, msg);
            if (node.func instanceof VariableNode variable) {
                SymbolTable.Entry entry = symbolTable.get(variable.id.name);
                msg = "declared here";
                reporter.note(entry.id.wholeLoc, msg);
            }
            node.expType = ErrorType.INSTANCE;
            // 检查参数
            node.args = node.args.stream().map(this::checkExpressionAndDecay).toList();
            return null;
        }
        if (node.args.size() < funcType.parameterCount()) {
            // 参数不足
            String msg = "too few arguments to function '" + reporter.white(node.func.wholeLoc) +
                         "' (" + funcType.parameterCount() + " expected)";
            reporter.error(node.func.wholeLoc, msg);
            if (node.func instanceof VariableNode variable) {
                SymbolTable.Entry entry = symbolTable.get(variable.id.name);
                msg = "declared here";
                reporter.note(entry.id.wholeLoc, msg);
            }
            node.expType = ErrorType.INSTANCE;
            // 检查参数
            node.args = node.args.stream().map(this::checkExpressionAndDecay).toList();
            return null;
        }

        // 形参数量必须等于实参数量（除非使用省略号形参）
        // 参数数量正确，检查类型
        boolean noError = true;
        for (int i = 0; i < node.args.size(); i++) {
            ExpressionNode arg = node.args.get(i);
            // 检查参数类型
            arg = checkExpressionAndDecay(arg);
            node.args.set(i, arg);
            if (arg.expType.isError()) {
                noError = false;
                continue;
            }
            arg.expType = arg.expType.removeConst();

            // 必须存在如同赋值的隐式转换，将对应实参的无限定类型转换为形参类型
            Type paramType = funcType.parameterTypes().get(i);

            if (!paramType.isComplete()) {
                // 形参类型不完整
                String msg =
                    "type of formal parameter " + (i + 1) + " is incomplete";
                reporter.error(arg.wholeLoc, msg);

                noError = false;
            } else if (!validConvertAsIfByAssignment(arg, paramType)) {
                // 参数类型不兼容
                noError = false;
                String msg =
                    "incompatible type for argument " + (i + 1) + " of '" + reporter.white(node.func.wholeLoc) + "'";
                reporter.error(arg.wholeLoc, msg);

                msg = "expected '" + reporter.white(paramType.typename()) +
                      "' but argument is of type '" + reporter.white(arg.expType.typename()) + "'";

                // 仅信息打印
                if (node.func instanceof VariableNode variable) {
                    SymbolTable.Entry entry = symbolTable.get(variable.id.name);
                    TypeNode paramTypeNode = ((FunctionTypeNode) entry.typeNode).paramTypes.get(i);
                    IdentifierNode idNode = ((FunctionTypeNode) entry.typeNode).params.get(i);
                    // 匿名参数中参数名可能为 null，特殊处理
                    SourceLocation loc = idNode == null ? paramTypeNode.getWholeLocation() :
                        SourceLocation.concat(paramTypeNode.getWholeLocation(), idNode.getWholeLocation());
                    reporter.note(loc, msg);
                } else {
                    reporter.note(msg);
                }
            } else {
                node.args.set(i, convertTo(arg, paramType));
            }
        }
        node.expType = noError ? funcType.returnType() : ErrorType.INSTANCE;
        return null;
    }

    @Override
    public Void visit(ConstantNode node) {
        Constant value = node.value;
        node.expType = value.getType();
        return null;
    }

    @Override
    public Void visit(CastExpressionNode node) {
        // 类型名 - void 类型或任何标量类型
        checkType(node.targetType, null, false);
        Type targetType = node.targetType.getType().removeQualifiers();

        // 若类型名是 void，则表达式为其副效应求值，并舍弃其返回值，与单独将表达式用作表达式语句时相同
        if (targetType.isVoid()) {
            node.exp = checkExpressionAndDecay(node.exp);
            node.expType = VoidType.INSTANCE;
            return null;
        }

        if (!targetType.isScalar()) {
            String msg = "cast to non-scalar type other than void is not allowed";
            reporter.error(node.targetType.getWholeLocation(), msg);
            node.expType = ErrorType.INSTANCE;
            node.exp = checkExpressionAndDecay(node.exp);
            return null;
        }

        // 表达式 - 任何标量类型表达式（除非 类型名是 void，此情况下它可以是任何表达式）
        node.exp = checkExpressionAndDecay(node.exp);
        if (node.exp.expType.isError()) {
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        node.exp.expType = node.exp.expType.removeConst();

        if (!node.exp.expType.isScalar()) {
            String msg = "cast from non-scalar '" + reporter.white(node.exp.expType.typename()) +
                         "' type to scalar type is not allowed";
            reporter.error(node.exp.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        // 否则，若类型名恰是表达式的类型，则不做任何事
        // 否则，转换表达式的值为由类型名所指名的类型，如下：
        // 允许每种如同赋值的隐式转换
        if (validConvertAsIfByAssignment(node.exp, targetType)) {
            node.expType = targetType;
            return null;
        }
        // 除了隐式转换之外，还允许下列转换规则
        // ...
        // 不允许不列于此的转换。特别是
        // 没有指针和浮点数类型间的转换
        boolean error = false;
        if (node.exp.expType.isPointer() && targetType.isDouble()) {
            error = true;
        }
        if (targetType.isPointer() && node.exp.expType.isDouble()) {
            error = true;
        }
        // 没有指向函数指针和指向对象指针（含 void*）间的转换
        if (targetType instanceof PointerType lpt && node.exp.expType instanceof PointerType rpt) {
            if (lpt.referencedType().isFunction() != rpt.referencedType().isFunction()) {
                error = true;
            }
        }

        if (error) {
            String msg = "invalid cast from type '" + reporter.white(node.exp.expType.typename()) + "' to '" +
                         reporter.white(targetType.typename()) + "'";
            reporter.error(node.exp.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
        } else {
            node.expType = targetType;
        }
        return null;
    }

    @Override
    public Void visit(AddressOfNode node) {
        checkExpression(node.exp);
        if (node.exp.expType.isError()) {
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        if (!node.exp.expType.isFunction() && !isLvalueExpression(node.exp)) {
            String msg = "lvalue or function designator required as address-of operand; has type '" +
                         reporter.white(node.exp.expType.typename()) + "'";
            reporter.error(node.operatorLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        node.expType = new PointerType(node.exp.expType);
        return null;
    }

    @Override
    public Void visit(DereferenceNode node) {
        node.exp = checkExpressionAndDecay(node.exp);
        if (node.exp.expType.isError()) {
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        node.exp.expType = node.exp.expType.removeConst();

        if (!(node.exp.expType instanceof PointerType pointerType)) {
            String msg = "operand of dereference must have pointer type; have '" +
                         reporter.white(node.exp.expType.typename()) + "'";
            reporter.error(node.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }

        // 拒绝对指向 void 的指针进行解引用，非标准，特别实现
        if (pointerType.referencedType().isVoid()) {
            reporter.error(node.wholeLoc, "dereference of pointer to void type");
            node.expType = ErrorType.INSTANCE;
            return null;
        }

        node.expType = pointerType.referencedType();
        return null;
    }

    @Override
    public Void visit(SubscriptNode node) {
        node.lhs = checkExpressionAndDecay(node.lhs);
        node.rhs = checkExpressionAndDecay(node.rhs);
        if (node.lhs.expType.isError() || node.rhs.expType.isError()) {
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        node.lhs.expType = node.lhs.expType.removeConst();
        node.rhs.expType = node.rhs.expType.removeConst();
        Type lhsType = node.lhs.expType;
        Type rhsType = node.rhs.expType;

        // 一个是指向完整对象的指针类型，另一个拥有整数类型
        PointerType ptr;
        if (lhsType instanceof PointerType lhsPt && rhsType.isInteger()) {
            node.rhs = convertTo(node.rhs, BasicType.LONG);
            ptr = lhsPt;
        } else if (lhsType.isInteger() && rhsType instanceof PointerType rhsPt) {
            node.lhs = convertTo(node.lhs, BasicType.LONG);
            ptr = rhsPt;
        } else {
            String msg = "operands to subscript should have pointer to object type and integer type (have '" +
                         reporter.white(lhsType.typename()) + "' and '" +
                         reporter.white(rhsType.typename()) + "')";
            reporter.error(node.operatorLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }

        Type referencedType = ptr.referencedType();
        if (!referencedType.isComplete()) {
            String msg = "invalid use of pointer to an incomplete type '" + reporter.white(referencedType.typename()) +
                         "'";
            reporter.error(node.operatorLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        if (referencedType.isFunction()) {
            String msg = "invalid use of pointer to function type '" + reporter.white(referencedType.typename()) +
                         "'";
            reporter.error(node.operatorLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }

        node.expType = referencedType;
        return null;
    }

    @Override
    public Void visit(StringLiteralNode node) {
        node.expType = new ArrayType(BasicType.CHAR, new ConstantUnsignedLong(node.literal.length + 1));
        return null;
    }

    @Override
    public Void visit(SizeOfNode node) {
        checkExpression(node.exp);
        Type t = node.exp.expType;
        if (t.isError()) {
            node.expType = ErrorType.INSTANCE;
            return null;
        } else if (!t.isComplete()) {
            String msg = "invalid application of '" + reporter.white("sizeof") +
                         "' to an incomplete type '" +
                         reporter.white(t.typename()) + "'";
            reporter.error(node.exp.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        } else if (t instanceof FunctionType) {
            String msg = "invalid application of '" + reporter.white("sizeof") +
                         "' to a function type '" +
                         reporter.white(t.typename()) + "'";
            reporter.error(node.exp.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        node.expType = BasicType.UNSIGNED_LONG;
        return null;
    }

    @Override
    public Void visit(SizeOfTypeNode node) {
        checkType(node.type, null, false);
        if (!node.type.getType().isComplete()) {
            String msg = "invalid application of '" + reporter.white("sizeof") +
                         "' to an incomplete type '" +
                         reporter.white(node.type.typename()) + "'";
            reporter.error(node.type.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        } else if (node.type instanceof FunctionTypeNode) {
            String msg = "invalid application of '" + reporter.white("sizeof") +
                         "' to a function type '" +
                         reporter.white(node.type.typename()) + "'";
            reporter.error(node.type.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        node.expType = BasicType.UNSIGNED_LONG;
        return null;
    }

    @Override
    public Void visit(CommaExpressionNode node) {
        node.lhs = checkExpressionAndDecay(node.lhs);
        node.rhs = checkExpressionAndDecay(node.rhs);
        node.expType = node.rhs.expType;
        return null;
    }
}
