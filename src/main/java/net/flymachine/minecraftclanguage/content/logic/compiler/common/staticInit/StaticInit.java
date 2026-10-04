package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

public sealed interface StaticInit permits DoubleInit, IntInit, LongInit, SymbolInit, UnsignedIntInit, UnsignedLongInit, ZeroInit {
    long toByteRepresentation();
}
