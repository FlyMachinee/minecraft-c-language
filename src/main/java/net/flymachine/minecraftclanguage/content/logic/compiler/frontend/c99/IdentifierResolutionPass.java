package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.StorageClassSpecifier;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * 将变量的名字替换为唯一的名字，并检查变量的重复定义和未定义使用
 */
public final class IdentifierResolutionPass extends SemanticAnalysePass implements AstVisitor<Void> {

    public IdentifierResolutionPass() {
        super(new ConsoleLogger());
    }

    private int renameCounter = 0;

    private String makeUniqueName(String base) {
        return base + ".." + (renameCounter++);
    }

    private final Stack<Map<String, IdentifierEntry>> scopeStack = new Stack<>();

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
        scopeStack.push(new HashMap<>());
    }

    private void exitScope() {
        scopeStack.pop();
    }

    private boolean definedInCurrentScope(String identifier) {
        return scopeStack.peek().containsKey(identifier);
    }

    private boolean definedInScope(String identifier, Map<String, IdentifierEntry> scope) {
        return scope.containsKey(identifier);
    }

    private boolean inGlobalScope() {
        return scopeStack.size() == 1;
    }

    private IdentifierEntry definitionInCurrentScope(String identifier) {
        return scopeStack.peek().get(identifier);
    }

    private IdentifierEntry definitionOf(String identifier) {
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Map<String, IdentifierEntry> scope = scopeStack.get(i);
            if (definedInScope(identifier, scope)) {
                return scope.get(identifier);
            }
        }
        return null;
    }

    private void define(String identifier, IdentifierEntry definition) {
        scopeStack.peek().put(identifier, definition);
    }

    @Override
    public Void visit(ProgramNode node) {
        enterScope();
        for (ExternalDeclarationNode externalDeclaration : node.extDecls) {
            externalDeclaration.accept(this);
        }
        exitScope();
        return null;
    }

    private void panicWithPreviousRef(String msg, IdentifierNode id, IdentifierEntry previous) {
        logErrorWithSourceLine(id.wholeLoc, msg);
        msg = "previous " + (previous.defined ? "definition" : "declaration") + " of '" +
              getLogger().white(id.id) + "' with type '" +
              getLogger().white(previous.t.getType().toString()) + "'";
        logNoteWithSourceLine(previous.id.wholeLoc, msg);
    }

    private void visitFunctionTypeNode(FunctionTypeNode funcType, boolean isDefinition) {
        if (isDefinition) {
            // 如果 isDefinition，要求参数要么是单独的 void，要么就必须具名
            // 若检查无误，则将形参换名并定义在当前作用域

            if (funcType.hasNoParameters()) {
                // 参数列表为单独的 void
                return;
            }

            for (int i = 0; i < funcType.params.size(); i++) {
                // 检查是否具名
                if (funcType.params.get(i) == null) {
                    error();
                    String msg = "ISO C99 does not support omitting parameter names in function definitions";
                    logErrorWithSourceLine(funcType.paramTypes.get(i).getWholeLocation(), msg);
                } else {
                    visitDeclarationLike(funcType.params.get(i), funcType.paramTypes.get(i), null, true);
                }
            }
        } else {
            // 否则，只要求参数列表中的参数名不重复即可，参数可不具名，也不会被定义
            HashMap<String, IdentifierEntry> scope = new HashMap<>();
            for (int i = 0; i < funcType.params.size(); i++) {
                if (funcType.params.get(i) == null) { continue; }

                IdentifierNode identifier = funcType.params.get(i);
                TypeNode type = funcType.paramTypes.get(i);

                IdentifierEntry entry = scope.get(identifier.id);
                if (entry == null) {
                    // 这里认为参数声明是定义，为 No Linkage
                    scope.put(identifier.id, new IdentifierEntry(identifier, type, false, true));
                } else {
                    error();
                    String msg = "redefinition of parameter '" + getLogger().white(identifier.id) + "'";
                    panicWithPreviousRef(msg, identifier, entry);
                }
            }
        }
        // 递归检查返回类型和参数类型
        if (funcType.retType instanceof FunctionTypeNode) {
            visitFunctionTypeNode((FunctionTypeNode) funcType.retType, false);
        }
        for (TypeNode paramType : funcType.paramTypes) {
            if (paramType instanceof FunctionTypeNode) {
                visitFunctionTypeNode((FunctionTypeNode) paramType, false);
            }
        }
    }

    @Override
    public Void visit(FunctionDefinitionNode node) {
        visitDeclarationLike(node.id, node.funcType, node.storageClass, true);
        enterScope();
        if (node.funcType instanceof FunctionTypeNode) {
            visitFunctionTypeNode((FunctionTypeNode) node.funcType, true);
        }
        for (BlockItemNode item : node.body.blockItems) {
            item.accept(this);
        }
        exitScope();
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
        visitDeclarationLike(
            node.id, node.type, node.storageClass,
            !(node.type instanceof FunctionTypeNode) && node.init != null);
        if (node.type instanceof FunctionTypeNode funcType) {
            visitFunctionTypeNode(funcType, false);
        }
        if (node.init != null) {
            node.init.accept(this);
        }
        return null;
    }

    private void visitDeclarationLike(
        IdentifierNode id, TypeNode type, @Nullable StorageClassSpecifierNode storageClass, boolean defined) {

        String name = id.id;
        IdentifierEntry previous = definitionOf(name);
        if (type instanceof FunctionTypeNode) {
            // 函数声明，始终有链接
            if (previous != null && definedInCurrentScope(name) && !previous.hasLinkage) {
                // 当前作用域先前的声明无链接，肯定是变量，当前声明是函数声明，有链接，冲突
                error();
                String msg = "'" + getLogger().white(name) + "' redeclared as different kind of symbol";
                panicWithPreviousRef(msg, id, previous);
                return;
            }
            // 无先前声明、先前声明有链接、先前声明不在当前作用域，这里暂时不管类型
            if (!inGlobalScope() && storageClass != null &&
                storageClass.storageClass.equals(StorageClassSpecifier.STATIC)) {
                // 块作用域函数声明为 static 非法
                error();
                String msg = "invalid storage class for function '" + getLogger().white(name) + "'";
                logErrorWithSourceLine(id.wholeLoc, msg);
            }
            // 函数声明不需要重命名
            if (previous != null && previous.defined && previous.hasLinkage) {
                // 先前有声明、有链接且为定义，引用先前的定义
                define(name, new IdentifierEntry(previous.id, previous.t, true, true));
            } else {
                // 先前无声明，或先前声明不是定义，或先前声明无链接
                // 引入新符号
                define(name, new IdentifierEntry(id, type, true, defined));
            }
        } else {
            // 变量声明，如果在全局作用域则有链接，否则没有链接
            if (inGlobalScope()) {
                // 全局变量不需要重命名
                // 这里不考虑可能的定义冲突，随后在类型检查中处理
                if (previous == null || !previous.defined) {
                    // 先前无声明，或先前声明不是定义
                    define(name, new IdentifierEntry(id, type, true, defined));
                }
                // 否则采用先前的定义，目前在全局作用域，那么我们不需要再次定义，使用先前的即可
            } else {
                // 块作用域变量
                if (previous != null) {
                    // 先前有声明，而且是当前作用域的
                    if (definedInCurrentScope(name) &&
                        !(previous.hasLinkage && storageClass != null &&
                          storageClass.storageClass.equals(StorageClassSpecifier.EXTERN))) {
                        // 当前作用域之前的声明有链接，并且当前声明是 extern，则允许重定义
                        // 除此之外，声明冲突
                        error();
                        String msg;
                        if (!previous.hasLinkage) {
                            // 先前的声明无链接
                            if (storageClass == null ||
                                !storageClass.storageClass.equals(StorageClassSpecifier.EXTERN)) {
                                // 当前的声明无链接
                                msg =
                                    (defined ? "redefinition" : "redeclaration") + " of '" + getLogger().white(name) +
                                    "' with no linkage";
                            } else {
                                // 当前的声明是 extern，有链接
                                if (defined) {
                                    msg = "redefinition of '" + getLogger().white(name) + "'";
                                } else {
                                    msg = "extern declaration of '" + getLogger().white(name) +
                                          "' follows declaration with no linkage";
                                }
                            }
                        } else {
                            // 先前的声明有链接，则当前声明无链接
                            msg = (defined ? "redefinition" : "redeclaration") + " of '" + getLogger().white(name) +
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
                        define(name, new IdentifierEntry(previous.id, previous.t, true, true));
                    } else {
                        // 先前无声明，或先前声明不是定义，或先前声明无链接
                        // 引入新符号
                        define(name, new IdentifierEntry(id, type, true, defined));
                    }
                } else {
                    // 无链接，需要重命名
                    id.id = makeUniqueName(name);
                    define(name, new IdentifierEntry(id, type, false, defined));
                }
            }
        }
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
        String name = node.id;
        IdentifierEntry renamed = definitionOf(name);
        if (renamed == null) {
            error();
            String msg = "'" + getLogger().white(name) + "' undeclared";
            logErrorWithSourceLine(node.wholeLoc, msg);
        } else {
            node.id = renamed.id.id;
        }
        return null;
    }

    @Override
    public Void visit(AssignmentNode node) {
        if (!(node.lhs instanceof IdentifierNode)) {
            error();
            String msg = "lvalue required as left operand of assignment";
            logErrorWithSourceLine(node.op.wholeLoc, msg);
        }
        node.lhs.accept(this);
        node.rhs.accept(this);
        return null;
    }

    @Override
    public Void visit(IncrementDecrementNode node) {
        if (!(node.operand instanceof IdentifierNode)) {
            error();
            String msg;
            if (node.isIncrement) {
                msg = "lvalue required as increment operand";
            } else {
                msg = "lvalue required as decrement operand";
            }
            logErrorWithSourceLine(node.operatorLoc, msg);
        }
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
        enterScope();
        for (BlockItemNode item : node.blockItems) {
            item.accept(this);
        }
        exitScope();
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
        node.body.accept(this);
        exitScope();
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
        node.func.accept(this);
        for (ExpressionNode arg : node.args) {
            arg.accept(this);
        }
        return null;
    }
}
