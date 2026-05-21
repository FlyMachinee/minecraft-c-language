package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.AssignmentOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.StorageClassSpecifier;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.ErrorType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.FunctionType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
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

            // 检查参数类型
            // 到了这里，要么所有参数都具名且不重复，要么只有单独的void参数
            for (int i = 0; i < funcType.params.size(); i++) {
                IdentifierNode id = funcType.params.get(i);
                TypeNode type = funcType.paramTypes.get(i);
                if (id != null) {
                    if (!type.getType().isComplete()) {
                        // 不完整类型
                        error();
                        String msg =
                            "parameter '" + getLogger().white(getSourceFile().getByLocation(id.wholeLoc)) +
                            "' has incomplete type '" +
                            getLogger().white(type.getType().toString()) + "'";
                        logErrorWithSourceLine(id.wholeLoc, msg);
                    }
                    symbolTable.put(
                        id.id,
                        new SymbolTable.Entry(id, type, type.getType(), SymbolTable.Entry.LocalAttr.INSTANCE));
                }
            }
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
        if (functionContext.funcType instanceof FunctionTypeNode funcType) {
            // 若表达式的类型与函数的返回类型不同，则如同赋值给该函数返回类型的对象一般对其值进行转换
            Type convertedType = getConvertTypeAsIfByAssignment(node.exp, funcType.retType.getType());
            if (convertedType instanceof ErrorType) {
                error();
                String msg =
                    "incompatible types when returning type '" + getLogger().white(node.exp.expType.toString()) +
                    "' but '" + getLogger().white(funcType.retType.getType().toString()) + "' was expected";
                logErrorWithSourceLine(node.exp.wholeLoc, msg);
            } else {
                node.exp = convertTo(node.exp, convertedType);
            }
        }
        return null;
    }

    private ExpressionNode convertTo(ExpressionNode exp, Type type) {
        if (exp.expType.equals(type) || exp.expType instanceof ErrorType || type instanceof ErrorType) {
            return exp;
        } else {
            if (!(type instanceof BasicType basicType)) {
                throw new IllegalStateException("unexpected non-basic type: " + exp.expType);
            } else {
                ExpressionNode newExp =
                    new CastExpressionNode(exp.wholeLoc, new BasicTypeNode(exp.wholeLoc, basicType), exp);
                newExp.expType = basicType;
                return newExp;
            }
        }
    }

    @Override
    public Void visit(UnaryExpressionNode node) {
        node.exp.accept(this);
        node.expType = switch (node.op.op) {
            case NEGATE -> {
                if (!node.exp.expType.isArithmetic()) {
                    error();
                    String msg = "operand of unary minus must have arithmetic type; have '" +
                                 getLogger().white(node.exp.expType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                } else {
                    yield node.exp.expType;
                }
            }
            case COMPLEMENT -> {
                if (!node.exp.expType.isInteger()) {
                    error();
                    String msg = "operand of bitwise complement must have integer type; have '" +
                                 getLogger().white(node.exp.expType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                } else {
                    yield node.exp.expType;
                }
            }
            case NOT -> {
                if (!node.exp.expType.isScalar()) {
                    error();
                    String msg = "operand of logical negation must have scalar type; have '" +
                                 getLogger().white(node.exp.expType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                } else {
                    yield BasicType.INT;
                }
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

    private void typeCheckBinaryExp(BinaryExpressionNode node) {
        node.expType = switch (node.op.op) {
            case MULTIPLY, DIVIDE, ADD, SUBTRACT -> {
                if (!node.lhs.expType.isArithmetic() || !node.rhs.expType.isArithmetic()) {
                    error();
                    String msg = "operands of binary operator " + node.op.op.getSymbol() +
                                 " must have arithmetic type; have '" +
                                 getLogger().white(node.lhs.expType.toString()) + "' and '" +
                                 getLogger().white(node.rhs.expType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                } else {
                    Type commonType = Type.commonRealType(
                        (BasicType) node.lhs.expType, (BasicType) node.rhs.expType);
                    node.lhs = convertTo(node.lhs, commonType);
                    node.rhs = convertTo(node.rhs, commonType);
                    yield commonType;
                }
            }
            case MODULO, BITWISE_AND, BITWISE_OR, BITWISE_XOR -> {
                if (!node.lhs.expType.isInteger() || !node.rhs.expType.isInteger()) {
                    error();
                    String msg = "operands of binary operator " + node.op.op.getSymbol() +
                                 " must have integer type; have '" +
                                 getLogger().white(node.lhs.expType.toString()) + "' and '" +
                                 getLogger().white(node.rhs.expType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                } else {
                    Type commonType = Type.commonRealType((BasicType) node.lhs.expType, (BasicType) node.rhs.expType);
                    node.lhs = convertTo(node.lhs, commonType);
                    node.rhs = convertTo(node.rhs, commonType);
                    yield commonType;
                }
            }
            case LEFT_SHIFT, RIGHT_SHIFT -> {
                if (!node.lhs.expType.isInteger() || !node.rhs.expType.isInteger()) {
                    error();
                    String msg = "operands of binary operator " + node.op.op.getSymbol() +
                                 " must have integer type; have '" +
                                 getLogger().white(node.lhs.expType.toString()) + "' and '" +
                                 getLogger().white(node.rhs.expType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                } else {
                    yield node.lhs.expType;
                }
            }
            case LOGICAL_AND, LOGICAL_OR -> {
                if (!node.lhs.expType.isScalar() || !node.rhs.expType.isScalar()) {
                    error();
                    String msg = "operands of logical operator " + node.op.op.getSymbol() +
                                 " must have scalar type; have '" +
                                 getLogger().white(node.lhs.expType.toString()) + "' and '" +
                                 getLogger().white(node.rhs.expType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                } else {
                    yield BasicType.INT;
                }
            }
            case LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL -> {
                if (!node.lhs.expType.isReal() || !node.rhs.expType.isReal()) {
                    error();
                    String msg = "operands of relational operator " + node.op.op.getSymbol() +
                                 " must have real type; have '" + getLogger().white(node.lhs.expType.toString()) +
                                 "' and '" + getLogger().white(node.rhs.expType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                } else {
                    Type commonType = Type.commonRealType((BasicType) node.lhs.expType, (BasicType) node.rhs.expType);
                    node.lhs = convertTo(node.lhs, commonType);
                    node.rhs = convertTo(node.rhs, commonType);
                    yield BasicType.INT;
                }
            }
            case EQUAL, NOT_EQUAL -> {
                if (!node.lhs.expType.isArithmetic() || !node.rhs.expType.isArithmetic()) {
                    error();
                    String msg = "operands of equality operator " + node.op.op.getSymbol() +
                                 " must have arithmetic type; have '" +
                                 getLogger().white(node.lhs.expType.toString()) +
                                 "' and '" + getLogger().white(node.rhs.expType.toString()) + "'";
                    logErrorWithSourceLine(node.op.wholeLoc, msg);
                    yield ErrorType.INSTANCE;
                } else {
                    Type commonType = Type.commonRealType((BasicType) node.lhs.expType, (BasicType) node.rhs.expType);
                    node.lhs = convertTo(node.lhs, commonType);
                    node.rhs = convertTo(node.rhs, commonType);
                    yield BasicType.INT;
                }
            }
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
        } else if (node.type instanceof FunctionTypeNode funcType) {
            // 函数声明
            visitFunctionDeclaration(node.id, funcType, node.storageClass, false);

            if (node.init != null) {
                // 函数类型不能使用赋值初始化
                error();
                String msg = "function '" + getLogger().white(node.id.id) + "' is initialized like a variable";
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
              getLogger().white(id.id) + "' with type '" + getLogger().white(previous.type.toString()) +
              "'";
        logNoteWithSourceLine(previous.id.wholeLoc, msg);
    }

    public void visitFunctionDeclaration(
        IdentifierNode id, FunctionTypeNode funcType, @Nullable StorageClassSpecifierNode storageClass,
        boolean isDefinition) {

        // 检查返回类型
        // 目前只能是 int 或 long
        if (!(funcType.retType instanceof BasicTypeNode returnType) ||
            returnType.getType() == BasicType.VOID) {
            // 返回值类型不合法
            error();
            String msg = "function '" + getLogger().white(id.id) + "' has invalid return type '" +
                         getLogger().white(funcType.retType.getType().toString()) + "'";
            logErrorWithSourceLine(id.wholeLoc, msg);
        }

        // 如果已经声明/定义，检查类型是否匹配
        SymbolTable.Entry previous = symbolTable.get(id.id);
        if (previous != null) {
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
                String msg = "redefinition of '" + getLogger().white(id.id) + "'";
                panicWithPreviousRef(msg, id, previous, true);
                return;
            }
            if (funcAttr.isGlobal() && storageClass != null &&
                storageClass.storageClass.equals(StorageClassSpecifier.STATIC)) {
                // 之前是全局的（External linkage），现在是静态的（Internal linkage），链接冲突
                error();
                String msg = "static declaration of '" + getLogger().white(id.id) + "' follows non-static declaration";
                panicWithPreviousRef(msg, id, previous, alreadyDefined);
                return;
            }
            // 链接不冲突，不需要修改 global
            if (!alreadyDefined) {
                // 先前未定义，更新声明/定义行
                previous.id = id;
            }
            funcAttr.defined = alreadyDefined || isDefinition;
        } else {
            // 第一次
            boolean global = storageClass == null || !storageClass.storageClass.equals(StorageClassSpecifier.STATIC);
            SymbolTable.Entry.IdentifierAttr attr = new SymbolTable.Entry.FuncAttr(isDefinition, global);
            symbolTable.put(id.id, new SymbolTable.Entry(id, funcType, funcType.getType(), attr));
        }
    }

    /**
     * @param init 常量初始化器
     * @param t    被初始化的类型
     */
    private SymbolTable.Entry.StaticAttr.InitialValue getInitialValueFromInitializer(ConstantNode init, Type t) {
        // 若提供了初始化式，对于
        // 标量类型初始化，见标量初始化
        if (t.isScalar()) {
            // 求值该表达式，而其值在如同赋值般转换到对象类型后，成为被初始化对象的初值
            if (t.isArithmetic()) {
                BasicType bt = (BasicType) t;
                return new SymbolTable.Entry.StaticAttr.Initial(init.value.castTo(bt).toStaticInit());
            } else {
                throw new IllegalStateException("unexpected static initializer: " + t);
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

        // 获取初始化类型
        SymbolTable.Entry.StaticAttr.InitialValue initialValue;
        if (init == null) {
            // 无初始化
            if (storageClass != null && storageClass.storageClass.equals(StorageClassSpecifier.EXTERN)) {
                // 来自其他编译单元，外部定义，未定义
                initialValue = SymbolTable.Entry.StaticAttr.NoInitializer.INSTANCE;
            } else {
                // 本编译单元内定义，试探性定义
                initialValue = SymbolTable.Entry.StaticAttr.Tentative.INSTANCE;
            }
        } else if (init instanceof ConstantNode constInit) {
            // 整数常量初始化
            Type t = type.getType();
            initialValue = getInitialValueFromInitializer(constInit, t);
        } else {
            // 其他类型的初始化表达式不合法
            error();
            String msg = "initializer element is not constant";
            logErrorWithSourceLine(init.wholeLoc, msg);
            // 给一个 dummy 类型以继续后续检查
            initialValue = SymbolTable.Entry.StaticAttr.NoInitializer.INSTANCE;
        }

        boolean global = storageClass == null || !storageClass.storageClass.equals(StorageClassSpecifier.STATIC);

        SymbolTable.Entry previous = symbolTable.get(id.id);
        if (previous != null) {
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
                    msg = "non-static declaration of '" + getLogger().white(id.id) + "' follows static declaration";
                } else {
                    // 当前非 global（Internal Linkage），先前 global（External Linkage）
                    msg = "static declaration of '" + getLogger().white(id.id) + "' follows non-static declaration";
                }
                panicWithPreviousRef(msg, id, previous, alreadyDefined);
                return;
            }

            if (prevAttr.initialValue instanceof SymbolTable.Entry.StaticAttr.Initial prevInit) {
                if (initialValue instanceof SymbolTable.Entry.StaticAttr.Initial) {
                    // 定义了两次，且都有初始化，冲突
                    error();
                    String msg = "redefinition of '" + getLogger().white(id.id) + "'";
                    panicWithPreviousRef(msg, id, previous, true);
                } else {
                    // 当前无定义，先前有初始化，使用先前的初始化信息
                    initialValue = prevInit;
                }
            } else if (!(initialValue instanceof SymbolTable.Entry.StaticAttr.Initial) &&
                       prevAttr.initialValue instanceof SymbolTable.Entry.StaticAttr.Tentative) {
                // 当前无初始化（NoInitializer 或 Tentative），先前为 Tentative，则为 Tentative
                initialValue = SymbolTable.Entry.StaticAttr.Tentative.INSTANCE;
            }
            // 其他情况使用当前的初始化信息

            if (!alreadyDefined) {
                // 先前未定义，更新声明/定义行
                previous.id = id;
            }
            // 更新定义属性
            prevAttr.initialValue = initialValue;
            prevAttr.global = global;
        } else {
            // 第一次
            SymbolTable.Entry.IdentifierAttr attr = new SymbolTable.Entry.StaticAttr(initialValue, global);
            symbolTable.put(id.id, new SymbolTable.Entry(id, type, type.getType(), attr));
        }
    }

    public void visitBlockScopeVariableDeclaration(DeclarationNode decl) {
        IdentifierNode id = decl.id;
        TypeNode type = decl.type;
        StorageClassSpecifierNode storageClass = decl.storageClass;
        ExpressionNode init = decl.init;

        if (storageClass == null) {
            // 无存储类说明符，不可能重复定义
            SymbolTable.Entry.LocalAttr attr = SymbolTable.Entry.LocalAttr.INSTANCE;
            symbolTable.put(id.id, new SymbolTable.Entry(id, type, type.getType(), attr));
            if (init != null) {
                init.accept(this);
                // 若提供了初始化式，对于
                // 标量类型初始化，见标量初始化
                if (type.getType().isScalar()) {
                    // 求值该表达式，而其值在如同赋值般转换到对象类型后，成为被初始化对象的初值
                    Type convertedType = getConvertTypeAsIfByAssignment(init, type.getType());
                    if (convertedType instanceof ErrorType) {
                        error();
                        String msg = "incompatible types when initializing type '" +
                                     getLogger().white(type.getType().toString()) +
                                     "' using type '" + getLogger().white(init.expType.toString()) + "'";
                        logErrorWithSourceLine(init.wholeLoc, msg);
                    } else {
                        decl.init = convertTo(init, convertedType);
                    }
                }
            }
        } else if (storageClass.storageClass.equals(StorageClassSpecifier.EXTERN)) {
            // 块作用域的 extern 声明不允许有初始化
            if (init != null) {
                error();
                String msg =
                    "'" + getLogger().white(id.id) + "' has both '" + getLogger().white("extern") + "' and initializer";
                logErrorWithSourceLine(init.wholeLoc, msg);
                // 这里不 return，继续处理下面的检查与定义
            }
            SymbolTable.Entry previous = symbolTable.get(id.id);
            if (previous != null) {
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
            } else {
                // 第一次
                SymbolTable.Entry.IdentifierAttr attr = new SymbolTable.Entry.StaticAttr(
                    SymbolTable.Entry.StaticAttr.NoInitializer.INSTANCE, true);
                symbolTable.put(id.id, new SymbolTable.Entry(id, type, type.getType(), attr));
            }
        } else {
            // static
            SymbolTable.Entry.StaticAttr.InitialValue initialValue = null;
            Type t = type.getType();
            if (init == null) {
                // 块作用域 static 无初始化器
                // 若未提供初始化式
                // 拥有静态及线程局域存储期的对象被空初始化
                if (t.isInteger()) {
                    // 整数类型对象被初始化成无符号的零
                    if (t instanceof BasicType bt) {
                        if (bt == BasicType.INT) {
                            initialValue = SymbolTable.Entry.StaticAttr.Initial.INT_ZERO;
                        } else if (bt == BasicType.LONG) {
                            initialValue = SymbolTable.Entry.StaticAttr.Initial.LONG_ZERO;
                        } else {
                            throw new IllegalStateException("unexpected integer type: " + t);
                        }
                    } else {
                        throw new IllegalStateException("unexpected static initializer: " + t);
                    }
                }
            } else if (init instanceof ConstantNode constInit) {
                // 常量初始化
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
            symbolTable.put(id.id, new SymbolTable.Entry(id, type, type.getType(), attr));
        }
    }

    private void panicConflictType(
        IdentifierNode id, TypeNode type, SymbolTable.Entry previous, boolean alreadyDefined) {
        error();
        String msg;
        if ((previous.type instanceof FunctionType) != (type instanceof FunctionTypeNode)) {
            msg = "'" + getLogger().white(id.id) + "' redeclared as different kind of symbol";
        } else {
            msg = "conflicting types for '" + getLogger().white(id.id) + "'; have '" +
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
    public Void visit(IdentifierNode node) {
        // 始终有定义
        SymbolTable.Entry entry = symbolTable.get(node.id);
        // TypeNode type = entry.typeNode;
        Type t = entry.type;
        if (!t.isComplete()) {
            // 不完整类型不能使用
            error();
            String msg = "storage size of '" + getLogger().white(getSourceFile().getByLocation(node.wholeLoc)) +
                         "' isn't known; have type '" + getLogger().white(t.toString()) + "'";
            logErrorWithSourceLine(node.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
        } else {
            node.expType = t;
        }
        return null;
    }

    private Type getConvertTypeAsIfByAssignment(ExpressionNode rhs, Type lhsType) {
        // rhs 与 lhs 必须满足下列条件之一
        // lhs 与 rhs 拥有兼容的 struct 或 union 类型，或……
        // rhs 必须可隐式转换成 lhs，这表示
        // lhs 与 rhs 均拥有算术类型
        if (!lhsType.isArithmetic() || !rhs.expType.isArithmetic()) {
            return ErrorType.INSTANCE;
        } else {
            return lhsType;
        }
    }

    @Override
    public Void visit(AssignmentNode node) {
        node.lhs.accept(this);
        node.rhs.accept(this);
        // 简单赋值
        // rhs 与 lhs 必须满足下列条件之一
        // lhs 与 rhs 拥有兼容的 struct 或 union 类型，或……
        // rhs 必须可隐式转换成 lhs，这表示
        // lhs 与 rhs 均拥有算术类型
        if (node.op.op != AssignmentOperator.ASSIGN) {
            // 复合赋值
            // lhs, rhs	- 拥有算术类型的表达式
            if (!node.lhs.expType.isArithmetic() || !node.rhs.expType.isArithmetic()) {
                error();
                String msg = "operands of compound assignment operator " + node.op.op.getSymbol() +
                             " must have arithmetic type; have '" + getLogger().white(node.lhs.expType.toString()) +
                             "' and '" + getLogger().white(node.rhs.expType.toString()) + "'";
                logErrorWithSourceLine(node.op.wholeLoc, msg);
                node.expType = ErrorType.INSTANCE;
            } else {
                // 表达式 lhs @= rhs 与 lhs = lhs @ (rhs) 完全相同
                // 替换右表达式为新表达式
                BinaryExpressionNode binaryExp =
                    new BinaryExpressionNode(
                        new BinaryOperatorNode(node.op.wholeLoc, node.op.op.toBinaryOperator()), node.lhs, node.rhs);
                typeCheckBinaryExp(binaryExp);
                node.op = new AssignmentOperatorNode(node.op.wholeLoc, AssignmentOperator.ASSIGN);
                node.rhs = binaryExp;
                // 回到简单赋值的情形
            }
        }
        Type convertedType = getConvertTypeAsIfByAssignment(node.rhs, node.lhs.expType);
        if (convertedType instanceof ErrorType) {
            error();
            String msg =
                "incompatible types when initializing type '" + getLogger().white(node.lhs.expType.toString()) +
                "' using type '" + getLogger().white(node.rhs.expType.toString()) + "'";
            logErrorWithSourceLine(node.rhs.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
        } else {
            node.rhs = convertTo(node.rhs, convertedType);
            node.expType = node.lhs.expType;
        }
        return null;
    }

    @Override
    public Void visit(IncrementDecrementNode node) {
        node.operand.accept(this);
        // 前缀和后缀自增或自减的操作数表达式 必须为整数类型、实浮点数类型或指针类型的可修改左值
        if (!node.operand.expType.isArithmetic()) {
            error();
            String msg = "operand of " + (node.isIncrement ? "increment" : "decrement") +
                         " operator must have arithmetic type; have '" +
                         getLogger().white(node.operand.expType.toString()) + "'";
            logErrorWithSourceLine(node.operatorLoc, msg);
            node.expType = ErrorType.INSTANCE;
        } else {
            node.expType = node.operand.expType;
        }
        return null;
    }

    @Override
    public Void visit(IfStatementNode node) {
        node.cond.accept(this);
        if (!node.cond.expType.isScalar()) {
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
        // 条件 - 标量类型的表达式
        if (!node.cond.expType.isScalar()) {
            error();
            String msg = "condition of conditional operator must have scalar type; have '" +
                         getLogger().white(node.cond.expType.toString()) + "'";
            logErrorWithSourceLine(node.cond.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
            return null;
        }

        // 仅允许下列表达式为 表达式真 和 表达式假
        // 两个任何算术类型的表达式
        if (node.thenExp.expType.isArithmetic() && node.elseExp.expType.isArithmetic()) {
            // 若表达式拥有算术类型，则公共类型为一般算术转换后的类型
            Type commonType = Type.commonRealType((BasicType) node.thenExp.expType, (BasicType) node.elseExp.expType);
            node.thenExp = convertTo(node.thenExp, commonType);
            node.elseExp = convertTo(node.elseExp, commonType);
            node.expType = commonType;
            return null;
        }
        // 两个 void 类型的表达式
        if (node.thenExp.expType.isVoid() && node.elseExp.expType.isVoid()) {
            node.expType = BasicType.VOID;
            return null;
        }
        // 其他情况非法
        error();
        String msg =
            "invalid operands to conditional operator; have '" + getLogger().white(node.thenExp.expType.toString()) +
            "' and '" + getLogger().white(node.elseExp.expType.toString()) + "'";
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
        if (!node.cond.expType.isScalar()) {
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
                    String msg = "declaration of non-variable '" + getLogger().white(decl.id.id) +
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
            if (!node.cond.expType.isScalar()) {
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
        if (!node.exp.expType.isInteger()) {
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
        if (!(node.func instanceof IdentifierNode id)) {
            // 目前不允许调用函数指针
            error();
            String msg = "function call expression shall have name as function designator";
            logErrorWithSourceLine(node.func.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
        } else {
            SymbolTable.Entry entry = symbolTable.get(id.id);
            Type type = entry.type;
            if (!(type instanceof FunctionType funcType)) {
                // 不是函数类型
                error();
                String msg = "called object '" + getLogger().white(getSourceFile().getByLocation(id.wholeLoc)) +
                             "' is not a function or function pointer; have type '" +
                             getLogger().white(type.toString()) + "'";
                logErrorWithSourceLine(id.wholeLoc, msg);
                msg = "declared here";
                logNoteWithSourceLine(entry.id.wholeLoc, msg);
                node.expType = ErrorType.INSTANCE;
            } else {
                // 检查调用是否合法
                if (funcType.hasNoParameters()) {
                    // 无参数
                    if (!node.args.isEmpty()) {
                        // 传递了参数
                        error();
                        String msg = "too many arguments to function '" + getLogger().white(id.id) + "'";
                        logErrorWithSourceLine(node.func.wholeLoc, msg);
                        msg = "declared here";
                        logNoteWithSourceLine(entry.id.wholeLoc, msg);
                        node.expType = ErrorType.INSTANCE;
                    } else {
                        node.expType = funcType.returnType();
                    }
                } else {
                    // 有参数
                    if (node.args.size() < funcType.parameterTypes().size()) {
                        // 参数不足
                        error();
                        String msg = "too few arguments to function '" + getLogger().white(id.id) + "'";
                        logErrorWithSourceLine(node.func.wholeLoc, msg);
                        msg = "declared here";
                        logNoteWithSourceLine(entry.id.wholeLoc, msg);
                        node.expType = ErrorType.INSTANCE;
                    } else if (node.args.size() > funcType.parameterTypes().size()) {
                        // 参数过多
                        error();
                        String msg = "too many arguments to function '" + getLogger().white(id.id) + "'";
                        logErrorWithSourceLine(node.func.wholeLoc, msg);
                        msg = "declared here";
                        logNoteWithSourceLine(entry.id.wholeLoc, msg);
                        node.expType = ErrorType.INSTANCE;
                    } else {
                        // 形参数量必须等于实参数量（除非使用省略号形参）
                        // 参数数量正确，检查类型
                        boolean noError = true;
                        for (int i = 0; i < node.args.size(); i++) {
                            ExpressionNode arg = node.args.get(i);
                            // 检查参数类型
                            arg.accept(this);
                            // 必须存在如同赋值的隐式转换，将对应实参的无限定类型转换为形参类型
                            Type paramType = funcType.parameterTypes().get(i);
                            Type convertedType = getConvertTypeAsIfByAssignment(arg, paramType);
                            if (convertedType instanceof ErrorType) {
                                // 参数类型不兼容
                                error();
                                noError = false;
                                String msg =
                                    "incompatible type for argument " + (i + 1) + " of '" + getLogger().white(id.id) +
                                    "'";
                                logErrorWithSourceLine(node.args.get(i).wholeLoc, msg);
                                msg = "expected '" + getLogger().white(paramType.toString()) +
                                      "' but argument is of type '" + getLogger().white(arg.expType.toString()) + "'";
                                logNoteWithSourceLine(
                                    SourceLocation.concat(
                                        ((FunctionTypeNode) entry.typeNode).paramTypes.get(i).getWholeLocation(),
                                        ((FunctionTypeNode) entry.typeNode).params.get(i).getWholeLocation()),
                                    msg);
                            } else {
                                node.args.set(i, convertTo(arg, convertedType));
                            }
                        }
                        node.expType = noError ? funcType.returnType() : ErrorType.INSTANCE;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Void visit(ConstantNode node) {
        Constant value = node.value;
        if (value instanceof ConstantInt) {
            node.expType = BasicType.INT;
        } else if (value instanceof ConstantLong) {
            node.expType = BasicType.LONG;
        } else if (value instanceof ConstantUnsignedInt) {
            node.expType = BasicType.UNSIGNED_INT;
        } else if (value instanceof ConstantUnsignedLong) {
            node.expType = BasicType.UNSIGNED_LONG;
        } else {
            throw new IllegalStateException("unexpected constant type: " + value);
        }
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
        Type convertedType = getConvertTypeAsIfByAssignment(node.exp, targetType);
        if (convertedType instanceof ErrorType) {
            error();
            String msg = "invalid cast from type '" + getLogger().white(node.exp.expType.toString()) + "' to '" +
                         getLogger().white(targetType.toString()) + "'";
            logErrorWithSourceLine(node.exp.wholeLoc, msg);
            node.expType = ErrorType.INSTANCE;
        } else {
            node.exp = convertTo(node.exp, convertedType);
            node.expType = targetType;
        }
        return null;
    }
}
