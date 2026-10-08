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
                          int exit(int code) {
                              int (*exit_func)(int) = (int(*)(int)) 0;
                              exit_func(code);
                          }
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
                          
                          // 简易内存管理实现
                          
                          /*
                           * 1 MiB 静态堆。unsigned long 数组保证堆起始至少 8 字节对齐
                           * 块头 32 字节 = 4 个 unsigned long：
                           *   hdr[0] = size    (用户数据字节数，向上取整到 16)
                           *   hdr[1] = is_free (1 = 空闲，0 = 已分配)
                           *   hdr[2] = next    (下一个块的绝对地址)
                           *   hdr[3] = prev    (上一个块的绝对地址)
                           */
                          static unsigned long heap_storage[131072];
                          
                          // 第一个块的地址；0 表示未初始化
                          static unsigned long heap_head = 0;
                          
                          // 初始化堆：创建覆盖整个堆的一个空闲块
                          static void heap_init(void) {
                              unsigned long *hdr;
                          
                              hdr = (unsigned long *)heap_storage;
                              hdr[0] = sizeof(heap_storage) - 32;
                              hdr[1] = 1;
                              hdr[2] = 0;
                              hdr[3] = 0;
                              heap_head = (unsigned long)heap_storage;
                          }
                          
                          void *malloc(unsigned long size) {
                              unsigned long blk;
                              unsigned long *hdr;
                              unsigned long total;
                              unsigned long rest;
                              unsigned long *rhdr;
                              unsigned long next;
                          
                              if (heap_head == 0) {
                                  heap_init();
                              }
                              if (size == 0) {
                                  size = 1;
                              }
                              // 向上取整到 16 字节，保证下一个块仍然 8 字节对齐
                              size = (size + 15) & ~((unsigned long)15);
                          
                              // 首次适应
                              blk = heap_head;
                              while (blk != 0) {
                                  hdr = (unsigned long *)blk;
                                  if (hdr[1] == 1 && hdr[0] >= size) {
                                      // 找到。如果剩余空间足够容纳新头部 + 16 字节数据，就分裂
                                      total = 32 + size;
                                      if (hdr[0] >= total + 48) {
                                          rest = blk + total;
                                          rhdr = (unsigned long *)rest;
                                          next = hdr[2];
                                          rhdr[0] = hdr[0] - total;
                                          rhdr[1] = 1;
                                          rhdr[2] = next;
                                          rhdr[3] = blk;
                                          if (next != 0) {
                                              ((unsigned long *)next)[3] = rest;
                                          }
                                          hdr[2] = rest;
                                          hdr[0] = size;
                                      }
                                      hdr[1] = 0;
                                      // 用户数据从块起始 + 32 开始，8 字节对齐
                                      return (void *)(blk + 32);
                                  }
                                  blk = hdr[2];
                              }
                              return (void *)0;
                          }
                          
                          void free(void *ptr) {
                              unsigned long blk;
                              unsigned long *hdr;
                              unsigned long next;
                              unsigned long prev;
                              unsigned long *nhdr;
                              unsigned long *phdr;
                          
                              if (ptr == (void *)0) {
                                  return;
                              }
                              blk = (unsigned long)ptr - 32;
                              hdr = (unsigned long *)blk;
                              hdr[1] = 1;
                          
                              // 与后继空闲块合并
                              next = hdr[2];
                              if (next != 0) {
                                  nhdr = (unsigned long *)next;
                                  if (nhdr[1] == 1) {
                                      hdr[0] = hdr[0] + 32 + nhdr[0];
                                      hdr[2] = nhdr[2];
                                      if (nhdr[2] != 0) {
                                          ((unsigned long *)nhdr[2])[3] = blk;
                                      }
                                  }
                              }
                          
                              // 与前驱空闲块合并
                              prev = hdr[3];
                              if (prev != 0) {
                                  phdr = (unsigned long *)prev;
                                  if (phdr[1] == 1) {
                                      phdr[0] = phdr[0] + 32 + hdr[0];
                                      phdr[2] = hdr[2];
                                      if (hdr[2] != 0) {
                                          ((unsigned long *)hdr[2])[3] = prev;
                                      }
                                  }
                              }
                          }
                          
                          void *calloc(unsigned long n, unsigned long size) {
                              unsigned long total;
                              unsigned char *p;
                              unsigned long i;
                              void *mem;
                          
                              // 溢出检查
                              if (n != 0 && size != 0) {
                                  total = n * size;
                                  if (total / n != size) {
                                      return (void *)0;
                                  }
                              } else {
                                  total = 0;
                              }
                          
                              mem = malloc(total);
                              if (mem == (void *)0) {
                                  return (void *)0;
                              }
                          
                              p = (unsigned char *)mem;
                              for (i = 0; i < total; i++) {
                                  p[i] = 0;
                              }
                              return mem;
                          }
                          
                          void *realloc(void *ptr, unsigned long size) {
                              unsigned long blk;
                              unsigned long *hdr;
                              void *new_ptr;
                              unsigned char *src;
                              unsigned char *dst;
                              unsigned long n;
                              unsigned long i;
                          
                              if (ptr == (void *)0) {
                                  return malloc(size);
                              }
                              if (size == 0) {
                                  free(ptr);
                                  return (void *)0;
                              }
                          
                              blk = (unsigned long)ptr - 32;
                              hdr = (unsigned long *)blk;
                              if (hdr[0] >= size) {
                                  return ptr;
                              }
                          
                              new_ptr = malloc(size);
                              if (new_ptr == (void *)0) {
                                  return (void *)0;
                              }
                          
                              n = hdr[0];
                              if (n > size) {
                                  n = size;
                              }
                              src = (unsigned char *)ptr;
                              dst = (unsigned char *)new_ptr;
                              for (i = 0; i < n; i++) {
                                  dst[i] = src[i];
                              }
                              free(ptr);
                              return new_ptr;
                          }
                          
                          void *aligned_alloc(unsigned long alignment, unsigned long size) {
                              // 只支持 alignment 为 2 的幂且不超过 8
                              if (alignment == 0) {
                                  return (void *)0;
                              }
                              if ((alignment & (alignment - 1)) != 0) {
                                  return (void *)0;
                              }
                              if (alignment > 8) {
                                  return (void *)0;
                              }
                              // malloc 已经保证 8 字节对齐
                              return malloc(size);
                          }
                          
                          void *memcpy(void *dst, const void *src, unsigned long n) {
                              unsigned char *d;
                              const unsigned char *s;
                              unsigned long i;
                          
                              d = (unsigned char *)dst;
                              s = (const unsigned char *)src;
                              for (i = 0; i < n; i++) {
                                  d[i] = s[i];
                              }
                              return dst;
                          }
                          
                          int memcmp(const void *a, const void *b, unsigned long n) {
                              const unsigned char *pa;
                              const unsigned char *pb;
                              unsigned long i;
                          
                              pa = (const unsigned char *)a;
                              pb = (const unsigned char *)b;
                              for (i = 0; i < n; i++) {
                                  if (pa[i] != pb[i]) {
                                      if (pa[i] < pb[i]) {
                                          return -1;
                                      }
                                      return 1;
                                  }
                              }
                              return 0;
                          }
                          
                          void *memset(void *s, int c, unsigned long n) {
                              unsigned char *p;
                              unsigned char byte;
                              unsigned long i;
                          
                              p = (unsigned char *)s;
                              byte = (unsigned char)c;
                              for (i = 0; i < n; i++) {
                                  p[i] = byte;
                              }
                              return s;
                          }
                          """;
        libcCode = String.format(libcCode, Teletypewriter.BASE_ADDRESS);

        LA64Assembly libcAssembly = compiler.compile(libcCode);
        LIB_C = assembler.assemble(libcAssembly);
    }
}
