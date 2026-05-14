package net.flymachine.minecraftclanguage.content.logic.assembler.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.LA64Register;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.LA64RegisterResolver;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.*;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LA64AssemblyParser {

    private static final Pattern LABEL_PATTERN = Pattern.compile("^([a-zA-Z_.][a-zA-Z0-9_.]*):\\s*(.*)$");
    private static final Pattern DIRECTIVE_PATTERN = Pattern.compile("^\\.([a-zA-Z][a-zA-Z0-9_]*)(?:\\s+(.*))?$");
    private static final Pattern INSTRUCTION_PATTERN = Pattern.compile("^([a-zA-Z][a-zA-Z0-9.]*)(?:\\s+(.*))?$");

    private final LA64RegisterResolver registerResolver = LA64RegisterResolver.getInstance();

    public LA64Assembly parse(String source, String fileName) {
        List<LA64AsmStatement> statements = new ArrayList<>();
        String[] lines = source.split("\\r?\\n");

        for (int lineNum = 1; lineNum <= lines.length; lineNum++) {
            String line = lines[lineNum - 1];
            // 去除注释（# 之后的内容）
            int commentIdx = line.indexOf('#');
            if (commentIdx != -1) {
                line = line.substring(0, commentIdx);
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            // 分离标签和剩余部分
            Matcher labelMatcher = LABEL_PATTERN.matcher(line);
            String rest = line;
            if (labelMatcher.matches()) {
                String labelName = labelMatcher.group(1);
                rest = labelMatcher.group(2).trim();
                statements.add(new LA64AsmLabel(labelName));
                if (rest.isEmpty()) {
                    continue;
                }
            }

            // 处理剩余部分（伪指令或普通指令）
            if (rest.startsWith(".")) {
                Matcher dirMatcher = DIRECTIVE_PATTERN.matcher(rest);
                if (!dirMatcher.matches()) {
                    throw new IllegalArgumentException(
                        String.format("Invalid directive at line %d: %s", lineNum, rest));
                }
                String dirName = dirMatcher.group(1);
                String argsPart = dirMatcher.group(2);
                List<LA64DirectiveArgument> args = parseDirectiveArgs(argsPart);
                statements.add(new LA64AsmDirective(dirName, args));
            } else {
                Matcher insMatcher = INSTRUCTION_PATTERN.matcher(rest);
                if (!insMatcher.matches()) {
                    throw new IllegalArgumentException(
                        String.format("Invalid instruction at line %d: %s", lineNum, rest));
                }
                String mnemonic = insMatcher.group(1);
                String opsPart = insMatcher.group(2);
                List<LA64AsmOperand> operands = parseInstructionOperands(opsPart);
                statements.add(new LA64AsmInstruction(mnemonic, operands));
            }
        }

        return new LA64Assembly(statements, fileName);
    }

    private List<LA64DirectiveArgument> parseDirectiveArgs(String argsPart) {
        if (argsPart == null || argsPart.isBlank()) {
            return List.of();
        }
        List<LA64DirectiveArgument> args = new ArrayList<>();
        String[] parts = splitArguments(argsPart);
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            if (containsWhitespace(part)) {
                throw new IllegalArgumentException("Directive argument contains whitespace: " + part);
            }
            if (isNumber(part)) {
                long value = parseNumber(part);
                args.add(new LA64DirectiveNumArg(value));
            } else {
                args.add(new LA64DirectiveSymArg(part));
            }
        }
        return args;
    }

    private List<LA64AsmOperand> parseInstructionOperands(String opsPart) {
        if (opsPart == null || opsPart.isBlank()) {
            return List.of();
        }
        List<LA64AsmOperand> operands = new ArrayList<>();
        String[] parts = splitArguments(opsPart);
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            if (containsWhitespace(part)) {
                throw new IllegalArgumentException("Instruction operand contains whitespace: " + part);
            }
            if (registerResolver.isRegisterName(part)) {
                LA64Register reg = registerResolver.findRegister(part).orElseThrow();
                operands.add(reg);
                continue;
            }
            if (isNumber(part)) {
                long value = parseNumber(part);
                operands.add(new LA64AsmImmOperand(value));
                continue;
            }
            operands.add(new LA64AsmSymOperand(part));
        }
        return operands;
    }

    private String[] splitArguments(String args) {
        if (args == null || args.isEmpty()) {
            return new String[0];
        }
        // 逗号分割，分割后每个部分可能含有前后空白，由调用者 trim
        return args.split(",");
    }

    private boolean containsWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean isNumber(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        if (s.startsWith("0x") || s.startsWith("0X")) {
            String hex = s.substring(2);
            if (hex.isEmpty()) {
                return false;
            }
            return hex.matches("[0-9a-fA-F]+");
        }
        String tmp = s;
        if (tmp.startsWith("-")) {
            if (tmp.length() == 1) {
                return false;
            }
            tmp = tmp.substring(1);
        }
        return tmp.matches("\\d+");
    }

    private long parseNumber(String s) {
        if (s.startsWith("0x") || s.startsWith("0X")) {
            return Long.parseUnsignedLong(s.substring(2), 16);
        } else {
            return Long.parseLong(s);
        }
    }
}
