package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.IntInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.LongInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StaticInit;
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

        public sealed interface IdentifierAttr permits FuncAttr, LocalAttr, StaticAttr {
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
            public InitialValue initialValue;
            /**
             * 是否对其他编译单元可见
             */
            public boolean global;

            StaticAttr(InitialValue initialValue, boolean global) {
                this.initialValue = initialValue;
                this.global = global;
            }

            @Override
            public boolean isDefinition() {
                return initialValue instanceof Initial;
            }

            @Override
            public boolean isGlobal() {
                return global;
            }

            public sealed interface InitialValue permits Initial, NoInitializer, Tentative { }

            public static final class Tentative implements InitialValue {
                private Tentative() { }

                public static final Tentative INSTANCE = new Tentative();
            }

            public static final class NoInitializer implements InitialValue {
                private NoInitializer() { }

                public static final NoInitializer INSTANCE = new NoInitializer();
            }

            public record Initial(StaticInit init) implements InitialValue {
                public static final Initial INT_ZERO = new Initial(IntInit.ZERO);
                public static final Initial LONG_ZERO = new Initial(LongInit.ZERO);
            }
        }

        public static final class LocalAttr implements IdentifierAttr {
            private LocalAttr() { }

            public static final LocalAttr INSTANCE = new LocalAttr();

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
