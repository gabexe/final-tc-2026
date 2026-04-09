package com.compilador;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

public class AnalizadorLexico {
    // Listener para contar errores lexicos
    static class ContadorErroresListener extends BaseErrorListener {
        public int errores = 0;
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            errores++;
        }
    }
    public static void main(String[] args) throws Exception {

        // Leer archivo fuente
        CharStream input;
        if (args.length > 0) {
            input = CharStreams.fromFileName(args[0]);
        } else {
            System.out.println("Ingrese el codigo fuente.");
            input = CharStreams.fromStream(System.in);
        }

        MiLenguajeLexer lexer = new MiLenguajeLexer(input);
        // Listener para contar errores lexicos
        ContadorErroresListener errorListener = new ContadorErroresListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        Token token;
        int tokensValidos = 0;  // Contador de tokens validos

        System.out.println("\nTokens reconocidos:");
        // Procesar tokens hasta EOF
        while ((token = lexer.nextToken()).getType() != Token.EOF) {
            String nombre = MiLenguajeLexer.VOCABULARY.getSymbolicName(token.getType());
            if (nombre == null) {
            } else {
                System.out.printf("%-12s | %-10s | Linea:%d Col:%d\n", nombre, token.getText(), token.getLine(), token.getCharPositionInLine());
                tokensValidos++;
            }
        }

        System.out.println("\nResumen:");
        System.out.println("Tokens validos: " + tokensValidos);
        System.out.println("Errores lexicos: " + errorListener.errores);
    }
}