package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.IdentifierNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.TypeNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class SymbolTable {

    public SymbolTable() { }

    public Entry get(String identifier) {
        return symbolTable.get(identifier);
    }

    public void put(String identifier, Entry symbolEntry) {
        symbolTable.put(identifier, symbolEntry);
    }

    public Entry remove(String identifier) {
        return symbolTable.remove(identifier);
    }

    public boolean contains(String identifier) {
        return symbolTable.containsKey(identifier);
    }

    public void clear() {
        symbolTable.clear();
    }

    public Collection<Entry> getEntries() {
        return symbolTable.values();
    }

    private final Map<String, Entry> symbolTable = new HashMap<>();

    public static final class Entry {
        public IdentifierNode id;
        TypeNode typeNode; // 仅用于信息打印
        public Type type;
        public IdentifierAttr attr;

        public Entry(IdentifierNode id, TypeNode typeNode, Type type, IdentifierAttr attr) {
            this.id = id;
            this.typeNode = typeNode;
            this.type = type;
            this.attr = attr;
        }

        public sealed interface IdentifierAttr permits FuncAttr, AutoAttr, StaticAttr {
            boolean isDefinition();

            boolean isGlobal();
        }

        public static final class FuncAttr implements IdentifierAttr {
            public boolean defined;
            /**
             * 是否对其他编译单元可见
             */
            public boolean global;

            FuncAttr(boolean defined, boolean global) {
                this.defined = defined;
                this.global = global;
            }

            @Override
            public boolean isDefinition() {
                return defined;
            }

            @Override
            public boolean isGlobal() {
                return global;
            }
        }

        public static final class StaticAttr implements IdentifierAttr {
            public DefinitionType defType;
            /**
             * 是否对其他编译单元可见
             */
            public boolean global;

            StaticAttr(DefinitionType defType, boolean global) {
                this.defType = defType;
                this.global = global;
            }

            @Override
            public boolean isDefinition() {
                return defType instanceof Defined;
            }

            @Override
            public boolean isGlobal() {
                return global;
            }

            public sealed interface DefinitionType permits Defined, NoDefinition, Tentative { }

            public static final class Tentative implements DefinitionType {
                private Tentative() { }

                public static final Tentative INSTANCE = new Tentative();
            }

            public static final class NoDefinition implements DefinitionType {
                private NoDefinition() { }

                public static final NoDefinition INSTANCE = new NoDefinition();
            }

            public record Defined(StaticInit init) implements DefinitionType {
                public static final Defined INT_ZERO = new Defined(IntInit.ZERO);
                public static final Defined LONG_ZERO = new Defined(LongInit.ZERO);
                public static final Defined UNSIGNED_INT_ZERO = new Defined(UnsignedIntInit.ZERO);
                public static final Defined UNSIGNED_LONG_ZERO = new Defined(UnsignedLongInit.ZERO);
                public static final Defined DOUBLE_ZERO = new Defined(DoubleInit.ZERO);
            }
        }

        public static final class AutoAttr implements IdentifierAttr {
            private AutoAttr() { }

            public static final AutoAttr INSTANCE = new AutoAttr();

            @Override
            public boolean isDefinition() {
                return true;
            }

            @Override
            public boolean isGlobal() {
                return false;
            }
        }
    }
}
