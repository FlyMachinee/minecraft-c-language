package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.StorageClassSpecifier;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.StructInfo;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.scope.ScopeStack;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;
import org.jetbrains.annotations.Nullable;

/**
 * 将变量的名字替换为唯一的名字，并检查变量的重复定义和未定义使用
 */
public final class IdentifierResolutionPass implements AstVisitor<Void> {

    private final DiagnosticReporter reporter;

    public IdentifierResolutionPass(DiagnosticReporter reporter) {
        this.reporter = reporter;
    }

    private int renameCounter = 0;

    private String makeUniqueName(String base) {
        return base + ".." + (renameCounter++);
    }

    private final ScopeStack<String, IdentifierEntry> scopeStack = new ScopeStack<>();
    private final ScopeStack<String, StructInfo> tagScopeStack = new ScopeStack<>();

    private static class IdentifierEntry {
        public IdentifierNode id;
        public TypeNode t;
        public boolean hasLinkage;
        public boolean defined; // 仅用于信息打印，不参与逻辑检查

        public IdentifierEntry(IdentifierNode id, TypeNode t, boolean hasLinkage, boolean defined) {
            this.id = id;
            this.t = t;
            this.hasLinkage = hasLinkage;
            this.defined = defined;
        }
    }

    private void enterScope() {
        scopeStack.enterScope();
        tagScopeStack.enterScope();
    }

    private void exitScope() {
        scopeStack.exitScope();
        tagScopeStack.exitScope();
    }

    private boolean inGlobalScope() {
        return scopeStack.inGlobalScope();
    }

    @Override
    public Void visit(ProgramNode node) {
        for (ExternalDeclarationNode externalDeclaration : node.extDecls) {
            externalDeclaration.accept(this);
        }
        return null;
    }

    private void visitStatementLabel(StatementNode node) {
        for (var caseLabel : node.caseLabels) {
            caseLabel.caseValue.accept(this);
        }
    }

    private void panicWithPreviousRef(String msg, IdentifierNode id, IdentifierEntry previous) {
        reporter.error(id.wholeLoc, msg);
        msg = "previous " + (previous.defined ? "definition" : "declaration") + " of '" +
              reporter.white(id.name) + "' with type '" +
              reporter.white(previous.t.typename()) + "'";
        reporter.note(previous.id.wholeLoc, msg);
    }

    private void visitTypeNode(TypeNode type) {
        if (type instanceof FunctionTypeNode funcType) {
            visitTypeNode(funcType.retType);
            visitFunctionTypeNode(funcType, false);
        }

        if (type instanceof ArrayTypeNode arrayType) {
            if (arrayType.size != null) {
                arrayType.size.accept(this);
            }
            visitTypeNode(arrayType.elementType);
        }

        if (type instanceof PointerTypeNode pointerType) {
            visitTypeNode(pointerType.referencedType);
        }

        if (type instanceof StructTypeNode structType) {
            if (structType.resolved()) {
                return;
            }
            if (structType.memberDeclarations != null) {
                // struct tag? { ... } ...?
                // 结构体定义，仅在当前作用域中查找
                if (structType.tag == null) {
                    // 匿名结构体
                    StructInfo info = new StructInfo(null);
                    structType.resolve(info);
                } else {
                    String tag = structType.tag.name;
                    if (tagScopeStack.declaredInCurrentScope(tag)) {
                        // 当前作用域已有声明，进行引用
                        StructInfo previous = tagScopeStack.declarationInCurrentScope(tag).orElseThrow();
                        structType.resolve(previous);
                    } else {
                        // 当前作用域无定义，添加声明
                        StructInfo info = new StructInfo(tag);
                        structType.resolve(info);
                        tagScopeStack.declare(tag, info);
                    }
                }
                for (MemberDeclarationNode memberDecl : structType.memberDeclarations) {
                    visitMemberDeclaration(memberDecl);
                }
            } else {
                // struct tag
                assert structType.tag != null;
                String tag = structType.tag.name;
                // 查找先前声明
                if (tagScopeStack.declaredInScope(tag)) {
                    // 先前已有声明，进行引用
                    StructInfo previous = tagScopeStack.declarationOf(tag).orElseThrow();
                    structType.resolve(previous);
                } else {
                    // 无声明，在当前作用域添加声明
                    StructInfo info = new StructInfo(tag);
                    structType.resolve(info);
                    tagScopeStack.declare(tag, info);
                }
            }
        }
    }

