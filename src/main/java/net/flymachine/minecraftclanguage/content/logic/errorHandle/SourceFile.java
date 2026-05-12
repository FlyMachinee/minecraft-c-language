package net.flymachine.minecraftclanguage.content.logic.errorHandle;

import org.antlr.v4.runtime.CharStream;

public class SourceFile {
    private final String fileName;
    private final String[] lines;

    public SourceFile(String fileName, String[] lines) {
        this.fileName = fileName;
        this.lines = lines;
    }

    public SourceFile(String fileName, String content) {
        this.fileName = fileName;
        this.lines = content.split("\r?\n", -1);
    }

    public SourceFile(CharStream charStream) {
        this(charStream.getSourceName(), charStream.toString());
    }

    public String getFileName() {
        return fileName;
    }

    public String getLine(int line) {
        if (line < 1 || line > lines.length) {
            return null;
        }
        return lines[line - 1].replace("\r", "");
    }

    public String getByLocation(SourceLocation location) {
        if (location == null) {
            return null;
        }
        String line = getLine(location.line());
        int from = Math.min(location.column(), line.length());
        int len = Math.min(location.length(), line.length() - from);
        return line.substring(from, from + len);
    }
}
