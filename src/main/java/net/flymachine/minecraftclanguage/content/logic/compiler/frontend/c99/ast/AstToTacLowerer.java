package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.*;

import java.util.ArrayList;
import java.util.List;

public final class AstToTacLowerer {

    public AstToTacLowerer() { }

    public TacProgram lower(ProgramNode program) {
        TacFunction functionDefinition = lowerFunction(program.functionDefinition);
        return new TacProgram(functionDefinition);
    }

    private int tempVarCounter = 0;

    private String makeTempVar() {
        return "tmp." + (tempVarCounter++);
    }

    private TacFunction lowerFunction(FunctionDefinitionNode functionDefinition) {
        List<TacInstruction> instructions = lowerStatement(functionDefinition.body);
        return new TacFunction(functionDefinition.name, instructions);
    }

    private List<TacInstruction> lowerStatement(StatementNode statement) {
        List<TacInstruction> instructions = new ArrayList<>();
        if (statement instanceof ReturnNode returnNode) {
            TacValue returnValue = lowerExpression(returnNode.expression, instructions);
            instructions.add(new TacReturn(returnValue));
        } else {
            throw new UnsupportedOperationException(
                "Unsupported statement type: " + statement.getClass().getSimpleName());
        }
        return instructions;
    }

    private TacValue lowerExpression(ExpressionNode expression, List<TacInstruction> instructions) {
        if (expression instanceof IntConstantNode intConstantNode) {
            return new TacIntConstant(intConstantNode.value);
        } else if (expression instanceof UnaryExpressionNode unaryExpressionNode) {
            TacValue src = lowerExpression(unaryExpressionNode.exp, instructions);
            TacVariable dst = new TacVariable(makeTempVar());
            instructions.add(new TacUnaryOperation(unaryExpressionNode.op, src, dst));
            return dst;
        } else if (expression instanceof BinaryExpressionNode binaryExpressionNode) {
            TacValue lhs = lowerExpression(binaryExpressionNode.lhs, instructions);
            TacValue rhs = lowerExpression(binaryExpressionNode.rhs, instructions);
            TacVariable dst = new TacVariable(makeTempVar());
            instructions.add(new TacBinaryOperation(binaryExpressionNode.op, lhs, rhs, dst));
            return dst;
        }
        throw new UnsupportedOperationException(
            "Unsupported expression type: " + expression.getClass().getSimpleName());

    }
}
