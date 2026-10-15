package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.FunctionType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.IdentifierNode;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.util.List;
import java.util.stream.Collectors;

public final class FunctionTypeNode extends TypeNode {
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
    public FunctionType getType() {
        return new FunctionType(retType.getType(), paramTypes.stream().map(TypeNode::getType).toList());
    }

    @Override
    public boolean containsAlias() {
        return retType.containsAlias() || paramTypes.stream().anyMatch(TypeNode::containsAlias);
    }

    @Override
    public FunctionTypeNode unwrap() {
        return new FunctionTypeNode(
            wholeLoc, retType.unwrap(), paramTypes.stream().map(TypeNode::unwrap).toList(), params);
    }

    @Override
    protected String format(String declarator) {
        String params = paramTypes.stream()
                                  .map(t -> t.format(""))
                                  .collect(Collectors.joining(", "));
        String newDecl = Type.wrapIfPointer(declarator) + "(" + params + ")";
        return retType.format(newDecl);
    }

    public boolean hasNoParameters() {
        if (paramTypes.isEmpty()) { return true; }
        return paramTypes.size() == 1 && paramTypes.get(0) instanceof VoidTypeNode && params.get(0) == null;
    }
}
