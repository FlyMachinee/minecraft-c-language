package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

public sealed interface StaticInit permits CharInit, DoubleInit, IntInit, LongInit, StringInit, SymbolInit, UnsignedCharInit, UnsignedIntInit, UnsignedLongInit, ZeroInit {
    long toByteRepresentation();
}
