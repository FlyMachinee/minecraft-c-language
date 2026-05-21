package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.StorageClassSpecifier;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public class StorageClassSpecifierNode extends AstNode {
    public StorageClassSpecifier storageClass;

    public StorageClassSpecifierNode(SourceLocation wholeLocation, StorageClassSpecifier storageClass) {
        super(wholeLocation);
        this.storageClass = storageClass;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        stream.print(storageClass);
    }
}
