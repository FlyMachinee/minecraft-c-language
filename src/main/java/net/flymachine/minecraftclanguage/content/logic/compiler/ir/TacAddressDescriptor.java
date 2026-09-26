package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantPointer;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;

/**
 * 描述一个内存地址的计算
 *
 * @param base   基地址，应为指针类型
 * @param index  索引，应为整型类型，若为 null 表示无索引
 * @param scale  缩放因子，当 index 不为 null 时有效
 * @param offset 字节偏移量
 */
public record TacAddressDescriptor(
    TacValue base, @Nullable TacValue index, long scale, long offset) implements TacDataStructure {

    public TacAddressDescriptor(TacValue base) {
        this(base, null, 1, 0);
    }

    public TacAddressDescriptor(TacValue base, TacValue index) {
        this(base, index, 1, 0);
    }

    public TacAddressDescriptor fold() {
        TacValue base = this.base;
        TacValue index = this.index;
        long scale = this.scale;
        long offset = this.offset;

        boolean changed = false;

        if (index instanceof TacConstant indexConst) {
            long indexValue = indexConst.value.toLong().value();
            offset += indexValue * scale;
            index = null;
            scale = 1;
            changed = true;
        }

        if (base instanceof TacConstant baseConst && offset != 0) {
            ConstantPointer basePointer = (ConstantPointer) baseConst.value;
            base = new TacConstant(new ConstantPointer(basePointer.value() + offset, basePointer.referencedType()));
            offset = 0;
            changed = true;
        }

        if (changed) {
            return new TacAddressDescriptor(base, index, scale, offset);
        } else {
            return this;
        }
    }

    public boolean hasIndex() {
        return index != null;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        stream.print("AddrDesc[");
        base.dump(stream);
        stream.print(" + ");
        if (hasIndex()) {
            index.dump(stream);
            stream.print(" * ");
            stream.print(scale);
            stream.print(" + ");
        }
        stream.print(offset);
        stream.print("]");
    }
}
