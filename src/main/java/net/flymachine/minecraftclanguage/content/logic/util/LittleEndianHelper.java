package net.flymachine.minecraftclanguage.content.logic.util;

import java.io.OutputStream;

public final class LittleEndianHelper {
    private LittleEndianHelper() { }

    public static void writeByte(OutputStream out, byte value) {
        try {
            out.write(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeShort(OutputStream out, short value) {
        try {
            out.write(value & 0xFF);
            out.write((value >> 8) & 0xFF);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeInt(OutputStream out, int value) {
        try {
            out.write(value & 0xFF);
            out.write((value >> 8) & 0xFF);
            out.write((value >> 16) & 0xFF);
            out.write((value >> 24) & 0xFF);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeLong(OutputStream out, long value) {
        try {
            out.write((int) (value & 0xFF));
            out.write((int) ((value >> 8) & 0xFF));
            out.write((int) ((value >> 16) & 0xFF));
            out.write((int) ((value >> 24) & 0xFF));
            out.write((int) ((value >> 32) & 0xFF));
            out.write((int) ((value >> 40) & 0xFF));
            out.write((int) ((value >> 48) & 0xFF));
            out.write((int) ((value >> 56) & 0xFF));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
