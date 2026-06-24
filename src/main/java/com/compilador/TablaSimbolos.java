package com.compilador;

import org.antlr.v4.runtime.ParserRuleContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TablaSimbolos {
    private static final String RESET = "\u001B[0m";
    private static final String ROJO = "\u001B[31m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    
    // Nueva clase para manejar Errores y Warnings
    public static class Diagnostic {
        public enum Severity { ERROR, WARNING }
        public final Severity severity;
        public final String message;
        public final int line;
        public final int column;

        public Diagnostic(Severity severity, String message, int line, int column) {
            this.severity = severity;
            this.message = message;
            this.line = line;
            this.column = column;
        }
        
        @Override
        public String toString() {
            return String.format("[%s] Línea %d:%d - %s", severity, line, column, message);
        }
    }

    public static class Symbol {
        public final String name;
        public final String type;
        public final String category; // "variable", "funcion", "parametro", "arreglo"
        public final int scopeLevel;
        public boolean used = false;
        public boolean initialized = false;
        public boolean duplicate = false;
        public ParserRuleContext ctx; // Guardamos el contexto para reportar warnings de "no usado"

        public Symbol(String name, String type, String category, int scopeLevel, ParserRuleContext ctx) {
            this.name = name;
            this.type = type;
            this.category = category;
            this.scopeLevel = scopeLevel;
            this.ctx = ctx;
        }

        @Override
        public String toString() {
            return String.format("%-20s | %-10s | %-12s | Nivel %-6d | %-12b | %-7b | %-9b",
                    name, type, category, scopeLevel, initialized, used, duplicate);
        }
    }

    private final List<Map<String, Symbol>> scopes = new ArrayList<>();
    private final List<Symbol> allSymbols = new ArrayList<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private int currentScopeLevel = 0;

    public TablaSimbolos() {
        scopes.add(new HashMap<>());
    }

    public void addError(String message, ParserRuleContext ctx) {
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();
        diagnostics.add(new Diagnostic(Diagnostic.Severity.ERROR, message, line, col));
    }

    public void addWarning(String message, ParserRuleContext ctx) {
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();
        diagnostics.add(new Diagnostic(Diagnostic.Severity.WARNING, message, line, col));
    }

    public void enterScope() {
        scopes.add(new HashMap<>());
        currentScopeLevel++;
    }

    public void exitScope() {
        if (scopes.size() > 1) {
            Map<String, Symbol> currentScope = scopes.get(scopes.size() - 1);
            // Al salir del scope, revisamos si quedaron variables sin usar
            for (Symbol sym : currentScope.values()) {
                if (!sym.used && !sym.duplicate && 
                   (sym.category.equals("variable") || sym.category.equals("parametro") || sym.category.equals("arreglo"))) {
                    addWarning("La variable '" + sym.name + "' fue declarada pero nunca se usó.", sym.ctx);
                }
            }
            scopes.remove(scopes.size() - 1);
            currentScopeLevel--;
        }
    }
    
    public void checkGlobalScope() {
        Map<String, Symbol> globalScope = scopes.get(0);
        for (Symbol sym : globalScope.values()) {
            if (!sym.used && !sym.duplicate && 
               (sym.category.equals("variable") || sym.category.equals("arreglo"))) {
                addWarning("La variable global '" + sym.name + "' fue declarada pero nunca se usó.", sym.ctx);
            }
        }
    }

    public boolean define(String name, String type, String category, ParserRuleContext ctx) {
        Map<String, Symbol> currentScope = scopes.get(scopes.size() - 1);
        if (currentScope.containsKey(name)) {
            Symbol sym = new Symbol(name, type, category, currentScopeLevel, ctx);
            sym.duplicate = true;
            allSymbols.add(sym);
            return false;
        }
        Symbol sym = new Symbol(name, type, category, currentScopeLevel, ctx);
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
        long duplicadas = allSymbols.stream().filter(s -> s.duplicate).count();
        System.out.println("- Declaraciones duplicadas: " + duplicadas);
        System.out.println("===================================================================================================================");
    }

    public void printDiagnostics() {
        System.out.println("\n--- Diagnóstico Semántico ---");
        if (diagnostics.isEmpty()) {
            System.out.println(VERDE + "✓ No se encontraron errores ni advertencias semánticas." + RESET);
            return;
        }
        
        diagnostics.sort((d1, d2) -> {
            if (d1.line != d2.line) return Integer.compare(d1.line, d2.line);
            return Integer.compare(d1.column, d2.column);
        });
        
        long errors = diagnostics.stream().filter(d -> d.severity == Diagnostic.Severity.ERROR).count();
        long warnings = diagnostics.stream().filter(d -> d.severity == Diagnostic.Severity.WARNING).count();
        
        for (Diagnostic d : diagnostics) {
            if (d.severity == Diagnostic.Severity.ERROR) {
                System.err.println(ROJO + "[ERROR SEMÁNTICO] Línea " + d.line + ":" + d.column + " - " + d.message + RESET);
            } else {
                System.out.println(AMARILLO + "[WARNING] Línea " + d.line + ":" + d.column + " - " + d.message + RESET);
            }
        }
        
        System.out.println("\nResumen Semántico:");
        if (errors > 0) {
            System.out.println(ROJO + "✗ " + errors + " error(es) semántico(s)." + RESET);
        }
        if (warnings > 0) {
            System.out.println(AMARILLO + "⚠ " + warnings + " advertencia(s)." + RESET);
        }
    }

    public boolean hayErroresSemanticos() {
        return diagnostics.stream().anyMatch(d -> d.severity == Diagnostic.Severity.ERROR);
    }

    public int getCantidadErroresSemanticos() {
        return (int) diagnostics.stream().filter(d -> d.severity == Diagnostic.Severity.ERROR).count();
    }

    public static boolean esNumerico(String type) {
        return "int".equals(type) || "double".equals(type);
    }

    public static boolean esCompatibleAsignacion(String tipoVar, String tipoExpr) {
        if (tipoVar == null || tipoExpr == null || "error".equals(tipoVar) || "error".equals(tipoExpr)) return false;
        if (tipoVar.equals(tipoExpr)) return true;
        if (esNumerico(tipoVar) && esNumerico(tipoExpr)) return true; // Permite int <-> double
        return false;
    }
}