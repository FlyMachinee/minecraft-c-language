package net.flymachine.minecraftclanguage.content.logic.library.la64;

import net.flymachine.minecraftclanguage.content.logic.assembler.la64.LA64Assembler;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64Assembly;
import net.flymachine.minecraftclanguage.content.logic.compiler.C99ToLA64Compiler;
import net.flymachine.minecraftclanguage.content.logic.device.la64.Teletypewriter;
import net.flymachine.minecraftclanguage.content.logic.object.la64.LA64Object;

public final class LibraryC {

    private LibraryC() { }

    public static final LA64Object LIB_C;

    static {
        C99ToLA64Compiler compiler = new C99ToLA64Compiler();
        LA64Assembler assembler = new LA64Assembler();

        String libcCode = """
                          int putchar(int c) {
                              char* tty = (char*)%#X;
                              *tty = (char)c;
                              return c;
                          }
                          int puts(const char* str) {
                              while (*str) {
                                  putchar(*str++);
                              }
                              putchar('\\n');
                              return 0;
                          }
                          unsigned long strlen(const char* str) {
                              unsigned long len = 0;
                              while (*str++) {
                                  len++;
                              }
                              return len;
                          }
                          int strcmp(const char* str1, const char* str2) {
                              while (*str1 && (*str1 == *str2)) {
                                  str1++;
                                  str2++;
                              }
                              return *(unsigned char*)str1 - *(unsigned char*)str2;
                          }
                          int atoi(const char* str) {
                              int result = 0;
                              int sign = 1;
                              if (*str == '-') {
                                  sign = -1;
                                  str++;
                              }
                              while (*str >= '0' && *str <= '9') {
                                  result = result * 10 + (*str - '0');
                                  str++;
                              }
                              return sign * result;
                          }
                          """;
        libcCode = String.format(libcCode, Teletypewriter.BASE_ADDRESS);

        LA64Assembly libcAssembly = compiler.compile(libcCode);
        LIB_C = assembler.assemble(libcAssembly);
    }
}
