package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public interface ConstantEvalVisitor {
    Either<Constant, SourceLocation> visit(ConstantNode node, ConstantCategory category);

    Either<Constant, SourceLocation> visit(VariableNode node, ConstantCategory category);

    Either<Constant, SourceLocation> visit(UnaryExpressionNode node, ConstantCategory category);

    Either<Constant, SourceLocation> visit(BinaryExpressionNode node, ConstantCategory category);

    Either<Constant, SourceLocation> visit(CastExpressionNode node, ConstantCategory category);

    Either<Constant, SourceLocation> visit(ConditionalExpressionNode node, ConstantCategory category);

    Either<Constant, SourceLocation> visit(AddressOfNode node, ConstantCategory category);

    Either<Constant, SourceLocation> visit(DereferenceNode node, ConstantCategory category);

    Either<Constant, SourceLocation> visit(SubscriptNode node, ConstantCategory category);

    enum ConstantCategory {
        INTEGER, ARITHMETIC
    }
}
