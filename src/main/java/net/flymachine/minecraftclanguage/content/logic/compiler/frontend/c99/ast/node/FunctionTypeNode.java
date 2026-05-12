package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.FunctionType;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.util.List;

public final class FunctionTypeNode extends AstNode implements TypeNode {
    public TypeNode returnType;
    public List<TypeNode> parameterTypes;
    public List<IdentifierNode> parameters;

    public FunctionTypeNode(
        SourceLocation wholeLocation, TypeNode returnType, List<TypeNode> parameterTypes,
        List<IdentifierNode> parameters) {

        super(wholeLocation);
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.parameters = parameters;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        stringBuilder.append(getType());
    }

    @Override
    public FunctionType getType() {
        return new FunctionType(returnType.getType(), parameterTypes.stream().map(TypeNode::getType).toList());
    }

    public boolean hasNoParameters() {
        if (parameterTypes.isEmpty()) { return true; }
        return parameterTypes.size() == 1 && parameterTypes.get(0) instanceof BasicTypeNode basicType &&
               basicType.getType().getKind() == BasicType.Kind.VOID;
    }
}
