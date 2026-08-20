package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.AssignmentOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.StorageClassSpecifier;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 进行类型检查等工作
 * <p>
 * 需要先进行 {@link IdentifierResolutionPass}
 */
public final class TypeCheckingPass extends SemanticAnalysePass implements AstVisitor<Void> {

    public TypeCheckingPass() {
        super(new ConsoleLogger());
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
        return null;
    }

    @Override
    public Void visit(FunctionDefinitionNode node) {

        if (!(node.funcType instanceof FunctionTypeNode funcType)) {
            // 不是函数类型
            error();
            String msg = "name declared in a function definition shall have a function type; have '" +
                         getLogger().white(node.funcType.getType().toString()) + "'";
            logErrorWithSourceLine(node.id.wholeLoc, msg);
        } else {
            visitFunctionDeclaration(node.id, funcType, node.storageClass, true);
            checkFunctionParameter(funcType, true);
        }
        // 检查函数体
        functionContext = node;
        visit(node.body);
        functionContext = null;
        return null;
    }

    @Override
    public Void visit(ReturnNode node) {
        node.exp.accept(this);
        if (node.exp.expType instanceof ErrorType) { return null; }

        if (functionContext.funcType instanceof FunctionTypeNode funcType) {
            // 若表达式的类型与函数的返回类型不同，则如同赋值给该函数返回类型的对象一般对其值进行转换
            Type retType = funcType.retType.getType();
            if (!validConvertAsIfByAssignment(node.exp, retType)) {
                error();
                String msg =
                    "incompatible types when returning type '" + getLogger().white(node.exp.expType.toString()) +
                    "' but '" + getLogger().white(retType.toString()) + "' was expected";
                logErrorWithSourceLine(node.exp.wholeLoc, msg);
                return null;
            }
            node.exp = convertTo(node.exp, retType);
        }
        return null;
    }

    private ExpressionNode convertTo(ExpressionNode exp, Type type) {
        if (exp.expType.equals(type)) {
            return exp;
        }
        if (exp.expType instanceof ErrorType || type instanceof ErrorType) {
            exp.expType = ErrorType.INSTANCE;
            return exp;
        }

        ExpressionNode ret;
        if (exp instanceof ConstantNode constExp) {
            ret = new ConstantNode(constExp.wholeLoc, constExp.value.castTo(type));
        } else {
            ret = new CastExpressionNode(exp.wholeLoc, TypeNode.fromType(type), exp);
        }
        ret.expType = type;
        return ret;
    }

