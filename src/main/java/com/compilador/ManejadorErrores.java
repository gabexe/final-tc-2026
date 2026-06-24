package com.compilador;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

public class ManejadorErrores extends BaseErrorListener {
    private final List<String> errores = new ArrayList<>();
    private final String tipo;

    private static final String RESET = "\u001B[0m";
    private static final String ROJO = "\u001B[31m";
    private static final String VERDE = "\u001B[32m";

    public ManejadorErrores(String tipo) {
        this.tipo = tipo; // "Léxico" o "Sintáctico"
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        int lineaReportada = line;
        if (msg.contains("mismatched input") && (
            msg.matches(".*expecting.*([';']|\\{[^}]*;[^}]*\\}).*") ||
            msg.matches(".*expecting.*([,]|\\{[^}]*,[^}]*\\}).*")
        )) {
            lineaReportada = Math.max(1, line - 1);
        }
        
        String error = String.format("%s[ERROR %s] Línea %d:%d - %s%s", 
            ROJO, tipo, lineaReportada, charPositionInLine, msg, RESET);
        errores.add(error);
        System.err.println(error);
    }

    public boolean hayErrores() {
        return !errores.isEmpty();
    }

    public int getCantidadErrores() {
        return errores.size();
    }

    public void imprimirResumen() {
        if (errores.isEmpty()) {
            System.out.println(VERDE + "✓ No se encontraron errores " + tipo.toLowerCase() + "s." + RESET);
        } else {
            System.out.println(ROJO + "✗ Errores " + tipo.toLowerCase() + "s encontrados: " + errores.size() + RESET);
        }
    }
}