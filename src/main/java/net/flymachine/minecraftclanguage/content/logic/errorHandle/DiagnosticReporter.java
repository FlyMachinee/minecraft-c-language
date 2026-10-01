package net.flymachine.minecraftclanguage.content.logic.errorHandle;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logger.Logger;

public class DiagnosticReporter {
    private final Logger logger;
    private final SourceFile sourceFile;

    private int errorCount = 0;
    private int warningCount = 0;

    private SourceLocation lastLocation = null;

    private boolean suppressDiagnostics = false;

    public DiagnosticReporter(Logger logger, SourceFile sourceFile) {
        this.logger = logger;
        this.sourceFile = sourceFile;
    }

    public DiagnosticReporter(SourceFile sourceFile) {
        this(new ConsoleLogger(), sourceFile);
    }

    public void error(SourceLocation loc, String msg) {
        ++errorCount;
        ErrorHandleUtil.logErrorWithSourceLine(logger, sourceFile, loc, msg);
        lastLocation = loc;
    }

    public void error(int line, int charPosition, int len, String msg) {
        ++errorCount;
        ErrorHandleUtil.logErrorWithSourceLine(logger, sourceFile, line, charPosition, len, msg);
    }

    public void warning(SourceLocation loc, String msg) {
        ++warningCount;
        ErrorHandleUtil.logWarningWithSourceLine(logger, sourceFile, loc, msg);
        lastLocation = loc;
    }

    public void note(SourceLocation loc, String msg) {
        ErrorHandleUtil.logNoteWithSourceLine(logger, sourceFile, loc, msg);
        lastLocation = loc;
    }

    public void note(String msg) {
        if (lastLocation != null) {
            ErrorHandleUtil.logNote(logger, sourceFile, lastLocation, msg);
        }
    }

    public void suppressDiagnostics(boolean suppress) {
        this.suppressDiagnostics = suppress;
    }

    public void suppressDiagnostics() {
        suppressDiagnostics(true);
    }

    public void unsuppressDiagnostics() {
        suppressDiagnostics(false);
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public String white(String msg) {
        return logger.white(msg);
    }

    public String white(SourceLocation loc) {
        return logger.white(byLocation(loc));
    }

    public String byLocation(SourceLocation loc) {
        return sourceFile.getByLocation(loc);
    }
}
