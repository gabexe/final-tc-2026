package com.compilador;

import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class App {
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";

    public static void main(String[] args) {
        try {
            Compilador compilador = new Compilador(args);
            compilador.compilar();
        } catch (Exception e) {
            System.err.println("Ocurrió un error inesperado durante la compilación: " + e.getMessage());
            e.printStackTrace();
        }
    }
}