    private void visitMemberDeclaration(MemberDeclarationNode memberDecl) {
        // 结构体成员声明中，每行都至少声明了一个标识符
        // 故这里我们不处理其基类型
        for (MemberDeclaratorNode memberDeclarator : memberDecl.memberDeclarators) {
            visitTypeNode(memberDeclarator.finalType);
        }
    }

    private void visitFunctionTypeNode(FunctionTypeNode funcType, boolean isDefinition) {
        if (funcType.hasNoParameters()) {
            // 参数列表为单独的 void
            return;
        }

        if (!isDefinition) {
            enterScope(); // 函数原型作用域
        }

        for (int i = 0; i < funcType.params.size(); i++) {
            TypeNode type = funcType.paramTypes.get(i);
            visitTypeNode(type);

            IdentifierNode identifier = funcType.params.get(i);
            if (identifier == null) {
                if (isDefinition) {
                    String msg = "ISO C99 does not support omitting parameter names in function definitions";
                    reporter.error(type.getWholeLocation(), msg);
                }
                // 函数声明中允许参数不具名
                continue;
            }

            // 这里认为参数声明是定义，为 No Linkage
            visitDeclarationLike(identifier, type, null, true);
        }

        if (!isDefinition) {
            exitScope(); // 函数原型作用域
        }
    }

    @Override
    public Void visit(FunctionDefinitionNode node) {
        if (node.funcType instanceof FunctionTypeNode funcType) {
            // 先处理其返回类型
            visitTypeNode(funcType.retType);
        } else {
            // 函数定义不是函数类型，将在类型检查中报错，这里先递归处理其类型
            visitTypeNode(node.funcType);
        }

        // 函数体作用域
        enterScope();
        // 再处理形参列表
        if (node.funcType instanceof FunctionTypeNode funcType) {
            visitFunctionTypeNode(funcType, true);
        }

        // 声明结束，回过头来定义函数名本身
        // 回到全局作用域
        var bodyScope = scopeStack.popScope();
        var tagBodyScope = tagScopeStack.popScope();
        visitDeclarationLike(node.id, node.funcType, node.storageClass, true);

        // 回到函数体作用域
        scopeStack.pushScope(bodyScope);
        tagScopeStack.pushScope(tagBodyScope);

        // 处理函数体
        for (BlockItemNode item : node.body.blockItems) {
            item.accept(this);
        }

        // 退出函数体作用域
        exitScope();
        return null;
    }

    @Override
    public Void visit(ReturnNode node) {
        visitStatementLabel(node);
        if (node.exp != null) {
            node.exp.accept(this);
        }
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
        if (node.initDeclarators.isEmpty()) {
            if (node.baseType instanceof StructTypeNode structType) {
                if (structType.memberDeclarations == null) {
                    // struct S;
                    assert structType.tag != null;
                    String tag = structType.tag.name;
                    // 检查当前作用域是否有声明
                    if (tagScopeStack.declaredInCurrentScope(tag)) {
                        // 当前作用域已有声明，进行引用
                        StructInfo previous = tagScopeStack.declarationInCurrentScope(tag).orElseThrow();
                        structType.resolve(previous);
                    } else {
                        // 当前作用域无定义，添加声明
                        StructInfo info = new StructInfo(tag);
                        structType.resolve(info);
                        tagScopeStack.declare(tag, info);
                    }
                    return null;
                } else {
                    // struct tag? { ... };
                    if (structType.tag == null) {
                        // 匿名结构体，且没有相关联的标识符
                        String msg = "unnamed struct/union that defines no instances";
                        reporter.warning(structType.wholeLoc, msg);
                    }
                    visitTypeNode(structType);
                }
            }
        }

        for (InitDeclaratorNode initDecl : node.initDeclarators) {
            visitTypeNode(initDecl.finalType);
            visitDeclarationLike(
                initDecl.id, initDecl.finalType, node.storageClass,
                !(initDecl.finalType instanceof FunctionTypeNode) && initDecl.init != null);
            if (initDecl.init != null) {
                initDecl.init.accept(this);
            }
        }
        return null;
    }