    @Override
    public Void visit(UnaryExpressionNode node) {
        node.exp.accept(this);
        if (node.exp.expType instanceof ErrorType) {
            node.expType = ErrorType.INSTANCE;
            return null;
        }

        node.expType = switch (node.op.op) {
            case NEGATE -> {
                if (!node.exp.expType.isArithmetic()) {
                    error();
                    String msg = "operand of unary minus must have arithmetic type; have '" +
                                 getLogger().white(node.exp.expType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }
                yield node.exp.expType;
            }
            case COMPLEMENT -> {
                if (!node.exp.expType.isInteger()) {
                    error();
                    String msg = "operand of bitwise complement must have integer type; have '" +
                                 getLogger().white(node.exp.expType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }
                yield node.exp.expType;
            }
            case NOT -> {
                if (!node.exp.expType.isScalar()) {
                    error();
                    String msg = "operand of logical negation must have scalar type; have '" +
                                 getLogger().white(node.exp.expType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }
                yield BasicType.INT;
            }
        };
        return null;
    }

    @Override
    public Void visit(BinaryExpressionNode node) {
        node.lhs.accept(this);
        node.rhs.accept(this);
        typeCheckBinaryExp(node);
        return null;
    }

    private boolean isNullPointerConstant(ExpressionNode exp) {
        if (!(exp instanceof ConstantNode constExp)) {
            return false;
        }
        return constExp.value.isNullPointer();
    }

    private Type getCommonPointerType(ExpressionNode lhs, ExpressionNode rhs) {
        Type lhsType = lhs.expType;
        Type rhsType = rhs.expType;
        if (lhsType.isCompatible(rhsType)) {
            return lhsType;
        }
        if (isNullPointerConstant(lhs)) {
            return rhsType;
        }
        if (isNullPointerConstant(rhs)) {
            return lhsType;
        }
        return ErrorType.INSTANCE;
    }

    private void typeCheckBinaryExp(BinaryExpressionNode node) {
        final Type lhsType = node.lhs.expType;
        final Type rhsType = node.rhs.expType;

        if (lhsType instanceof ErrorType || rhsType instanceof ErrorType) {
            node.expType = ErrorType.INSTANCE;
            return;
        }

        node.expType = switch (node.op.op) {
            case MULTIPLY, DIVIDE, ADD, SUBTRACT -> {
                if (!lhsType.isArithmetic() || !rhsType.isArithmetic()) {
                    error();
                    String msg = "operands of binary operator " + node.op.op.getSymbol() +
                                 " must have arithmetic type; have '" +
                                 getLogger().white(lhsType.toString()) + "' and '" +
                                 getLogger().white(rhsType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }
                BasicType commonType = Type.commonRealType((BasicType) lhsType, (BasicType) rhsType);
                node.lhs = convertTo(node.lhs, commonType);
                node.rhs = convertTo(node.rhs, commonType);
                yield commonType;
            }
            case MODULO, BITWISE_AND, BITWISE_OR, BITWISE_XOR -> {
                if (!lhsType.isInteger() || !rhsType.isInteger()) {
                    error();
                    String msg = "operands of binary operator " + node.op.op.getSymbol() +
                                 " must have integer type; have '" +
                                 getLogger().white(lhsType.toString()) + "' and '" +
                                 getLogger().white(rhsType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }
                BasicType commonType = Type.commonRealType((BasicType) lhsType, (BasicType) rhsType);
                node.lhs = convertTo(node.lhs, commonType);
                node.rhs = convertTo(node.rhs, commonType);
                yield commonType;
            }
            case LEFT_SHIFT, RIGHT_SHIFT -> {
                if (!lhsType.isInteger() || !rhsType.isInteger()) {
                    error();
                    String msg = "operands of binary operator " + node.op.op.getSymbol() +
                                 " must have integer type; have '" +
                                 getLogger().white(lhsType.toString()) + "' and '" +
                                 getLogger().white(rhsType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }
                yield lhsType;
            }
            case LOGICAL_AND, LOGICAL_OR -> {
                if (!lhsType.isScalar() || !rhsType.isScalar()) {
                    error();
                    String msg = "operands of logical operator " + node.op.op.getSymbol() +
                                 " must have scalar type; have '" +
                                 getLogger().white(lhsType.toString()) + "' and '" +
                                 getLogger().white(rhsType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }
                yield BasicType.INT;
            }
            case LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL -> {
                if (!lhsType.isReal() || !rhsType.isReal()) {
                    error();
                    String msg = "operands of relational operator " + node.op.op.getSymbol() +
                                 " must have real type; have '" + getLogger().white(lhsType.toString()) +
                                 "' and '" + getLogger().white(rhsType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                }
                BasicType commonType = Type.commonRealType((BasicType) lhsType, (BasicType) rhsType);
                node.lhs = convertTo(node.lhs, commonType);
                node.rhs = convertTo(node.rhs, commonType);
                yield BasicType.INT;
            }
            case EQUAL, NOT_EQUAL -> {
                Type commonType = ErrorType.INSTANCE;
                if (lhsType.isArithmetic() && rhsType.isArithmetic()) {
                    commonType = Type.commonRealType((BasicType) lhsType, (BasicType) rhsType);
                } else if (lhsType instanceof PointerType || rhsType instanceof PointerType) {
                    commonType = getCommonPointerType(node.lhs, node.rhs);
                }
                if (commonType instanceof ErrorType) {
                    error();
                    String msg = "cannot compare between '" + getLogger().white(lhsType.toString()) + "' and '" +
                                 getLogger().white(rhsType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
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
        final Type lhsType = node.lhs.expType;
        final Type rhsType = node.rhs.expType;

        return switch (node.op.op) {
            case ASSIGN -> throw new IllegalArgumentException("Cannot handle simple assignment");
            case MULTIPLY_ASSIGN, DIVIDE_ASSIGN, ADD_ASSIGN, SUBTRACT_ASSIGN, MODULO_ASSIGN, BITWISE_AND_ASSIGN,
                 BITWISE_OR_ASSIGN, BITWISE_XOR_ASSIGN -> {
                BasicType commonType = Type.commonRealType((BasicType) lhsType, (BasicType) rhsType);
                yield new TypeCheckCompoundAssignmentResult(commonType, commonType, commonType);
            }
            case LEFT_SHIFT_ASSIGN, RIGHT_SHIFT_ASSIGN ->
                new TypeCheckCompoundAssignmentResult(lhsType, rhsType, lhsType);
        };
    }

    @Override
    public Void visit(DeclarationNode node) {
        if (!node.type.getType().isComplete()) {
            // 不完整类型
            error();
            String msg =
                "storage size of '" + getLogger().white(getSourceFile().getByLocation(node.id.wholeLoc)) +
                "' isn't known; have type '" + getLogger().white(node.type.getType().toString()) + "'";
            logErrorWithSourceLine(node.id.wholeLoc, msg);
            return null;
        }

        if (node.type instanceof FunctionTypeNode funcType) {
            // 函数声明
            visitFunctionDeclaration(node.id, funcType, node.storageClass, false);
            checkFunctionParameter(funcType, false);

            if (node.init != null) {
                // 函数类型不能使用赋值初始化
                error();
                String msg = "function '" + getLogger().white(node.id.name) + "' is initialized like a variable";
                logErrorWithSourceLine(node.init.wholeLoc, msg);
            }
        } else {
            // 变量声明
            boolean isFileScope = functionContext == null;
            if (isFileScope) {
                visitFileScopeVariableDeclaration(node);
            } else {
                visitBlockScopeVariableDeclaration(node);
            }
        }
        return null;
    }

    private void panicWithPreviousRef(
        String msg, IdentifierNode id, SymbolTable.Entry previous, boolean defined) {
        logErrorWithSourceLine(id.wholeLoc, msg);
        msg = "previous " + (defined ? "definition" : "declaration") + " of '" +
              getLogger().white(id.name) + "' with type '" + getLogger().white(previous.type.toString()) +
              "'";
        logNoteWithSourceLine(previous.id.wholeLoc, msg);
    }

    public void visitFunctionDeclaration(
        IdentifierNode id, FunctionTypeNode funcType, @Nullable StorageClassSpecifierNode storageClass,
        boolean isDefinition) {

        // 检查返回类型
        boolean validRetType = true;
        if (funcType.retType.getType().isVoid()) {
            validRetType = false;
        } else if (funcType.retType instanceof FunctionTypeNode) {
            validRetType = false;
        }
        if (!validRetType) {
            // 返回值类型不合法
            error();
            String msg = "function '" + getLogger().white(id.name) + "' has invalid return type '" +
                         getLogger().white(funcType.retType.getType().toString()) + "'";
            logErrorWithSourceLine(id.wholeLoc, msg);
        }

        SymbolTable.Entry previous = symbolTable.get(id.name);
        if (previous == null) {
            // 第一次
            boolean global = storageClass == null || !storageClass.storageClass.equals(StorageClassSpecifier.STATIC);
            SymbolTable.Entry.IdentifierAttr attr = new SymbolTable.Entry.FuncAttr(isDefinition, global);
            symbolTable.put(id.name, new SymbolTable.Entry(id, funcType, funcType.getType(), attr));
            return;
        }

        // 如果已经声明/定义，检查类型是否匹配
        boolean alreadyDefined = previous.attr.isDefinition();
        if (!previous.type.isCompatible(funcType.getType())) {
            // 类型不匹配
            panicConflictType(id, funcType, previous, alreadyDefined);
            return;
        }
        // 类型匹配，一定是函数的属性
        SymbolTable.Entry.FuncAttr funcAttr = (SymbolTable.Entry.FuncAttr) previous.attr;

        if (alreadyDefined && isDefinition) {
            // 重定义函数
            error();
            String msg = "redefinition of '" + getLogger().white(id.name) + "'";
            panicWithPreviousRef(msg, id, previous, true);
            return;
        }
        if (funcAttr.isGlobal() && storageClass != null &&
            storageClass.storageClass.equals(StorageClassSpecifier.STATIC)) {
            // 之前是全局的（External linkage），现在是静态的（Internal linkage），链接冲突
            error();
            String msg = "static declaration of '" + getLogger().white(id.name) +
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

    private void checkFunctionParameter(FunctionTypeNode funcType, boolean isDefinition) {
        // 检查参数类型
        if (funcType.hasNoParameters()) {
            return;
        }
        for (int i = 0; i < funcType.paramTypes.size(); i++) {
            TypeNode paramType = funcType.paramTypes.get(i);
            IdentifierNode param = funcType.params.get(i);
            if (!paramType.getType().isComplete()) {
                // 不完整类型
                error();
                if (param != null) {
                    String msg =
                        "parameter '" + getLogger().white(getSourceFile().getByLocation(param.wholeLoc)) +
                        "' has incomplete type '" +
                        getLogger().white(paramType.getType().toString()) + "'";
                    logErrorWithSourceLine(param.wholeLoc, msg);
                } else {
                    String msg =
                        "unnamed parameter " + (i + 1) + " has incomplete type '" +
                        getLogger().white(paramType.getType().toString()) + "'";
                    logErrorWithSourceLine(paramType.getWholeLocation(), msg);
                }
            }
            if (isDefinition) {
                assert param != null;
                symbolTable.put(
                    param.name,
                    new SymbolTable.Entry(param, paramType, paramType.getType(), SymbolTable.Entry.AutoAttr.INSTANCE));
            }
        }
    }

    /**
     * @param init 常量初始化器
     * @param t    被初始化的类型
     */
    private SymbolTable.Entry.StaticAttr.DefinitionType getInitialValueFromInitializer(ConstantNode init, Type t) {
        // 若提供了初始化式，对于
        // 标量类型初始化，见标量初始化
        if (t.isScalar()) {
            // 求值该表达式，而其值在如同赋值般转换到对象类型后，成为被初始化对象的初值
            if (!validConvertAsIfByAssignment(init, t)) {
                error();
                String msg = "incompatible types when initializing type '" +
                             getLogger().white(t.toString()) +
                             "' using type '" + getLogger().white(init.expType.toString()) + "'";
                logErrorWithSourceLine(init.wholeLoc, msg);
                return SymbolTable.Entry.StaticAttr.NoDefinition.INSTANCE;
            } else {
                return new SymbolTable.Entry.StaticAttr.Defined(init.value.castTo(t).toStaticInit());
            }
        } else {
            throw new IllegalStateException("unexpected static initializer: " + t);
        }
    }

    public void visitFileScopeVariableDeclaration(DeclarationNode decl) {
        IdentifierNode id = decl.id;
        TypeNode type = decl.type;
        StorageClassSpecifierNode storageClass = decl.storageClass;
        ExpressionNode init = decl.init;

        // 获取定义类型
        SymbolTable.Entry.StaticAttr.DefinitionType defType;
        if (init == null) {
            // 无初始化
            if (storageClass != null && storageClass.storageClass.equals(StorageClassSpecifier.EXTERN)) {
                // 来自其他编译单元，外部定义，未定义
                defType = SymbolTable.Entry.StaticAttr.NoDefinition.INSTANCE;
            } else {
                // 本编译单元内定义，试探性定义
                defType = SymbolTable.Entry.StaticAttr.Tentative.INSTANCE;
            }
        } else if (init instanceof ConstantNode constInit) {
            // 整数常量初始化
            Type t = type.getType();
            constInit.accept(this);
            defType = getInitialValueFromInitializer(constInit, t);
        } else {
            // 其他类型的初始化表达式不合法
            error();
            String msg = "initializer element is not constant";
            logErrorWithSourceLine(init.wholeLoc, msg);
            // 给一个 dummy 类型以继续后续检查
            defType = SymbolTable.Entry.StaticAttr.NoDefinition.INSTANCE;
        }

        boolean global = storageClass == null || !storageClass.storageClass.equals(StorageClassSpecifier.STATIC);

        SymbolTable.Entry previous = symbolTable.get(id.name);
        if (previous == null) {
            // 第一次
            SymbolTable.Entry.IdentifierAttr attr = new SymbolTable.Entry.StaticAttr(defType, global);
            symbolTable.put(id.name, new SymbolTable.Entry(id, type, type.getType(), attr));
            return;
        }

        // 先前有声明/定义
        boolean alreadyDefined = previous.attr.isDefinition();
        if (!previous.type.isCompatible(type.getType())) {
            // 类型不匹配
            panicConflictType(id, type, previous, alreadyDefined);
            return;
        }
        // 类型匹配，且在全局作用域，一定是全局变量
        SymbolTable.Entry.StaticAttr prevAttr = (SymbolTable.Entry.StaticAttr) previous.attr;

        if (storageClass != null && storageClass.storageClass.equals(StorageClassSpecifier.EXTERN)) {
            // 当前为 extern，链接属性跟随先前定义/声明的属性
            global = prevAttr.isGlobal();
        } else if (prevAttr.isGlobal() != global) {
            // 链接属性不同，冲突
            error();
            String msg;
            if (global) {
                // 当前 global（External Linkage），先前非 global（Internal Linkage）
                msg = "non-static declaration of '" + getLogger().white(id.name) + "' follows static declaration";
            } else {
                // 当前非 global（Internal Linkage），先前 global（External Linkage）
                msg = "static declaration of '" + getLogger().white(id.name) + "' follows non-static declaration";
            }
            panicWithPreviousRef(msg, id, previous, alreadyDefined);
            return;
        }

        if (prevAttr.defType instanceof SymbolTable.Entry.StaticAttr.Defined prevDef) {
            if (defType instanceof SymbolTable.Entry.StaticAttr.Defined) {
                // 定义了两次，且都有初始化，冲突
                error();
                String msg = "redefinition of '" + getLogger().white(id.name) + "'";
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

    public void visitBlockScopeVariableDeclaration(DeclarationNode decl) {
        IdentifierNode id = decl.id;
        TypeNode type = decl.type;
        StorageClassSpecifierNode storageClass = decl.storageClass;
        ExpressionNode init = decl.init;

        if (storageClass == null) {
            // 无存储类说明符，不可能重复定义
            SymbolTable.Entry.AutoAttr attr = SymbolTable.Entry.AutoAttr.INSTANCE;
            symbolTable.put(id.name, new SymbolTable.Entry(id, type, type.getType(), attr));

            if (init == null) { return; }
            init.accept(this);
            if (init.expType instanceof ErrorType) { return; }

            // 若提供了初始化式，对于
            // 标量类型初始化，见标量初始化
            if (type.getType().isScalar()) {
                // 求值该表达式，而其值在如同赋值般转换到对象类型后，成为被初始化对象的初值
                if (!validConvertAsIfByAssignment(init, type.getType())) {
                    error();
                    String msg = "incompatible types when initializing type '" +
                                 getLogger().white(type.getType().toString()) +
                                 "' using type '" + getLogger().white(init.expType.toString()) + "'";
                    logErrorWithSourceLine(init.wholeLoc, msg);
                } else {
                    decl.init = convertTo(init, type.getType());
                }
            }
            return;
        }

        if (storageClass.storageClass.equals(StorageClassSpecifier.EXTERN)) {
            // 块作用域的 extern 声明不允许有初始化
            if (init != null) {
                error();
                String msg =
                    "'" + getLogger().white(id.name) + "' has both '" + getLogger().white("extern") +
                    "' and initializer";
                logErrorWithSourceLine(init.wholeLoc, msg);
                // 这里不 return，继续处理下面的检查与定义
            }
            SymbolTable.Entry previous = symbolTable.get(id.name);
            if (previous == null) {
                // 第一次
                SymbolTable.Entry.IdentifierAttr attr = new SymbolTable.Entry.StaticAttr(
                    SymbolTable.Entry.StaticAttr.NoDefinition.INSTANCE, true);
                symbolTable.put(id.name, new SymbolTable.Entry(id, type, type.getType(), attr));
                return;
            }

            // 先前有声明/定义
            boolean alreadyDefined = previous.attr.isDefinition();
            if (!previous.type.isCompatible(type.getType())) {
                // 类型不匹配
                panicConflictType(id, type, previous, alreadyDefined);
            }
            if (!alreadyDefined) {
                // 更新声明/定义行
                previous.id = id;
            }
            return;
        }

        // static
        SymbolTable.Entry.StaticAttr.DefinitionType initialValue = null;
        Type t = type.getType();
        if (init == null) {
            // 块作用域 static 无初始化器
            // 若未提供初始化式
            // 拥有静态及线程局域存储期的对象被空初始化

            // 指针被初始化成其类型的空指针值
            if (t instanceof PointerType) {
                initialValue = SymbolTable.Entry.StaticAttr.Defined.UNSIGNED_LONG_ZERO;
            } else if (t instanceof BasicType bt) {
                initialValue = switch (bt) {
                    // 整数类型对象被初始化成无符号的零
                    case INT -> SymbolTable.Entry.StaticAttr.Defined.INT_ZERO;
                    case LONG -> SymbolTable.Entry.StaticAttr.Defined.LONG_ZERO;
                    case UNSIGNED_INT -> SymbolTable.Entry.StaticAttr.Defined.UNSIGNED_INT_ZERO;
                    case UNSIGNED_LONG -> SymbolTable.Entry.StaticAttr.Defined.UNSIGNED_LONG_ZERO;
                    // 浮点类型对象被初始化成正零
                    case DOUBLE -> SymbolTable.Entry.StaticAttr.Defined.DOUBLE_ZERO;
                    default -> throw new IllegalStateException("unexpected static initializer: " + t);
                };
            } else {
                throw new IllegalStateException("unexpected static initializer: " + t);
            }
        } else if (init instanceof ConstantNode constInit) {
            // 常量初始化
            constInit.accept(this);
            initialValue = getInitialValueFromInitializer(constInit, t);
        } else {
            // 其他类型的初始化表达式不合法
            error();
            String msg = "initializer element is not constant";
            logErrorWithSourceLine(init.wholeLoc, msg);
            // 这里不 return，继续处理下面的定义，防止后续引用无定义
        }
        // static 块作用域变量为 No Linkage，不可能重复定义（在 Identifier Resolution 中已检查）
        SymbolTable.Entry.IdentifierAttr attr = new SymbolTable.Entry.StaticAttr(initialValue, false);
        symbolTable.put(id.name, new SymbolTable.Entry(id, type, type.getType(), attr));
    }

    private void panicConflictType(
        IdentifierNode id, TypeNode type, SymbolTable.Entry previous, boolean alreadyDefined) {
        error();
        String msg;
        if ((previous.type instanceof FunctionType) != (type instanceof FunctionTypeNode)) {
            msg = "'" + getLogger().white(id.name) + "' redeclared as different kind of symbol";
        } else {
            msg = "conflicting types for '" + getLogger().white(id.name) + "'; have '" +
                  getLogger().white(type.getType().toString()) + "'";
        }
        panicWithPreviousRef(msg, id, previous, alreadyDefined);
    }

    @Override
    public Void visit(ExpressionStatementNode node) {
        node.exp.accept(this);
        return null;
    }

    @Override
    public Void visit(NullStatementNode node) {
        return null;
    }

    @Override
    public Void visit(VariableNode node) {
        // 始终有定义
        SymbolTable.Entry entry = symbolTable.get(node.id.name);
        Type t = entry.type;
        if (!t.isComplete()) {
            // 不完整类型不能使用
            error();
            String msg = "storage size of '" + getLogger().white(getSourceFile().getByLocation(node.wholeLoc)) +
                         "' isn't known; have type '" + getLogger().white(t.toString()) + "'";
            logErrorWithSourceLine(node.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }

        node.expType = t;
        return null;
    }

    private boolean validConvertAsIfByAssignment(ExpressionNode rhs, Type lhsType) {
        // rhs 与 lhs 必须满足下列条件之一
        // lhs 与 rhs 拥有兼容的 struct 或 union 类型，或……
        // rhs 必须可隐式转换成 lhs，这表示
        // lhs 与 rhs 均拥有算术类型
        if (lhsType.isArithmetic() && rhs.expType.isArithmetic()) {
            return true;
        }
        // lhs 与 rhs 均拥有指向兼容类型（忽略限定符）的指针类型
        if (lhsType instanceof PointerType lhsPtrType && rhs.expType instanceof PointerType rhsPtrType &&
            lhsPtrType.referencedType().isCompatible(rhsPtrType.referencedType())) {
            return true;
        }
        // lhs 是指针，而 rhs 是空指针常量
        if (lhsType instanceof PointerType && isNullPointerConstant(rhs)) {
            return true;
        }
        return false;
    }

    private boolean isLvalueExpression(ExpressionNode exp) {
        if (exp instanceof VariableNode var && !(var.expType instanceof FunctionType)) {
            return true;
        }
        if (exp instanceof DereferenceNode) {
            return true;
        }
        return false;
    }

    private boolean isModifiableLvalueExpression(ExpressionNode exp) {
        return isLvalueExpression(exp);
    }

    @Override
    public Void visit(AssignmentNode node) {
        node.lhs.accept(this);
        node.rhs.accept(this);

        if (node.lhs.expType instanceof ErrorType || node.rhs.expType instanceof ErrorType) {
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        if (!isModifiableLvalueExpression(node.lhs)) {
            error();
            String msg = "modifiable lvalue required as left operand of assignment";
            logErrorWithSourceLine(node.op.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }

        if (node.op.op == AssignmentOperator.ASSIGN) {
            // 简单赋值
            if (!validConvertAsIfByAssignment(node.rhs, node.lhs.expType)) {
                error();
                String msg =
                    "incompatible types when assigning type '" + getLogger().white(node.lhs.expType.toString()) +
                    "' using type '" + getLogger().white(node.rhs.expType.toString()) + "'";
                logErrorWithSourceLine(node.rhs.wholeLoc, msg);
                node.expType = ErrorType.INSTANCE;
                return null;
            }

            node.rhs = convertTo(node.rhs, node.lhs.expType);
            node.expType = node.lhs.expType;
            return null;
        }

        // 复合赋值
        // lhs, rhs	- 拥有算术类型的表达式
        if (!node.lhs.expType.isArithmetic() || !node.rhs.expType.isArithmetic()) {
            error();
            String msg = "operands of compound assignment operator " + node.op.op.getSymbol() +
                         " must have arithmetic type; have '" + getLogger().white(node.lhs.expType.toString()) +
                         "' and '" + getLogger().white(node.rhs.expType.toString()) + "'";
            logErrorWithSourceLine(node.op.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }

        // 表达式 lhs @= rhs 与 lhs = lhs @ (rhs) 完全相同，但只求值一次 lhs
        // 复用检查逻辑
        BinaryExpressionNode binaryExp =
            new BinaryExpressionNode(
                new BinaryOperatorNode(node.op.wholeLoc, node.op.op.toBinaryOperator()), node.lhs, node.rhs);
        typeCheckBinaryExp(binaryExp);
        // 推迟到生成 TAC 时再进行 cast
        // 目前始终能进行 cast
        node.expType = binaryExp.expType instanceof ErrorType ? ErrorType.INSTANCE : node.lhs.expType;
        return null;
    }

    @Override
    public Void visit(IncrementDecrementNode node) {
        node.operand.accept(this);
        if (node.operand.expType instanceof ErrorType) {
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        if (!isModifiableLvalueExpression(node.operand)) {
            error();
            String msg = node.isIncrement ?
                "modifiable lvalue required as increment operand" :
                "modifiable lvalue required as decrement operand";
            logErrorWithSourceLine(node.operatorLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        // 前缀和后缀自增或自减的操作数表达式 必须为整数类型、实浮点数类型或指针类型的可修改左值
        if (!node.operand.expType.isArithmetic()) {
            error();
            String msg = "operand of " + (node.isIncrement ? "increment" : "decrement") +
                         " operator must have arithmetic type; have '" +
                         getLogger().white(node.operand.expType.toString()) + "'";
            logErrorWithSourceLine(node.operatorLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }

        node.expType = node.operand.expType;
        return null;
    }

    @Override
    public Void visit(IfStatementNode node) {
        node.cond.accept(this);
        if (!(node.cond.expType instanceof ErrorType) && !node.cond.expType.isScalar()) {
            error();
            String msg = "condition of if statement must have scalar type; have '" +
                         getLogger().white(node.cond.expType.toString()) + "'";
            logErrorWithSourceLine(node.cond.wholeLoc, msg);
        }
        node.thenStmt.accept(this);
        if (node.elseStmt != null) {
            node.elseStmt.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ConditionalExpressionNode node) {
        node.cond.accept(this);
        node.thenExp.accept(this);
        node.elseExp.accept(this);

        Type thenExpType = node.thenExp.expType;
        Type elseExpType = node.elseExp.expType;

        // 条件 - 标量类型的表达式
        if (node.cond.expType instanceof ErrorType) {
            node.expType = ErrorType.INSTANCE;
        } else if (!node.cond.expType.isScalar()) {
            error();
            String msg = "condition of conditional operator must have scalar type; have '" +
                         getLogger().white(node.cond.expType.toString()) + "'";
            logErrorWithSourceLine(node.cond.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }

        // 仅允许下列表达式为 表达式真 和 表达式假
        // 两个任何算术类型的表达式
        if (thenExpType instanceof ErrorType || elseExpType instanceof ErrorType) {
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        if (thenExpType.isArithmetic() && elseExpType.isArithmetic()) {
            // 若表达式拥有算术类型，则公共类型为一般算术转换后的类型
            BasicType commonType = Type.commonRealType(
                (BasicType) thenExpType, (BasicType) elseExpType);
            node.thenExp = convertTo(node.thenExp, commonType);
            node.elseExp = convertTo(node.elseExp, commonType);
            node.expType = commonType;
            return null;
        }
        // 两个 void 类型的表达式
        if (thenExpType.isVoid() && elseExpType.isVoid()) {
            node.expType = BasicType.VOID;
            return null;
        }
        // 两个指针类型的表达式，指向兼容的类型，忽略 cvr 限定符
        // 一个表达式是指针而另一个是空指针常量
        if (thenExpType instanceof PointerType || elseExpType instanceof PointerType) {
            Type commonType = getCommonPointerType(node.thenExp, node.elseExp);
            if (!(commonType instanceof ErrorType)) {
                node.thenExp = convertTo(node.thenExp, commonType);
                node.elseExp = convertTo(node.elseExp, commonType);
                node.expType = commonType;
                return null;
            }
        }

        // 其他情况非法
        error();
        String msg =
            "invalid operands to conditional operator; have '" + getLogger().white(thenExpType.toString()) +
            "' and '" + getLogger().white(elseExpType.toString()) + "'";
        logErrorWithSourceLine(SourceLocation.concat(node.thenExp.wholeLoc, node.elseExp.wholeLoc), msg);
        node.expType = ErrorType.INSTANCE;
        return null;
    }

    @Override
    public Void visit(GotoNode node) {
        return null;
    }

    @Override
    public Void visit(CompoundStatementNode node) {
        for (BlockItemNode blockItem : node.blockItems) {
            blockItem.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(BreakNode node) {
        return null;
    }

    @Override
    public Void visit(ContinueNode node) {
        return null;
    }

    @Override
    public Void visit(WhileLoopNode node) {
        if (node.isDoWhile) {
            node.body.accept(this);
            node.cond.accept(this);
        } else {
            node.cond.accept(this);
            node.body.accept(this);
        }
        if (!(node.cond.expType instanceof ErrorType) && !node.cond.expType.isScalar()) {
            error();
            String msg = "condition of " + (node.isDoWhile ? "'do-while'" : "'while'") +
                         " statement must have scalar type; have '" +
                         getLogger().white(node.cond.expType.toString()) + "'";
            logErrorWithSourceLine(node.cond.wholeLoc, msg);
        }
        return null;
    }

    @Override
    public Void visit(ForLoopNode node) {
        if (node.init != null) {
            if (node.init instanceof ForInitDeclarationNode forInitDecl) {
                DeclarationNode decl = forInitDecl.decl;
                if (decl.type instanceof FunctionTypeNode) {
                    // for 初始化语句中不允许声明函数类型
                    error();
                    String msg = "declaration of non-variable '" + getLogger().white(decl.id.name) +
                                 "' in for loop initial declaration";
                    logErrorWithSourceLine(decl.wholeLoc, msg);
                }
                if (decl.storageClass != null) {
                    // for 初始化语句中不允许有存储类说明符
                    error();
                    String msg = "declaration of " + decl.storageClass.storageClass + " variable '" +
                                 getLogger().white(getSourceFile().getByLocation(decl.id.wholeLoc)) +
                                 "' in for loop initial declaration";
                    logErrorWithSourceLine(decl.id.wholeLoc, msg);
                }
            }
            node.init.accept(this);
        }
        if (node.cond != null) {
            node.cond.accept(this);
            if (!(node.cond.expType instanceof ErrorType) && !node.cond.expType.isScalar()) {
                error();
                String msg = "condition of for statement must have scalar type; have '" +
                             getLogger().white(node.cond.expType.toString()) + "'";
                logErrorWithSourceLine(node.cond.wholeLoc, msg);
            }
        }
        if (node.step != null) {
            node.step.accept(this);
        }
        node.body.accept(this);
        return null;
    }

    @Override
    public Void visit(SwitchStatementNode node) {
        node.exp.accept(this);
        if (!(node.exp.expType instanceof ErrorType) && !node.exp.expType.isInteger()) {
            error();
            String msg = "condition of switch statement must have integer type; have '" +
                         getLogger().white(node.exp.expType.toString()) + "'";
            logErrorWithSourceLine(node.exp.wholeLoc, msg);
        }
        node.body.accept(this);
        return null;
    }

    @Override
    public Void visit(FunctionCallNode node) {
        if (!(node.func instanceof VariableNode variable)) {
            // 目前不允许调用函数指针
            error();
            String msg = "function call expression shall have name as function designator";
            logErrorWithSourceLine(node.func.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            // 检查参数
            node.args.forEach(arg -> arg.accept(this));
            return null;
        }

        SymbolTable.Entry entry = symbolTable.get(variable.id.name);
        Type type = entry.type;
        if (!(type instanceof FunctionType funcType)) {
            // 不是函数类型
            error();
            String msg = "called object '" + getLogger().white(getSourceFile().getByLocation(variable.wholeLoc)) +
                         "' is not a function or function pointer; have type '" +
                         getLogger().white(type.toString()) + "'";
            logErrorWithSourceLine(variable.wholeLoc, msg);
            msg = "declared here";
            logNoteWithSourceLine(entry.id.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            // 检查参数
            node.args.forEach(arg -> arg.accept(this));
            return null;
        }

        // 检查调用是否合法
        if (node.args.size() > funcType.parameterCount()) {
            // 参数过多
            error();
            String msg = "too many arguments to function '" + getLogger().white(variable.id.name) + "'";
            logErrorWithSourceLine(node.func.wholeLoc, msg);
            msg = "declared here";
            logNoteWithSourceLine(entry.id.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            // 检查参数
            node.args.forEach(arg -> arg.accept(this));
            return null;
        }
        if (node.args.size() < funcType.parameterCount()) {
            // 参数不足
            error();
            String msg = "too few arguments to function '" + getLogger().white(variable.id.name) + "'";
            logErrorWithSourceLine(node.func.wholeLoc, msg);
            msg = "declared here";
            logNoteWithSourceLine(entry.id.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            // 检查参数
            node.args.forEach(arg -> arg.accept(this));
            return null;
        }

        // 形参数量必须等于实参数量（除非使用省略号形参）
        // 参数数量正确，检查类型
        boolean noError = true;
        for (int i = 0; i < node.args.size(); i++) {
            ExpressionNode arg = node.args.get(i);
            // 检查参数类型
            arg.accept(this);
            // 必须存在如同赋值的隐式转换，将对应实参的无限定类型转换为形参类型
            Type paramType = funcType.parameterTypes().get(i);
            if (!validConvertAsIfByAssignment(arg, paramType)) {
                // 参数类型不兼容
                error();
                noError = false;
                String msg =
                    "incompatible type for argument " + (i + 1) + " of '" + getLogger().white(variable.id.name) + "'";
                logErrorWithSourceLine(node.args.get(i).wholeLoc, msg);
                msg = "expected '" + getLogger().white(paramType.toString()) +
                      "' but argument is of type '" + getLogger().white(arg.expType.toString()) + "'";
                TypeNode paramTypeNode = ((FunctionTypeNode) entry.typeNode).paramTypes.get(i);
                IdentifierNode idNode = ((FunctionTypeNode) entry.typeNode).params.get(i);
                // 匿名参数中参数名可能为 null，特殊处理
                SourceLocation loc = idNode == null ? paramTypeNode.getWholeLocation() :
                    SourceLocation.concat(paramTypeNode.getWholeLocation(), idNode.getWholeLocation());
                logNoteWithSourceLine(loc, msg);
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
        node.exp.accept(this);
        // 类型名 - void 类型或任何标量类型
        // 表达式 - 任何标量类型表达式（除非 类型名是 void，此情况下它可以是任何表达式）
        Type targetType = node.targetType.getType();

        // 若类型名是 void，则表达式为其副效应求值，并舍弃其返回值，与单独将表达式用作表达式语句时相同
        if (targetType.isVoid()) {
            node.expType = BasicType.VOID;
            return null;
        }
        if (!targetType.isScalar()) {
            error();
            String msg = "cast to non-scalar type other than void is not allowed";
            logErrorWithSourceLine(node.targetType.getWholeLocation(), msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        if (node.exp.expType instanceof ErrorType) {
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        if (!node.exp.expType.isScalar()) {
            error();
            String msg = "cast from non-scalar '" + getLogger().white(node.exp.expType.toString()) +
                         "' type to scalar type is not allowed";
            logErrorWithSourceLine(node.exp.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        // 否则，若类型名恰是表达式的类型，则不做任何事
        // 否则，转换表达式的值为由类型名所指名的类型，如下：
        // 允许每种如同赋值的隐式转换
        if (validConvertAsIfByAssignment(node.exp, targetType)) {
            node.exp = convertTo(node.exp, targetType);
            node.expType = targetType;
            return null;
        }
        // 除了隐式转换之外，还允许下列转换规则
        // ...
        // 不允许不列于此的转换。特别是
        // 没有指针和浮点数类型间的转换
        boolean error = false;
        if (node.exp.expType instanceof PointerType && targetType instanceof BasicType bt && bt == BasicType.DOUBLE) {
            error = true;
        }
        if (targetType instanceof PointerType && node.exp.expType instanceof BasicType bt && bt == BasicType.DOUBLE) {
            error = true;
        }
        // 没有指向函数指针和指向对象指针（含 void*）间的转换
        if (targetType instanceof PointerType lpt && node.exp.expType instanceof PointerType rpt) {
            if (lpt.referencedType() instanceof FunctionType && !(rpt.referencedType() instanceof FunctionType)) {
                error = true;
            }
            if (rpt.referencedType() instanceof FunctionType && !(lpt.referencedType() instanceof FunctionType)) {
                error = true;
            }
        }

        if (error) {
            error();
            String msg = "invalid cast from type '" + getLogger().white(node.exp.expType.toString()) + "' to '" +
                         getLogger().white(targetType.toString()) + "'";
            logErrorWithSourceLine(node.exp.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
        } else {
            node.exp = convertTo(node.exp, targetType);
            node.expType = targetType;
        }
        return null;
    }

    @Override
    public Void visit(AddressOfNode node) {
        node.exp.accept(this);
        if (node.exp.expType instanceof ErrorType) {
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        if (!isLvalueExpression(node.exp)) {
            error();
            String msg = "lvalue required as address-of operand";
            logErrorWithSourceLine(node.operatorLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        node.expType = new PointerType(node.exp.expType);
        return null;
    }

    @Override
    public Void visit(DereferenceNode node) {
        node.exp.accept(this);
        if (node.exp.expType instanceof ErrorType) {
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        if (!(node.exp.expType instanceof PointerType pointerType)) {
            error();
            String msg = "operand of dereference must have pointer type; have '" +
                         getLogger().white(node.exp.expType.toString()) + "'";
            logErrorWithSourceLine(node.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }
        node.expType = pointerType.referencedType();
        return null;
    }
}
