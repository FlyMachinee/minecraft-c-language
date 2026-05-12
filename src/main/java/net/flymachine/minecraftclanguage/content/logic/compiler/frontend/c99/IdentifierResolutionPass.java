package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Linkage;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;

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
        public Linkage linkage;

        public IdentifierEntry(IdentifierNode id, TypeNode t, Linkage linkage) {
            this.id = id;
            this.t = t;
            this.linkage = linkage;
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
        for (ExternalDeclarationNode externalDeclaration : node.declarations) {
            externalDeclaration.accept(this);
        }
        exitScope();
        return null;
    }

    private void visitFunctionTypeNode(FunctionTypeNode funcType, boolean isDefinition) {
        if (isDefinition) {
            // 如果 isDefinition，要求参数要么是单独的 void，要么就必须具名
            // 若检查无误，则将形参换名并定义在当前作用域

            if (funcType.hasNoParameters()) {
                // 参数列表为单独的 void
                return;
            }

            for (int i = 0; i < funcType.parameters.size(); i++) {
                // 检查是否具名
                if (funcType.parameters.get(i) == null) {
                    error();
                    String msg = "ISO C99 does not support omitting parameter names in function definitions";
                    logErrorWithSourceLine(funcType.parameterTypes.get(i).getWholeLocation(), msg);
                } else {
                    visitDeclarationLike(funcType.parameters.get(i), funcType.parameterTypes.get(i));
                }
            }
        } else {
            // 否则，只要求参数列表中的参数名不重复即可，参数可不具名，也不会被定义
            HashMap<String, IdentifierEntry> scope = new HashMap<>();
            for (int i = 0; i < funcType.parameters.size(); i++) {
                if (funcType.parameters.get(i) == null) { continue; }

                IdentifierNode identifier = funcType.parameters.get(i);
                TypeNode type = funcType.parameterTypes.get(i);

                IdentifierEntry entry = scope.get(identifier.id);
                if (entry == null) {
                    scope.put(identifier.id, new IdentifierEntry(identifier, type, Linkage.NONE));
                } else {
                    error();
                    String msg = "redefinition of '" + getLogger().white(identifier.id) + "'";
                    logErrorWithSourceLine(identifier.wholeLocation, msg);
                    msg = "previous definition of '" + getLogger().white(identifier.id) +
                          "' with type '" + getLogger().white(entry.t.getType().toString()) + "'";
                    logNoteWithSourceLine(entry.id.wholeLocation, msg);
                }
            }
        }
        // 递归检查返回类型和参数类型
        if (funcType.returnType instanceof FunctionTypeNode) {
            visitFunctionTypeNode((FunctionTypeNode) funcType.returnType, false);
        }
        for (TypeNode paramType : funcType.parameterTypes) {
            if (paramType instanceof FunctionTypeNode) {
                visitFunctionTypeNode((FunctionTypeNode) paramType, false);
            }
        }
    }

    @Override
    public Void visit(FunctionDefinitionNode node) {
        visitDeclarationLike(node.identifier, node.functionType);
        enterScope();
        if (node.functionType instanceof FunctionTypeNode) {
            visitFunctionTypeNode((FunctionTypeNode) node.functionType, true);
        }
        for (BlockItemNode item : node.body.blockItems) {
            item.accept(this);
        }
        exitScope();
        return null;
    }

    @Override
    public Void visit(ReturnNode node) {
        node.expression.accept(this);
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
        visitDeclarationLike(node.identifier, node.type);
        if (node.type instanceof FunctionTypeNode funcType) {
            visitFunctionTypeNode(funcType, false);
        }
        if (node.initializer != null) {
            node.initializer.accept(this);
        }
        return null;
    }

    private void visitDeclarationLike(IdentifierNode id, TypeNode type) {
        Linkage linkage = type instanceof FunctionTypeNode ? Linkage.EXTERNAL : Linkage.NONE;
        String name = id.id;
        IdentifierEntry renamed = definitionInCurrentScope(name);
        if (renamed != null) {
            if ((linkage != Linkage.EXTERNAL || renamed.linkage != Linkage.EXTERNAL)) {
                error();
                String msg = "redefinition of '" + getLogger().white(name) + "'";
                logErrorWithSourceLine(id.wholeLocation, msg);
                msg = "previous definition of '" + getLogger().white(name) + "' with type '" +
                      getLogger().white(renamed.t.getType().toString()) + "'";
                logNoteWithSourceLine(renamed.id.wholeLocation, msg);
            }
        } else {
            if (linkage == Linkage.NONE) {
                id.id = makeUniqueName(name);
            }
            define(name, new IdentifierEntry(id, type, linkage));
        }
    }

    @Override
    public Void visit(ExpressionStatementNode node) {
        node.expression.accept(this);
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
            logErrorWithSourceLine(node.wholeLocation, msg);
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
            logErrorWithSourceLine(node.op.wholeLocation, msg);
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
            logErrorWithSourceLine(node.operatorLocation, msg);
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
        node.thenExpr.accept(this);
        node.elseExpr.accept(this);
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
        node.function.accept(this);
        for (ExpressionNode arg : node.arguments) {
            arg.accept(this);
        }
        return null;
    }
}
