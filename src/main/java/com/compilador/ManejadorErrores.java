package com.compilador;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

public class ManejadorErrores extends BaseErrorListener {
    private final List<String> errores = new ArrayList<>();
    private final String tipo;

    public ManejadorErrores(String tipo) {
        this.tipo = tipo; // "Léxico" o "Sintáctico"
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        // Ajuste para errores de falta de ';' o ',' (mismatched input ... expecting ';' o ',' o {';', ','})
        int lineaReportada = line;
        if (msg.contains("mismatched input") && (
            msg.matches(".*expecting.*([';']|\\{[^}]*;[^}]*\\}).*") ||
            msg.matches(".*expecting.*([,]|\\{[^}]*,[^}]*\\}).*")
        )) {
            lineaReportada = Math.max(1, line - 1);
        }
        String error = String.format("Error %s en la línea %d:%d -> %s", tipo, lineaReportada, charPositionInLine, msg);
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
        System.out.println("Errores " + tipo.toLowerCase() + "s: " + errores.size());
    }
}