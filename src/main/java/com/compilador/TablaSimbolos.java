package com.compilador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TablaSimbolos {
    public static class Symbol {
        public final String name;
        public final String type;
        public final String category; // "variable", "funcion", "parametro", "arreglo"
        public final int scopeLevel;
        public boolean used = false;
        public boolean initialized = false;
        public boolean duplicate = false;

        public Symbol(String name, String type, String category, int scopeLevel) {
            this.name = name;
            this.type = type;
            this.category = category;
            this.scopeLevel = scopeLevel;
        }

        @Override
        public String toString() {
            return String.format("%-20s | %-10s | %-12s | Nivel %-6d | %-12b | %-7b | %-9b",
                    name, type, category, scopeLevel, initialized, used, duplicate);
        }
    }

    private final List<Map<String, Symbol>> scopes = new ArrayList<>();
    private final List<Symbol> allSymbols = new ArrayList<>();
    private int currentScopeLevel = 0;
    private int duplicateCount = 0;

    public TablaSimbolos() {
        // Inicializar el scope global
        scopes.add(new HashMap<>());
    }

    public void enterScope() {
        scopes.add(new HashMap<>());
        currentScopeLevel++;
    }

    public void exitScope() {
        if (scopes.size() > 1) {
            scopes.remove(scopes.size() - 1);
            currentScopeLevel--;
        }
    }

    public boolean define(String name, String type, String category) {
        Map<String, Symbol> currentScope = scopes.get(scopes.size() - 1);
        if (currentScope.containsKey(name)) {
            Symbol sym = new Symbol(name, type, category, currentScopeLevel);
            sym.duplicate = true;
            duplicateCount++;
            allSymbols.add(sym);
            return false;
        }
        Symbol sym = new Symbol(name, type, category, currentScopeLevel);
        currentScope.put(name, sym);
        allSymbols.add(sym);
        return true;
    }

    public Symbol resolve(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Symbol> scope = scopes.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public List<Symbol> getAllSymbols() {
        return allSymbols;
    }

    public void printTable() {
        System.out.println("\n================================================ TABLA DE SÍMBOLOS ================================================");
        System.out.println(String.format("%-20s | %-10s | %-12s | %-12s | Inicializado | Usado   | Duplicado",
                "Nombre", "Tipo", "Categoría", "Scope"));
        System.out.println("-------------------------------------------------------------------------------------------------------------------");
        for (Symbol sym : allSymbols) {
            System.out.println(sym);
        }
        System.out.println("===================================================================================================================");
        System.out.println("Resumen de la Tabla de Símbolos:");
        System.out.println("- Símbolos totales registrados: " + allSymbols.size());
        System.out.println("- Declaraciones duplicadas: " + duplicateCount);
        System.out.println("===================================================================================================================");
    }
}
