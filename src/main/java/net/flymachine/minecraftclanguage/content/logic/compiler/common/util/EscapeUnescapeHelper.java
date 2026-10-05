package net.flymachine.minecraftclanguage.content.logic.compiler.common.util;

import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;

import java.io.ByteArrayOutputStream;

public final class EscapeUnescapeHelper {

    private final DiagnosticReporter reporter;

    public EscapeUnescapeHelper(DiagnosticReporter reporter) {
        this.reporter = reporter;
    }

    /**
     * 解析 C 字符串字面量内容（不含首尾双引号），返回字节序列
     * <p>
     * 字节序列按执行字符集存储
     */
    public byte[] unescapeBytes(String raw) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length());
        unescapeBytes(raw, out);
        return out.toByteArray();
    }

    public void unescapeBytes(String raw, ByteArrayOutputStream out) {
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c != '\\') {
                out.write(mapToExecutionCharSet(c));
                i++;
                continue;
            }
            i = processEscape(raw, i, out);
        }
    }

    /**
     * 处理一个转义序列，把结果字节写入 out，返回下一个未处理位置
     *
     * @param raw 原始内容
     * @param i   指向 '\\' 的位置
     * @return 下一个未处理位置
     */
    private int processEscape(String raw, int i, ByteArrayOutputStream out) {
        if (i + 1 >= raw.length()) {
            throw new IllegalArgumentException("trailing backslash");
        }
        char esc = raw.charAt(i + 1);
        switch (esc) {
            case '\'' -> {
                out.write('\'');
                return i + 2;
            }
            case '"' -> {
                out.write('"');
                return i + 2;
            }
            case '?' -> {
                out.write('?');
                return i + 2;
            }
            case '\\' -> {
                out.write('\\');
                return i + 2;
            }
            case 'a' -> {
                out.write(0x07);
                return i + 2;
            }
            case 'b' -> {
                out.write(0x08);
                return i + 2;
            }
            case 'f' -> {
                out.write(0x0C);
                return i + 2;
            }
            case 'n' -> {
                out.write(0x0A);
                return i + 2;
            }
            case 'r' -> {
                out.write(0x0D);
                return i + 2;
            }
            case 't' -> {
                out.write(0x09);
                return i + 2;
            }
            case 'v' -> {
                out.write(0x0B);
                return i + 2;
            }
            case 'x' -> {
                // 十六进制，至少一位，贪婪匹配
                int start = i + 2;
                int end = start;
                while (end < raw.length() && isHexDigit(raw.charAt(end))) {
                    end++;
                }
                if (end == start) {
                    throw new IllegalArgumentException("\\x used with no following hex digits");
                }
                while (start < end && raw.charAt(start) == '0') {
                    start++;
                }
                if (end - start > 2) {
                    throw new IllegalArgumentException(
                        "hex escape sequence '" + reporter.white("\\x" + raw.substring(i, end)) + "' out of range");
                }
                if (start == end) {
                    // 全是 0
                    out.write(0);
                    return end;
                }
                long value = Long.parseLong(raw.substring(start, end), 16);
                out.write((int) (value & 0xFF));
                return end;
            }
            default -> {
                if (esc >= '0' && esc <= '7') {
                    // 八进制，最多 3 位
                    int start = i + 1;
                    int end = start;
                    int bound = Math.min(raw.length(), start + 3);
                    while (end < bound && raw.charAt(end) >= '0' && raw.charAt(end) <= '7') {
                        end++;
                    }
                    int value = Integer.parseInt(raw.substring(start, end), 8);
                    if (value > 0xFF) {
                        throw new IllegalArgumentException(
                            "octal escape sequence '" + reporter.white("\\" + raw.substring(start, end)) +
                            "' out of range");
                    }
                    out.write(value & 0xFF);
                    return end;
                }
                throw new IllegalArgumentException("unknown escape sequence: '" + reporter.white("\\" + esc) + "'");
            }
        }
    }

    /**
     * 解析一个字符常量的内容（不含首尾单引号），返回 int 值
     */
    public int evalChar(String content) {
        if (content.isEmpty()) {
            throw new IllegalArgumentException("empty character constant");
        }

        // 解析单个 CChar
        int value;
        if (content.charAt(0) == '\\') {
            ByteArrayOutputStream out = new ByteArrayOutputStream(1);
            int next = processEscape(content, 0, out);
            if (next != content.length()) {
                throw new IllegalArgumentException("multi-character character constant");
            }
            value = out.toByteArray()[0] & 0xFF;
        } else {
            value = mapToExecutionCharSet(content.charAt(0));
            if (content.length() != 1) {
                throw new IllegalArgumentException("multi-character character constant");
            }
        }
        return value;
    }

    public static String escapeStringLiteral(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length + 2);
        sb.append('"');
        for (byte b : bytes) {
            int c = b & 0xFF;
            switch (c) {
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\\' -> sb.append("\\\\");
                case '\'' -> sb.append("\\'");
                case '"' -> sb.append("\\\"");
                default -> {
                    if (c >= 0x20 && c <= 0x7E) {
                        // 可打印 ASCII
                        sb.append((char) c);
                    } else {
                        // 非可打印字符，使用 \xHH 转义
                        sb.append(String.format("\\x%02X", c));
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9')
               || (c >= 'a' && c <= 'f')
               || (c >= 'A' && c <= 'F');
    }

    private static int mapToExecutionCharSet(char c) {
        if (c > 0xFF) {
            throw new IllegalArgumentException(
                "character out of range: U+" + Integer.toHexString(c));
        }
        return c & 0xFF;
    }
}