    private void visitDeclarationLike(
        IdentifierNode id, TypeNode type, @Nullable StorageClassSpecifierNode storageClass, boolean defined) {

        String name = id.name;
        IdentifierEntry previous = scopeStack.declarationOf(name).orElse(null);
        if (type instanceof FunctionTypeNode) {
            // 函数声明，始终有链接
            if (previous != null && scopeStack.declaredInCurrentScope(name) && !previous.hasLinkage) {
                // 当前作用域先前的声明无链接，肯定是变量，当前声明是函数声明，有链接，冲突
                String msg = "'" + reporter.white(name) + "' redeclared as different kind of symbol";
                panicWithPreviousRef(msg, id, previous);
                return;
            }
            // 无先前声明、先前声明有链接、先前声明不在当前作用域，这里暂时不管类型
            if (!inGlobalScope() && storageClass != null &&
                storageClass.storageClass.equals(StorageClassSpecifier.STATIC)) {
                // 块作用域函数声明为 static 非法
                String msg = "invalid storage class for function '" + reporter.white(name) + "'";
                reporter.error(id.wholeLoc, msg);
            }
            // 函数声明不需要重命名
            if (previous != null && previous.defined && previous.hasLinkage) {
                // 先前有声明、有链接且为定义，引用先前的定义
                scopeStack.declare(name, new IdentifierEntry(previous.id, previous.t, true, true));
            } else {
                // 先前无声明，或先前声明不是定义，或先前声明无链接
                // 引入新符号
                scopeStack.declare(name, new IdentifierEntry(id, type, true, defined));
            }
        } else {
            // 变量声明，如果在全局作用域则有链接，否则没有链接
            if (inGlobalScope()) {
                // 全局变量不需要重命名
                // 这里不考虑可能的定义冲突，随后在类型检查中处理
                if (previous == null || !previous.defined) {
                    // 先前无声明，或先前声明不是定义
                    scopeStack.declare(name, new IdentifierEntry(id, type, true, defined));
                }
                // 否则采用先前的定义，目前在全局作用域，那么我们不需要再次定义，使用先前的即可
            } else {
                // 块作用域变量
                if (previous != null) {
                    // 先前有声明，而且是当前作用域的
                    if (scopeStack.declaredInCurrentScope(name) &&
                        !(previous.hasLinkage && storageClass != null &&
                          storageClass.storageClass.equals(StorageClassSpecifier.EXTERN))) {
                        // 当前作用域之前的声明有链接，并且当前声明是 extern，则允许重定义
                        // 除此之外，声明冲突
                        String msg;
                        if (!previous.hasLinkage) {
                            // 先前的声明无链接
                            if (storageClass == null ||
                                !storageClass.storageClass.equals(StorageClassSpecifier.EXTERN)) {
                                // 当前的声明无链接
                                msg =
                                    (defined ? "redefinition" : "redeclaration") + " of '" + reporter.white(name) +
                                    "' with no linkage";
                            } else {
                                // 当前的声明是 extern，有链接
                                if (defined) {
                                    msg = "redefinition of '" + reporter.white(name) + "'";
                                } else {
                                    msg = "extern declaration of '" + reporter.white(name) +
                                          "' follows declaration with no linkage";
                                }
                            }
                        } else {
                            // 先前的声明有链接，则当前声明无链接
                            msg = (defined ? "redefinition" : "redeclaration") + " of '" + reporter.white(name) +
                                  "' with no linkage";
                        }
                        panicWithPreviousRef(msg, id, previous);
                        return;
                    }
                }
                // 声明不冲突或先前无声明（不考虑类型）
                if (storageClass != null && storageClass.storageClass.equals(StorageClassSpecifier.EXTERN)) {
                    // 当前声明是 extern，有链接，不需要重命名
                    if (previous != null && previous.defined && previous.hasLinkage) {
                        // 先前有声明、有链接且为定义，引用先前的定义
                        scopeStack.declare(name, new IdentifierEntry(previous.id, previous.t, true, true));
                    } else {
                        // 先前无声明，或先前声明不是定义，或先前声明无链接
                        // 引入新符号
                        scopeStack.declare(name, new IdentifierEntry(id, type, true, defined));
                    }
                } else {
                    // 无链接，需要重命名
                    id.name = makeUniqueName(name);
                    scopeStack.declare(name, new IdentifierEntry(id, type, false, defined));
                }
            }
        }
    }

