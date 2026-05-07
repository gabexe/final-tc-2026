package com.compilador;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class App {
    public static void main(String[] args) throws Exception {
        // 1. Obtener flujo de caracteres (desde archivo o consola)
        CharStream input;
        if (args.length > 0) {
            input = CharStreams.fromFileName(args[0]);
        } else {
            System.out.println("Ingrese el código fuente:");
            input = CharStreams.fromStream(System.in);
        }

        // 2. Análisis Léxico
        MiLenguajeLexer lexer = new MiLenguajeLexer(input);
        ManejadorErrores erroresLexicos = new ManejadorErrores("Léxico");
        lexer.removeErrorListeners();
        lexer.addErrorListener(erroresLexicos);

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // 3. Análisis Sintáctico
        MiLenguajeParser parser = new MiLenguajeParser(tokens);
        ManejadorErrores erroresSintacticos = new ManejadorErrores("Sintáctico");
        parser.removeErrorListeners();
        parser.addErrorListener(erroresSintacticos);

        // 4. Iniciar parsing desde la regla principal (programa)
        System.out.println("\n--- Iniciando Compilación ---");
        ParseTree tree = parser.programa();

        // 5. Mostrar resumen
        System.out.println("\n--- Resumen de Errores ---");
        erroresLexicos.imprimirResumen();
        erroresSintacticos.imprimirResumen();

        if (!erroresLexicos.hayErrores() && !erroresSintacticos.hayErrores()) {
            System.out.println("\n¡Compilación exitosa! No se encontraron errores léxicos ni sintácticos.");
        }
    }
}