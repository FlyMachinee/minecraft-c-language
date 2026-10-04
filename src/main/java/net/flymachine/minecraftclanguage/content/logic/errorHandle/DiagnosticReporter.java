package net.flymachine.minecraftclanguage.content.logic.errorHandle;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logger.Logger;

import java.util.Stack;

public class DiagnosticReporter {
    private final Logger logger;
    private final SourceFile sourceFile;

    private int errorCount = 0;
    private int warningCount = 0;

    private SourceLocation lastLocation = null;

    private Stack<Boolean> suppressDiagnosticsStack = new Stack<>();

    public DiagnosticReporter(Logger logger, SourceFile sourceFile) {
        this.logger = logger;
        this.sourceFile = sourceFile;
    }

    public DiagnosticReporter(SourceFile sourceFile) {
        this(new ConsoleLogger(), sourceFile);
    }

    public void error(SourceLocation loc, String msg) {
        if (!suppressDiagnosticsStack.isEmpty() && suppressDiagnosticsStack.peek()) {
            return;
        }
        ++errorCount;
        ErrorHandleUtil.logErrorWithSourceLine(logger, sourceFile, loc, msg);
        lastLocation = loc;
    }

    public void error(int line, int charPosition, int len, String msg) {
        if (!suppressDiagnosticsStack.isEmpty() && suppressDiagnosticsStack.peek()) {
            return;
        }
        ++errorCount;
        ErrorHandleUtil.logErrorWithSourceLine(logger, sourceFile, line, charPosition, len, msg);
    }

    public void warning(SourceLocation loc, String msg) {
        if (!suppressDiagnosticsStack.isEmpty() && suppressDiagnosticsStack.peek()) {
            return;
        }
        ++warningCount;
        ErrorHandleUtil.logWarningWithSourceLine(logger, sourceFile, loc, msg);
        lastLocation = loc;
    }

    public void note(SourceLocation loc, String msg) {
        if (!suppressDiagnosticsStack.isEmpty() && suppressDiagnosticsStack.peek()) {
            return;
        }
        ErrorHandleUtil.logNoteWithSourceLine(logger, sourceFile, loc, msg);
        lastLocation = loc;
    }

    public void note(String msg) {
        if (!suppressDiagnosticsStack.isEmpty() && suppressDiagnosticsStack.peek()) {
            return;
        }
        if (lastLocation != null) {
            ErrorHandleUtil.logNote(logger, sourceFile, lastLocation, msg);
        }
    }

    public void suppressDiagnostics() {
        suppressDiagnosticsStack.push(true);
    }

    public void unsuppressDiagnostics() {
        suppressDiagnosticsStack.push(false);
    }

    public void clearSuppressDiagnostics() {
        if (!suppressDiagnosticsStack.isEmpty()) {
            suppressDiagnosticsStack.pop();
        }
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
