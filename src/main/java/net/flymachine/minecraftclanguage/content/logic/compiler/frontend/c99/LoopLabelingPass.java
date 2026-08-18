package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;

import java.util.Stack;

/**
 * 为每个循环语句生成一个唯一的标签，并将 break 和 continue 语句与最近的循环/switch关联起来
 * <p>
 * 要求先进行 {@link LabelResolutionPass}
 */
public final class LoopLabelingPass extends SemanticAnalysePass implements AstVisitor<Void> {

    public LoopLabelingPass() {
        super(new ConsoleLogger());
    }

    // private final Stack<String> loopLabelStack = new Stack<>();
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

    @Override
    public Void visit(ProgramNode node) {
        for (ExternalDeclarationNode externalDeclaration : node.extDecls) {
            externalDeclaration.accept(this);
        }
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
    public Void visit(VariableNode node) {
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
        String currentLoopLabel = getCurrentBreakContextLabel();
        if (currentLoopLabel != null) {
            node.loopOrSwitchLabel = currentLoopLabel;
        } else {
            error();
            String msg = "break statement not within loop or switch";
            logErrorWithSourceLine(node.wholeLoc, msg);
        }
        return null;
    }

    @Override
    public Void visit(ContinueNode node) {
        String currentLoopLabel = getCurrentContinueContextLabel();
        if (currentLoopLabel != null) {
            node.loopLabel = currentLoopLabel;
        } else {
            error();
            String msg = "continue statement not within a loop";
            logErrorWithSourceLine(node.wholeLoc, msg);
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
    public Void visit(SwitchStatementNode node) {
        enterSwitch(node);
        node.body.accept(this);
        exitSwitch(node);
        return null;
    }

    @Override
    public Void visit(FunctionCallNode node) {
        return null;
    }

    @Override
    public Void visit(ConstantNode node) {
        return null;
    }

    @Override
    public Void visit(CastExpressionNode node) {
        return null;
    }

    @Override
    public Void visit(AddressOfNode node) {
        return null;
    }

    @Override
    public Void visit(DereferenceNode node) {
        return null;
    }
}
