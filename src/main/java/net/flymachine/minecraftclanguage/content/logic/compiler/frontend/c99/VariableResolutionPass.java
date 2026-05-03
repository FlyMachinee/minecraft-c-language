package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logger.Logger;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.ErrorHandleUtil;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceFile;

import java.util.HashMap;
import java.util.Map;

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

    private final Map<String, IdentifierNode> variableRenamingMap = new HashMap<>();

    @Override
    public Void visit(ProgramNode node) {
        visit(node.functionDefinition);
        return null;
    }

    @Override
    public Void visit(FunctionDefinitionNode node) {
        for (BlockItemNode blockItem : node.body) {
            blockItem.accept(this);
        }
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
        IdentifierNode renamed = variableRenamingMap.get(name);
        if (renamed != null) {
            semanticError = true;
            String msg = "redefinition of '" + logger.formatWithColor(name, Logger.Color.WHITE) + "'";
            ErrorHandleUtil.logErrorWithSourceLine(logger, sourceFile, node.variable.wholeLocation, msg);
            msg = "previous definition of '" + logger.formatWithColor(name, Logger.Color.WHITE) + "'";
            ErrorHandleUtil.logNoteWithSourceLine(logger, sourceFile, renamed.wholeLocation, msg);
        } else {
            node.variable.id = makeUniqueName(name);
            variableRenamingMap.put(name, node.variable);
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
    public Void visit(IdentifierNode node) {
        String name = node.id;
        IdentifierNode renamed = variableRenamingMap.get(name);
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
}
