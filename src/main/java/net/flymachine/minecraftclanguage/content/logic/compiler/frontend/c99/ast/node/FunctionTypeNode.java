package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.FunctionType;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;
import java.util.List;

public final class FunctionTypeNode extends AstNode implements TypeNode {
    public TypeNode retType;
    public List<TypeNode> paramTypes;
    public List<IdentifierNode> params;

    public FunctionTypeNode(
        SourceLocation wholeLocation, TypeNode retType, List<TypeNode> paramTypes,
        List<IdentifierNode> params) {

        super(wholeLocation);
        this.retType = retType;
        this.paramTypes = paramTypes;
        this.params = params;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        stream.print(getType());
    }

    @Override
    public FunctionType getType() {
        return new FunctionType(retType.getType(), paramTypes.stream().map(TypeNode::getType).toList());
    }

    public boolean hasNoParameters() {
        if (paramTypes.isEmpty()) { return true; }
        return paramTypes.size() == 1 && paramTypes.get(0) instanceof BasicTypeNode basicType &&
               basicType.getType() == BasicType.VOID && params.get(0) == null;
    }
}
