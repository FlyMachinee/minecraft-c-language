package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.Logger;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceFile;

public interface SemanticAnalysePass {
    boolean hasSemanticError();

    Logger getLogger();

    void setLogger(Logger logger);

    SourceFile getSourceFile();

    void setSourceFile(SourceFile sourceFile);
}
