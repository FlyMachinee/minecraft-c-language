package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logger.Logger;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.ErrorHandleUtil;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceFile;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public class SemanticAnalysePass {

    private Logger logger;
    private SourceFile sourceFile;
    private boolean semanticError = false;

    protected SemanticAnalysePass(Logger logger, SourceFile sourceFile) {
        this.logger = logger;
        this.sourceFile = sourceFile;
    }

    protected SemanticAnalysePass(Logger logger) {
        this.logger = logger;
    }

    protected SemanticAnalysePass() {
        this.logger = new ConsoleLogger();
    }

    public boolean hasSemanticError() {
        return semanticError;
    }

    public void setSemanticError(boolean semanticError) {
        this.semanticError = semanticError;
    }

    protected void error() {
        semanticError = true;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public SourceFile getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
    }

    protected void logErrorWithSourceLine(SourceLocation sourceLocation, String message) {
        ErrorHandleUtil.logErrorWithSourceLine(getLogger(), getSourceFile(), sourceLocation, message);
    }

    protected void logNoteWithSourceLine(SourceLocation sourceLocation, String message) {
        ErrorHandleUtil.logNoteWithSourceLine(getLogger(), getSourceFile(), sourceLocation, message);
    }

    protected void logWarningWithSourceLine(SourceLocation sourceLocation, String message) {
        ErrorHandleUtil.logWarningWithSourceLine(getLogger(), getSourceFile(), sourceLocation, message);
    }
}
