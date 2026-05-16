package net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand;

public sealed interface LA64DirectiveArgument permits LA64DirectiveNumArg, LA64DirectiveSymArg {
    default boolean isNum() {
        return this instanceof LA64DirectiveNumArg;
    }

    default boolean isSym() {
        return this instanceof LA64DirectiveSymArg;
    }

    default long asNum() {
        return ((LA64DirectiveNumArg) this).value();
    }

    default String asSym() {
        return ((LA64DirectiveSymArg) this).name();
    }
}
