package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.StorageClassSpecifier;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

public final class TypeCheckingPass extends SemanticAnalysePass implements AstVisitor<Void> {

    public TypeCheckingPass() {
        super(new ConsoleLogger());
    }

    private final SymbolTable symbolTable = new SymbolTable();
    private boolean isFileScope = true;

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
                    symbolTable.put(id.id, new SymbolTable.Entry(id, type, SymbolTable.Entry.LocalAttr.INSTANCE));
                }
            }
        }
        // 检查函数体
        isFileScope = false;
        visit(node.body);
        isFileScope = true;
        return null;
    }

    @Override
    public Void visit(ReturnNode node) {
        node.exp.accept(this);
        return null;
    }

    @Override
    public Void visit(UnaryExpressionNode node) {
        node.exp.accept(this);
        return null;
    }

    @Override
    public Void visit(BinaryExpressionNode node) {
        node.lhs.accept(this);
        node.rhs.accept(this);
        return null;
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
            if (isFileScope) {
                visitFileScopeVariableDeclaration(node.id, node.type, node.storageClass, node.init);
            } else {
                visitBlockScopeVariableDeclaration(node.id, node.type, node.storageClass, node.init);
            }
        }
        return null;
    }

    private void panicWithPreviousRef(
        String msg, IdentifierNode id, SymbolTable.Entry previous, boolean defined) {
        logErrorWithSourceLine(id.wholeLoc, msg);
        msg = "previous " + (defined ? "definition" : "declaration") + " of '" +
              getLogger().white(id.id) + "' with type '" + getLogger().white(previous.type.getType().toString()) + "'";
        logNoteWithSourceLine(previous.id.wholeLoc, msg);
    }

    public void visitFunctionDeclaration(
        IdentifierNode id, FunctionTypeNode funcType, @Nullable StorageClassSpecifierNode storageClass,
        boolean isDefinition) {

        // 检查返回类型
        // 目前只能是 int
        if (!(funcType.retType instanceof BasicTypeNode returnType) ||
            returnType.getType().getKind() != BasicType.Kind.INT) {
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
            if (!previous.type.getType().isCompatible(funcType.getType())) {
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
            symbolTable.put(id.id, new SymbolTable.Entry(id, funcType, attr));
        }
    }

    public void visitFileScopeVariableDeclaration(
        IdentifierNode id, TypeNode type, @Nullable StorageClassSpecifierNode storageClass,
        @Nullable ExpressionNode init) {

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
        } else if (init instanceof IntConstantNode intConstant) {
            // 整数常量初始化
            initialValue = new SymbolTable.Entry.StaticAttr.Initial(intConstant.value);
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
            if (!previous.type.getType().isCompatible(type.getType())) {
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
            symbolTable.put(id.id, new SymbolTable.Entry(id, type, attr));
        }
    }

    public void visitBlockScopeVariableDeclaration(
        IdentifierNode id, TypeNode type, @Nullable StorageClassSpecifierNode storageClass,
        @Nullable ExpressionNode init) {

        if (storageClass == null) {
            // 无存储类说明符，不可能重复定义
            SymbolTable.Entry.LocalAttr attr = SymbolTable.Entry.LocalAttr.INSTANCE;
            symbolTable.put(id.id, new SymbolTable.Entry(id, type, attr));
            if (init != null) {
                init.accept(this);
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
                if (!previous.type.getType().isCompatible(type.getType())) {
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
                symbolTable.put(id.id, new SymbolTable.Entry(id, type, attr));
            }
        } else {
            // static
            SymbolTable.Entry.StaticAttr.InitialValue initialValue;
            if (init == null) {
                // 块作用域 static 无初始化器，初始化为 0
                initialValue = SymbolTable.Entry.StaticAttr.Initial.ZERO;
            } else if (init instanceof IntConstantNode intConstant) {
                // 整数常量初始化
                initialValue = new SymbolTable.Entry.StaticAttr.Initial(intConstant.value);
            } else {
                // 其他类型的初始化表达式不合法
                error();
                String msg = "initializer element is not constant";
                logErrorWithSourceLine(init.wholeLoc, msg);
                // 这里不 return，继续处理下面的定义，防止后续引用无定义
                // 设置一个 dummy 值
                initialValue = SymbolTable.Entry.StaticAttr.Initial.ZERO;
            }
            // static 块作用域变量为 No Linkage，不可能重复定义（在 Identifier Resolution 中已检查）
            SymbolTable.Entry.IdentifierAttr attr = new SymbolTable.Entry.StaticAttr(initialValue, false);
            symbolTable.put(id.id, new SymbolTable.Entry(id, type, attr));
        }
    }

    private void panicConflictType(
        IdentifierNode id, TypeNode type, SymbolTable.Entry previous, boolean alreadyDefined) {
        error();
        String msg;
        if ((previous.type instanceof FunctionTypeNode) != (type instanceof FunctionTypeNode)) {
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
        TypeNode type = entry.type;
        if (type instanceof FunctionTypeNode) {
            // 函数类型不能作为表达式使用
            error();
            String msg = "function used in arithmetic";
            logErrorWithSourceLine(node.wholeLoc, msg);
        } else if (!type.getType().isComplete()) {
            // 不完整类型不能使用
            error();
            String msg = "storage size of '" + getLogger().white(getSourceFile().getByLocation(node.wholeLoc)) +
                         "' isn't known; have type '" + getLogger().white(type.getType().toString()) + "'";
            logErrorWithSourceLine(node.wholeLoc, msg);
        }
        return null;
    }

    @Override
    public Void visit(AssignmentNode node) {
        node.lhs.accept(this);
        node.rhs.accept(this);
        return null;
    }

    @Override
    public Void visit(IncrementDecrementNode node) {
        node.operand.accept(this);
        return null;
    }

    @Override
    public Void visit(IfStatementNode node) {
        node.cond.accept(this);
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
        } else {
            SymbolTable.Entry entry = symbolTable.get(id.id);
            TypeNode type = entry.type;
            if (!(type instanceof FunctionTypeNode funcType)) {
                // 不是函数类型
                error();
                String msg = "called object '" + getLogger().white(getSourceFile().getByLocation(id.wholeLoc)) +
                             "' is not a function or function pointer; have type '" +
                             getLogger().white(type.getType().toString()) + "'";
                logErrorWithSourceLine(id.wholeLoc, msg);
                msg = "declared here";
                logNoteWithSourceLine(entry.id.wholeLoc, msg);
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
                    }
                } else {
                    // 有参数
                    if (node.args.size() < funcType.paramTypes.size()) {
                        // 参数不足
                        error();
                        String msg = "too few arguments to function '" + getLogger().white(id.id) + "'";
                        logErrorWithSourceLine(node.func.wholeLoc, msg);
                        msg = "declared here";
                        logNoteWithSourceLine(entry.id.wholeLoc, msg);
                    } else if (node.args.size() > funcType.paramTypes.size()) {
                        // 参数过多
                        error();
                        String msg = "too many arguments to function '" + getLogger().white(id.id) + "'";
                        logErrorWithSourceLine(node.func.wholeLoc, msg);
                        msg = "declared here";
                        logNoteWithSourceLine(entry.id.wholeLoc, msg);
                    } else {
                        // 参数数量正确，检查类型
                        for (int i = 0; i < node.args.size(); i++) {
                            // 假设所有表达式类型都是 int
                            Type argType = BasicType.INT;
                            TypeNode paramType = funcType.paramTypes.get(i);
                            if (!argType.isCompatible(paramType.getType())) {
                                // 参数类型不兼容
                                error();
                                String msg =
                                    "incompatible type for argument " + (i + 1) + " of '" + getLogger().white(id.id) +
                                    "'";
                                logErrorWithSourceLine(node.args.get(i).wholeLoc, msg);
                                msg = "expected '" + getLogger().white(paramType.getType().toString()) +
                                      "' but argument is of type '" + getLogger().white(argType.toString()) + "'";
                                logNoteWithSourceLine(
                                    SourceLocation.concat(
                                        funcType.paramTypes.get(i).getWholeLocation(),
                                        funcType.params.get(i).getWholeLocation()),
                                    msg);
                            }
                        }
                    }
                }
            }
        }
        // 检查所有的参数
        for (ExpressionNode arg : node.args) {
            arg.accept(this);
        }
        return null;
    }
}