    @Override
    public Void visit(ExpressionStatementNode node) {
        visitStatementLabel(node);
        node.exp.accept(this);
        return null;
    }

    @Override
    public Void visit(NullStatementNode node) {
        visitStatementLabel(node);
        return null;
    }

    @Override
    public Void visit(VariableNode node) {
        String name = node.id.name;
        IdentifierEntry renamed = scopeStack.declarationOf(name).orElse(null);
        if (renamed == null) {
            reporter.error(node.wholeLoc, "'" + reporter.white(name) + "' undeclared");
        } else {
            node.id.name = renamed.id.name;
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

    private void visitSubstatement(StatementNode node) {
        if (node instanceof CompoundStatementNode || node instanceof IfStatementNode ||
            node instanceof SwitchStatementNode || node instanceof WhileLoopNode || node instanceof ForLoopNode) {
            node.accept(this);
        } else {
            enterScope();
            node.accept(this);
            exitScope();
        }
    }

    @Override
    public Void visit(IfStatementNode node) {
        visitStatementLabel(node);
        enterScope();
        node.cond.accept(this);
        visitSubstatement(node.thenStmt);
        if (node.elseStmt != null) {
            visitSubstatement(node.elseStmt);
        }
        exitScope();
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
        visitStatementLabel(node);
        return null;
    }

    @Override
    public Void visit(CompoundStatementNode node) {
        visitStatementLabel(node);
        enterScope();
        for (BlockItemNode item : node.blockItems) {
            item.accept(this);
        }
        exitScope();
        return null;
    }

    @Override
    public Void visit(BreakNode node) {
        visitStatementLabel(node);
        return null;
    }

    @Override
    public Void visit(ContinueNode node) {
        visitStatementLabel(node);
        return null;
    }

    @Override
    public Void visit(WhileLoopNode node) {
        visitStatementLabel(node);
        enterScope();
        if (node.isDoWhile) {
            visitSubstatement(node.body);
            node.cond.accept(this);
        } else {
            node.cond.accept(this);
            visitSubstatement(node.body);
        }
        exitScope();
        return null;
    }

    @Override
    public Void visit(ForLoopNode node) {
        visitStatementLabel(node);
        enterScope();
        if (node.init != null) {
            node.init.accept(this);
        }
        if (node.cond != null) {
            node.cond.accept(this);
        }
        if (node.step != null) {
            node.step.accept(this);
        }
        visitSubstatement(node.body);
        exitScope();
        return null;
    }

    @Override
    public Void visit(SwitchStatementNode node) {
        visitStatementLabel(node);
        enterScope();
        node.exp.accept(this);
        visitSubstatement(node.body);
        exitScope();
        return null;
    }

    @Override
    public Void visit(FunctionCallNode node) {
        node.func.accept(this);
        for (ExpressionNode arg : node.args) {
            arg.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ConstantNode node) {
        return null;
    }

    @Override
    public Void visit(CastExpressionNode node) {
        visitTypeNode(node.targetType);
        return node.exp.accept(this);
    }

    @Override
    public Void visit(AddressOfNode node) {
        node.exp.accept(this);
        return null;
    }

    @Override
    public Void visit(DereferenceNode node) {
        node.exp.accept(this);
        return null;
    }

    @Override
    public Void visit(SubscriptNode node) {
        node.lhs.accept(this);
        node.rhs.accept(this);
        return null;
    }

    @Override
    public Void visit(StringLiteralNode node) {
        return null;
    }

    @Override
    public Void visit(SizeOfNode node) {
        node.exp.accept(this);
        return null;
    }

    @Override
    public Void visit(SizeOfTypeNode node) {
        visitTypeNode(node.type);
        return null;
    }

    @Override
    public Void visit(CommaExpressionNode node) {
        node.lhs.accept(this);
        node.rhs.accept(this);
        return null;
    }

    @Override
    public Void visit(MemberAccessNode node) {
        node.base.accept(this);
        return null;
    }

    @Override
    public Void visit(PointerMemberAccessNode node) {
        node.pointer.accept(this);
        return null;
    }
}
