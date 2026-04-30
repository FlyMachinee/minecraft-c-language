package net.flymachine.minecraftclanguage.content.logic.architecture.la64.util;

public final class BitMath {

    private BitMath() { }

    public static int extractBits(int data, int lowBit, int length) {
        // return (data << (32 - lowBit - length)) >>> (32 - length);
        return (data >>> lowBit) & ((1 << length) - 1);
    }

    public static int extractBits(int data, int length) {
        // return (data << (32 - lowBit - length)) >>> (32 - length);
        return data & ((1 << length) - 1);
    }

    public static int extractSignedBits(int data, int lowBit, int length) {
        return (data << (32 - lowBit - length)) >> (32 - length);
    }

    public static int extractSignedBits(int data, int length) {
        return (data << (32 - length)) >> (32 - length);
    }


    public static int getRd(int machineCode) {
        return extractBits(machineCode, 5);
    }

    public static int getRj(int machineCode) {
        return extractBits(machineCode, 5, 5);
    }

    public static int getRk(int machineCode) {
        return extractBits(machineCode, 10, 5);
    }

    public static int getSi12(int machineCode) {
        return extractSignedBits(machineCode, 10, 12);
    }

    public static int getOffs16(int machineCode) {
        return extractSignedBits(machineCode, 10, 16);
    }

    public static boolean isSi12(int number) {
        // 检查该数字是否可被 12 位的有符号数表示
        return number >= -2048 && number <= 2047;
    }

    public static boolean isUi12(int number) {
        // 检查该数字是否可被 12 位的无符号数表示
        return (number & 0xFFFFF000) == 0;
    }
}
