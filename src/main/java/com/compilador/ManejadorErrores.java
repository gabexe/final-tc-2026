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
        String error = String.format("Error %s en la línea %d:%d -> %s", tipo, line, charPositionInLine, msg);
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