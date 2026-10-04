package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.constexprHelper;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantSymbolPointer;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.PointerConstant;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;

import java.util.Optional;

public final class LValuePathEvaluator implements ExpressionVisitor<Optional<LValuePath>> {

    private final SymbolTable symbolTable;
    private final ConstantEvaluator constantEvaluator;

    public LValuePathEvaluator(SymbolTable symbolTable, ConstantEvaluator constantEvaluator) {
        this.symbolTable = symbolTable;
        this.constantEvaluator = constantEvaluator;
    }

    public Optional<LValuePath> tryEvalPath(ExpressionNode exp) {
        if (exp.expType == null || exp.expType.isError()) {
            return Optional.empty();
        }
        return exp.accept(this);
    }

    public PointerConstant pathToAddress(LValuePath path) {
        if (path instanceof LValuePath.Root root) {
            return new ConstantSymbolPointer(root.symbol(), root.type());
        }
        if (path instanceof LValuePath.Dereference deref) {
            return deref.addr();
        }
        throw new IllegalStateException("Unknown LValuePath type: " + path.getClass().getName());
    }

    @Override
    public Optional<LValuePath> visit(VariableNode variable) {
        SymbolTable.Entry entry = symbolTable.get(variable.id.name);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.attr instanceof SymbolTable.Entry.StaticAttr || entry.attr instanceof SymbolTable.Entry.FuncAttr) {
            return Optional.of(new LValuePath.Root(variable.id.name, variable.expType));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<LValuePath> visit(DereferenceNode deref) {
        var addr = constantEvaluator.tryEvalAddressConstant(deref.exp);
        return addr.right().isPresent() ? Optional.empty() :
            Optional.of(new LValuePath.Dereference((PointerConstant) addr.orThrow()));
    }

    @Override
    public Optional<LValuePath> visit(SubscriptNode subscript) {
        // 注意在类型检查中已经进行了类型衰减
        // 指针[整数] 或 整数[指针]
        ExpressionNode pointer;
        ExpressionNode index;
        if (subscript.lhs.expType.isPointer()) {
            pointer = subscript.lhs;
            index = subscript.rhs;
        } else {
            pointer = subscript.rhs;
            index = subscript.lhs;
        }
        var ptrRes = constantEvaluator.tryEvalAddressConstant(pointer);
        if (ptrRes.right().isPresent()) {
            return Optional.empty();
        }
        var idxRes = constantEvaluator.tryEvalArithmeticConstant(index);
        if (idxRes.right().isPresent()) {
            return Optional.empty();
        }

        return Optional.of(
            new LValuePath.Dereference((PointerConstant) ptrRes.orThrow().apply(BinaryOperator.ADD, idxRes.orThrow())));
    }

    @Override
    public Optional<LValuePath> visit(ConstantNode constant) {
        return Optional.empty();
    }

    @Override
    public Optional<LValuePath> visit(UnaryExpressionNode unaryExp) {
        return Optional.empty();
    }

    @Override
    public Optional<LValuePath> visit(BinaryExpressionNode binaryExp) {
        return Optional.empty();
    }

    @Override
    public Optional<LValuePath> visit(AssignmentNode assignment) {
        return Optional.empty();
    }

    @Override
    public Optional<LValuePath> visit(IncrementDecrementNode incrementDecrement) {
        return Optional.empty();
    }

    @Override
    public Optional<LValuePath> visit(ConditionalExpressionNode condExp) {
        return Optional.empty();
    }

    @Override
    public Optional<LValuePath> visit(FunctionCallNode funcCall) {
        return Optional.empty();
    }

    @Override
    public Optional<LValuePath> visit(CastExpressionNode castExp) {
        return Optional.empty();
    }

    @Override
    public Optional<LValuePath> visit(AddressOfNode addrOf) {
        return Optional.empty();
    }

}
