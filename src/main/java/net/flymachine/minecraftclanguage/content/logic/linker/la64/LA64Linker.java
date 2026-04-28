package net.flymachine.minecraftclanguage.content.logic.linker.la64;

import net.flymachine.minecraftclanguage.content.logic.executable.la64.LA64Executable;
import net.flymachine.minecraftclanguage.content.logic.object.la64.LA64Object;

public final class LA64Linker {

    public LA64Linker() { }

    public LA64Executable link(LA64Object... objects) {
        return new LA64Executable(objects[0].text());
    }
}
