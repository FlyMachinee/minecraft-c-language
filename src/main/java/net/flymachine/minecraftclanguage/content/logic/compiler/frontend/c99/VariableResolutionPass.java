package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logger.Logger;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.ErrorHandleUtil;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * 将变量的名字替换为唯一的名字，并检查变量的重复定义和未定义使用
 */
public final class VariableResolutionPass implements AstVisitor<Void> {

    private Logger logger;
    private SourceFile sourceFile;

    public VariableResolutionPass() {
        this.logger = new ConsoleLogger();
    }

    private int renameCounter = 0;

    private String makeUniqueName(String base) {
        return base + ".." + (renameCounter++);
    }

    private boolean semanticError = false;

    public boolean hasSemanticError() {
        return semanticError;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public SourceFile getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
    }

    private final Stack<Map<String, IdentifierNode>> scopeStack = new Stack<>();

    private void enterScope() {
        scopeStack.push(new HashMap<>());
    }

    private void exitScope() {
        scopeStack.pop();
    }

    private boolean definedInCurrentScope(String identifier) {
        return scopeStack.peek().containsKey(identifier);
    }

    private boolean definedInScope(String identifier, Map<String, IdentifierNode> scope) {
        return scope.containsKey(identifier);
    }

    private IdentifierNode definitionInCurrentScope(String identifier) {
        return scopeStack.peek().get(identifier);
    }

    private IdentifierNode definitionOf(String identifier) {
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Map<String, IdentifierNode> scope = scopeStack.get(i);
            if (definedInScope(identifier, scope)) {
                return scope.get(identifier);
            }
        }
        return null;
    }

    private void define(String identifier, IdentifierNode definition) {
        scopeStack.peek().put(identifier, definition);
    }

    @Override
    public Void visit(ProgramNode node) {
        visit(node.functionDefinition);
        return null;
    }

    @Override
    public Void visit(FunctionDefinitionNode node) {
        visit(node.body);
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
        String name = node.variable.id;
        IdentifierNode renamed = definitionInCurrentScope(name);
        if (renamed != null) {
            semanticError = true;
            String msg = "redefinition of '" + logger.formatWithColor(name, Logger.Color.WHITE) + "'";
            ErrorHandleUtil.logErrorWithSourceLine(logger, sourceFile, node.variable.wholeLocation, msg);
            msg = "previous definition of '" + logger.formatWithColor(name, Logger.Color.WHITE) + "'";
            ErrorHandleUtil.logNoteWithSourceLine(logger, sourceFile, renamed.wholeLocation, msg);
        } else {
            node.variable.id = makeUniqueName(name);
            define(name, node.variable);
        }
        if (node.initializer != null) {
            node.initializer.accept(this);
        }
        return null;
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
        IdentifierNode renamed = definitionOf(name);
        if (renamed == null) {
            semanticError = true;
            String msg = "'" + logger.formatWithColor(name, Logger.Color.WHITE) + "' undeclared";
            ErrorHandleUtil.logErrorWithSourceLine(logger, sourceFile, node.wholeLocation, msg);
        } else {
            node.id = renamed.id;
        }
        return null;
    }

    @Override
    public Void visit(AssignmentNode node) {
        if (!(node.lhs instanceof IdentifierNode)) {
            semanticError = true;
            String msg = "lvalue required as left operand of assignment";
            ErrorHandleUtil.logErrorWithSourceLine(logger, sourceFile, node.op.wholeLocation, msg);
        }
        node.lhs.accept(this);
        node.rhs.accept(this);
        return null;
    }

    @Override
    public Void visit(IncrementDecrementNode node) {
        if (!(node.operand instanceof IdentifierNode)) {
            semanticError = true;
            String msg;
            if (node.isIncrement) {
                msg = "lvalue required as increment operand";
            } else {
                msg = "lvalue required as decrement operand";
            }
            ErrorHandleUtil.logErrorWithSourceLine(logger, sourceFile, node.operatorLocation, msg);
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
}
