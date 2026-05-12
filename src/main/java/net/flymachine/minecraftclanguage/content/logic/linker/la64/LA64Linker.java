package net.flymachine.minecraftclanguage.content.logic.linker.la64;

import net.flymachine.minecraftclanguage.content.logic.executable.la64.LA64Executable;
import net.flymachine.minecraftclanguage.content.logic.linker.LinkOptions;
import net.flymachine.minecraftclanguage.content.logic.memory.Segment;
import net.flymachine.minecraftclanguage.content.logic.object.SymbolEntry;
import net.flymachine.minecraftclanguage.content.logic.object.la64.LA64Object;

public final class LA64Linker {

    public LA64Linker() { }

    private LinkOptions options = LinkOptions.DEFAULT;

    public void setOptions(LinkOptions options) {
        this.options = options;
    }

    public LA64Executable link(LA64Object... objects) {
        LA64Object object = objects[0];

        String newName = object.fileName().replace(".o", ".exe");
        int entrySymbolIndex = object.symbolNames().indexOf(options.entrySymbol());
        if (entrySymbolIndex == -1) {
            throw new RuntimeException("Entry symbol " + options.entrySymbol() + " not found in symbol name table");
        }

        boolean foundEntrySymbol = false;
        int entryOffset = 0;
        for (SymbolEntry entry : object.symbols()) {
            if (entry.symbolNameIndex() == entrySymbolIndex && entry.isGlobal()) {
                if (entry.segment() != Segment.TEXT) {
                    throw new RuntimeException("Invalid entry symbol, in segment " + entry.symbolNameIndex());
                }
                if (foundEntrySymbol) {
                    throw new RuntimeException("Duplicate entry symbol in segment " + entry.symbolNameIndex());
                }
                foundEntrySymbol = true;
                entryOffset = entry.offset();
            }
        }
        if (!foundEntrySymbol) {
            throw new RuntimeException("Entry symbol " + options.entrySymbol() + " not found in symbol table");
        }
        if (!object.relocations().isEmpty()) {
            throw new RuntimeException("Relocations not supported in this version");
        }
        return new LA64Executable(newName, object.text(), options.textVA(), entryOffset);
    }
}
