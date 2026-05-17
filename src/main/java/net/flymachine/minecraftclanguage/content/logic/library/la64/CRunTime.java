package net.flymachine.minecraftclanguage.content.logic.library.la64;

import net.flymachine.minecraftclanguage.content.logic.assembler.la64.LA64Assembler;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.LA64AssemblyParser;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64Assembly;
import net.flymachine.minecraftclanguage.content.logic.object.la64.LA64Object;

public final class CRunTime {

    private CRunTime() { }

    public static final LA64Object CRT0;

    static {
        LA64AssemblyParser parser = new LA64AssemblyParser();
        LA64Assembler assembler = new LA64Assembler();

        String crt0Code = """
                                .global _start
                                .balign 4
                            _start:
                                # Initialize the stack pointer
                                la.abs sp, __stack_top
                          
                                # Call the main function
                                bl main
                          
                                # Exit the program (jump to virtual address 0)
                                jirl r0, r0, 0
                          """;

        LA64Assembly crt0Assembly = parser.parse(crt0Code, "crt0.s");
        CRT0 = assembler.assemble(crt0Assembly);
    }
}
