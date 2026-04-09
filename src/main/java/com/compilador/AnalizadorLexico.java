package com.compilador;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

public class AnalizadorLexico {
    public static void main(String[] args) throws Exception {
        boolean soportaColor = System.console() != null && System.getenv("TERM") != null;
        String RED = soportaColor ? "\u001B[31m" : "";
        String RESET = soportaColor ? "\u001B[0m" : "";

        CharStream input;
        if (args.length > 0) {
            input = CharStreams.fromFileName(args[0]);
        } else {
            System.out.println("Ingrese el código fuente (Ctrl+Z para finalizar):");
            input = CharStreams.fromStream(System.in);
        }
        MiLenguajeLexer lexer = new MiLenguajeLexer(input);
        Token token;
        System.out.println("\nTokens reconocidos:");
        while ((token = lexer.nextToken()).getType() != Token.EOF) {
            String nombre = MiLenguajeLexer.VOCABULARY.getSymbolicName(token.getType());
            if (nombre == null) {
                System.err.println(RED + "[Error léxico] Linea " + token.getLine() + ", Columna " + token.getCharPositionInLine() + ": '" + token.getText() + "'" + RESET);
            } else {
                System.out.printf("%-12s | %-10s | Linea:%d Col:%d\n", nombre, token.getText(), token.getLine(), token.getCharPositionInLine());
            }
        }
    }
}
