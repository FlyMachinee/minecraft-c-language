package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logger.Logger;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.ErrorHandleUtil;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceFile;

import java.util.Stack;

public final class LoopLabelingPass implements AstVisitor<Void>, SemanticAnalysePass {

    private Logger logger;
    private SourceFile sourceFile;
    private boolean semanticError = false;

    public LoopLabelingPass() {
        this.logger = new ConsoleLogger();
    }

    private final Stack<String> loopLabelStack = new Stack<>();
    private int forLoopCounter = 0;
    private int whileLoopCounter = 0;
    private int doWhileLoopCounter = 0;

    private String enterForLoop() {
        String loopLabel = "for_loop_" + forLoopCounter++;
        return loopLabelStack.push(loopLabel);
    }

    private String enterWhileLoop() {
        String loopLabel = "while_loop_" + whileLoopCounter++;
        return loopLabelStack.push(loopLabel);
    }

    private String enterDoWhileLoop() {
        String loopLabel = "do_while_loop_" + doWhileLoopCounter++;
        return loopLabelStack.push(loopLabel);
    }

    private void exitLoop() {
        loopLabelStack.pop();
    }

    private String getCurrentLoopLabel() {
        if (loopLabelStack.empty()) {
            return null;
        } else {
            return loopLabelStack.peek();
        }
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
        return null;
    }

    @Override
    public Void visit(UnaryExpressionNode node) {
        return null;
    }

    @Override
    public Void visit(BinaryExpressionNode node) {
        return null;
    }

    @Override
    public Void visit(DeclarationNode node) {
        return null;
    }

    @Override
    public Void visit(ExpressionStatementNode node) {
        return null;
    }

    @Override
    public Void visit(NullStatementNode node) {
        return null;
    }

    @Override
    public Void visit(IdentifierNode node) {
        return null;
    }

    @Override
    public Void visit(AssignmentNode node) {
        return null;
    }

    @Override
    public Void visit(IncrementDecrementNode node) {
        return null;
    }

    @Override
    public Void visit(IfStatementNode node) {
        node.thenStmt.accept(this);
        if (node.elseStmt != null) {
            node.elseStmt.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ConditionalExpressionNode node) {
        return null;
    }

    @Override
    public Void visit(GotoNode node) {
        return null;
    }

    @Override
    public Void visit(CompoundStatementNode node) {
        for (BlockItemNode item : node.blockItems) {
            item.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(BreakNode node) {
        String currentLoopLabel = getCurrentLoopLabel();
        if (currentLoopLabel != null) {
            node.loopLabel = currentLoopLabel;
        } else {
            semanticError = true;
            String msg = "break statement not within loop or switch";
            ErrorHandleUtil.logErrorWithSourceLine(logger, sourceFile, node.wholeLocation, msg);
        }
        return null;
    }

    @Override
    public Void visit(ContinueNode node) {
        String currentLoopLabel = getCurrentLoopLabel();
        if (currentLoopLabel != null) {
            node.loopLabel = currentLoopLabel;
        } else {
            semanticError = true;
            String msg = "continue statement not within a loop";
            ErrorHandleUtil.logErrorWithSourceLine(logger, sourceFile, node.wholeLocation, msg);
        }
        return null;
    }

    @Override
    public Void visit(WhileLoopNode node) {
        if (node.isDoWhile) {
            node.loopLabel = enterDoWhileLoop();
        } else {
            node.loopLabel = enterWhileLoop();
        }
        node.body.accept(this);
        exitLoop();
        return null;
    }

    @Override
    public Void visit(ForLoopNode node) {
        node.loopLabel = enterForLoop();
        node.body.accept(this);
        exitLoop();
        return null;
    }

    @Override
    public boolean hasSemanticError() {
        return semanticError;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public SourceFile getSourceFile() {
        return sourceFile;
    }

    @Override
    public void setSourceFile(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
    }
}
