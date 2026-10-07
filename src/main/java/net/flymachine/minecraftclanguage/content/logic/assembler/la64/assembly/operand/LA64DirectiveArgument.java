package net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand;

public sealed interface LA64DirectiveArgument permits LA64DirectiveNumArg, LA64DirectiveStrArg, LA64DirectiveSymArg {
    default boolean isNum() {
        return this instanceof LA64DirectiveNumArg;
    }

    default boolean isSym() {
        return this instanceof LA64DirectiveSymArg;
    }

    default long asNum() {
        return ((LA64DirectiveNumArg) this).value();
    }

    default LA64DirectiveSymArg asSym() {
        return (LA64DirectiveSymArg) this;
    }

    default boolean isStr() {
        return this instanceof LA64DirectiveStrArg;
    }

    default String asStr() {
        return ((LA64DirectiveStrArg) this).str();
    }
}
