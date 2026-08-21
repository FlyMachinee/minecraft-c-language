package net.flymachine.minecraftclanguage.content.logic.errorHandle;

/**
 * @param line   1-based
 * @param column 0-based
 */
public record SourceLocation(int line, int column, int startIndex, int endIndex) {

    public int length() {
        return endIndex - startIndex + 1;
    }

    public static SourceLocation concat(SourceLocation a, SourceLocation b) {
        if (a == null) { return b; }
        if (b == null) { return a; }
        if (a.startIndex < b.startIndex) {
            return new SourceLocation(a.line, a.column, a.startIndex, Math.max(a.endIndex, b.endIndex));
        } else {
            return new SourceLocation(b.line, b.column, b.startIndex, Math.max(a.endIndex, b.endIndex));
        }
    }
}
