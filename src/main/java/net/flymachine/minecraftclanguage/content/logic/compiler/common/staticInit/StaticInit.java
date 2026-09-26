package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

public sealed interface StaticInit permits DoubleInit, IntInit, LongInit, UnsignedIntInit, UnsignedLongInit, ZeroInit {
    long toByteRepresentation();
}
