package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.StatementVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;

import java.util.Stack;

/**
 * 为每个循环语句生成一个唯一的标签，并将 break 和 continue 语句与最近的循环/switch关联起来
 * <p>
 * 要求先进行 {@link LabelResolutionPass}
 */
public final class LoopLabelingPass implements StatementVisitor {

    private final DiagnosticReporter reporter;

    public LoopLabelingPass(DiagnosticReporter reporter) {
        this.reporter = reporter;
    }

    private final Stack<String> breakContextStack = new Stack<>();
    private final Stack<String> continueContextStack = new Stack<>();
    private int forLoopCounter = 0;
    private int whileLoopCounter = 0;
    private int doWhileLoopCounter = 0;

    private String enterForLoop() {
        String loopLabel = "for_loop_" + forLoopCounter++;
        breakContextStack.push(loopLabel);
        continueContextStack.push(loopLabel);
        return loopLabel;
    }

    private String enterWhileLoop() {
        String loopLabel = "while_loop_" + whileLoopCounter++;
        breakContextStack.push(loopLabel);
        continueContextStack.push(loopLabel);
        return loopLabel;
    }

    private String enterDoWhileLoop() {
        String loopLabel = "do_while_loop_" + doWhileLoopCounter++;
        breakContextStack.push(loopLabel);
        continueContextStack.push(loopLabel);
        return loopLabel;
    }

    private void exitLoop() {
        breakContextStack.pop();
        continueContextStack.pop();
    }

    private void enterSwitch(SwitchStatementNode node) {
        breakContextStack.push(node.switchLabel);
    }

    private void exitSwitch(SwitchStatementNode node) {
        breakContextStack.pop();
    }

    private String getCurrentBreakContextLabel() {
        if (breakContextStack.empty()) {
            return null;
        } else {
            return breakContextStack.peek();
        }
    }

    private String getCurrentContinueContextLabel() {
        if (continueContextStack.empty()) {
            return null;
        } else {
            return continueContextStack.peek();
        }
    }

    public void visit(ProgramNode node) {
        for (ExternalDeclarationNode externalDeclaration : node.extDecls) {
            if (externalDeclaration instanceof FunctionDefinitionNode funcDef) {
                visit(funcDef.body);
            }
        }
    }

    @Override
    public void visit(ReturnNode node) { }

    @Override
    public void visit(ExpressionStatementNode node) { }

    @Override
    public void visit(NullStatementNode node) { }

    @Override
    public void visit(IfStatementNode node) {
        node.thenStmt.accept(this);
        if (node.elseStmt != null) {
            node.elseStmt.accept(this);
        }
    }

    @Override
    public void visit(GotoNode node) { }

    @Override
    public void visit(CompoundStatementNode node) {
        for (BlockItemNode item : node.blockItems) {
            if (item instanceof StatementNode statementNode) {
                statementNode.accept(this);
            }
        }
    }

    @Override
    public void visit(BreakNode node) {
        String currentLoopLabel = getCurrentBreakContextLabel();
        if (currentLoopLabel != null) {
            node.loopOrSwitchLabel = currentLoopLabel;
        } else {
            reporter.error(node.wholeLoc, "break statement not within loop or switch");
        }
    }

    @Override
    public void visit(ContinueNode node) {
        String currentLoopLabel = getCurrentContinueContextLabel();
        if (currentLoopLabel != null) {
            node.loopLabel = currentLoopLabel;
        } else {
            reporter.error(node.wholeLoc, "continue statement not within a loop");
        }
    }

    @Override
    public void visit(WhileLoopNode node) {
        if (node.isDoWhile) {
            node.loopLabel = enterDoWhileLoop();
        } else {
            node.loopLabel = enterWhileLoop();
        }
        node.body.accept(this);
        exitLoop();
    }

    @Override
    public void visit(ForLoopNode node) {
        node.loopLabel = enterForLoop();
        node.body.accept(this);
        exitLoop();
    }

    @Override
    public void visit(SwitchStatementNode node) {
        enterSwitch(node);
        node.body.accept(this);
        exitSwitch(node);
    }
}
