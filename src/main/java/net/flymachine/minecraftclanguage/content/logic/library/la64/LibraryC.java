package net.flymachine.minecraftclanguage.content.logic.library.la64;

import net.flymachine.minecraftclanguage.content.logic.assembler.la64.LA64Assembler;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.LA64AssemblyParser;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64Assembly;
import net.flymachine.minecraftclanguage.content.logic.device.la64.Teletypewriter;
import net.flymachine.minecraftclanguage.content.logic.object.la64.LA64Object;

public final class LibraryC {

    private LibraryC() { }

    public static final LA64Object LIB_C;

    static {
        LA64AssemblyParser parser = new LA64AssemblyParser();
        LA64Assembler assembler = new LA64Assembler();

        String libcCode = """
                                .global putchar
                            putchar:
                                # argument in a0
                                # write in tty address
                                li.d t0, %#X
                                st.w a0, t0, 0
                                ret
                          """;
        libcCode = String.format(libcCode, Teletypewriter.BASE_ADDRESS);

        LA64Assembly libcAssembly = parser.parse(libcCode, "libc.s");
        LIB_C = assembler.assemble(libcAssembly);
    }
}
