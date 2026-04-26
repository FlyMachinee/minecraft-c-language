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

    private TacFunction lowerFunction(FunctionDefinitionNode functionDefinition) {
        List<TacInstruction> instructions = lowerStatement(functionDefinition.body);
        return new TacFunction(functionDefinition.name, instructions);
    }

    private List<TacInstruction> lowerStatement(StatementNode statement) {
        List<TacInstruction> instructions = new ArrayList<>();
        if (statement instanceof ReturnNode returnNode) {
            TacValue returnValue = lowerExpression(returnNode.expression);
            instructions.add(new TacReturn(returnValue));
        } else {
            throw new UnsupportedOperationException(
                "Unsupported statement type: " + statement.getClass().getSimpleName());
        }
        return instructions;
    }

    private TacValue lowerExpression(ExpressionNode expression) {
        if (expression instanceof IntConstantNode intConstantNode) {
            return new TacIntConstant(intConstantNode.value);
        }
        throw new UnsupportedOperationException(
            "Unsupported expression type: " + expression.getClass().getSimpleName());
    }
}
