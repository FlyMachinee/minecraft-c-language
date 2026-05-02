package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;

import java.util.HashMap;
import java.util.Map;

public final class VariableResolutionPass implements AstVisitor<Void> {

    public VariableResolutionPass() { }

    private int renameCounter = 0;

    private String makeUniqueName(String base) {
        return base + ".." + (renameCounter++);
    }

    private boolean semanticError = false;

    public boolean hasSemanticError() {
        return semanticError;
    }

    private final Map<String, String> variableRenamingMap = new HashMap<>();

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
    public Void visit(IntConstantNode node) {
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
        String name = node.identifier;
        if (variableRenamingMap.containsKey(name)) {
            semanticError = true;
            throw new RuntimeException("Duplicate variable name: " + name);
        }
        String uniqueName = makeUniqueName(name);
        variableRenamingMap.put(name, uniqueName);
        node.identifier = uniqueName;
        if (node.initializer != null) {
            node.initializer.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(NullStatementNode node) {
        return null;
    }

    @Override
    public Void visit(VariableNode node) {
        String name = node.identifier;
        if (!variableRenamingMap.containsKey(name)) {
            semanticError = true;
            throw new RuntimeException("Undefined variable: " + name);
        }
        node.identifier = variableRenamingMap.get(name);
        return null;
    }

    @Override
    public Void visit(AssignmentNode node) {
        if (!(node.lhs instanceof VariableNode)) {
            semanticError = true;
            throw new RuntimeException("Invalid lvalue: " + node.lhs.toString());
        }
        node.lhs.accept(this);
        node.rhs.accept(this);
        return null;
    }

    @Override
    public Void visit(IncrementDecrementNode node) {
        if (!(node.operand instanceof VariableNode)) {
            semanticError = true;
            throw new RuntimeException("Invalid lvalue: " + node.operand.toString());
        }
        node.operand.accept(this);
        return null;
    }
}
