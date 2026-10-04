package net.flymachine.minecraftclanguage.content.logic.emulator;

import java.security.SecureRandom;
import java.util.Random;

public final class AslrBaseGenerator {

    public static final long PAGE_SIZE = 0x1000L;
    public static final long PAGE_MASK = PAGE_SIZE - 1;

    public static final long BASE_MIN = 0x0000_0000_0040_0000L;
    public static final long BASE_MAX = 0x0000_0000_ffff_f000L;

    public static final int ASLR_BITS = 20;

    private final Random random;

    public AslrBaseGenerator() {
        this.random = new SecureRandom();
    }

    public AslrBaseGenerator(long seed) {
        this.random = new Random(seed);
    }

    public long nextBase() {
        long offset = random.nextLong() & ((1L << ASLR_BITS) - 1);
        long address = offset << 12;
        address += BASE_MIN;

        if (address > BASE_MAX) {
            address = BASE_MIN + ((address - BASE_MIN) % (BASE_MAX - BASE_MIN + 1));
            address &= ~PAGE_MASK;
        }

        return address;
    }

    public long nextBase(long moduleSize) {
        long modulePages = (moduleSize + PAGE_MASK) & ~PAGE_MASK;
        long maxStart = BASE_MAX - modulePages;
        if (maxStart < BASE_MIN) {
            throw new IllegalArgumentException("module too large: " + moduleSize);
        }

        long range = maxStart - BASE_MIN;
        long pageCount = (range >> 12) + 1;
        long randomPage = Math.floorMod(random.nextLong(), pageCount);
        return BASE_MIN + (randomPage << 12);
    }
}
