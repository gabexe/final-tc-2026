package com.compilador;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

public class AnalizadorLexico {
    public static void main(String[] args) throws Exception {
        CharStream input = CharStreams.fromString("int x = 10;");
        MiLenguajeLexer lexer = new MiLenguajeLexer(input);
        Token token;
        while ((token = lexer.nextToken()).getType() != Token.EOF) {
            System.out.println(token.getText() + " -> " + token.getType());
        }
    }
}
