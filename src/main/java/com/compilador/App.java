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

        // [CORRECCIÓN] Declaramos la tabla fuera del IF para que sea visible en el resumen final
        TablaSimbolos tablaSimbolos = new TablaSimbolos();

        // 5. Construir y mostrar el AST si no hay errores
        if (!erroresLexicos.hayErrores() && !erroresSintacticos.hayErrores()) {
            System.out.println("\n--- Árbol Sintáctico (Parse Tree) ---");
            System.out.println(tree.toStringTree(parser));
            System.out.println("\n--- Árbol de Sintaxis Abstracta (AST) ---");
            ASTBuilder builder = new ASTBuilder();
            ASTNode ast = tree.accept(builder);
            System.out.println(ast);

            // 5b. Análisis Semántico y Tabla de Símbolos
            System.out.println("\n--- Análisis Semántico y Tabla de Símbolos ---");
            
            // Ejecutar Visitor Semántico
            AnalizadorSemantico analizadorSemantico = new AnalizadorSemantico(tablaSimbolos);
            analizadorSemantico.visit(tree);
            tablaSimbolos.printTable();

            // Validación final (Sin detalles específicos de línea/columna)
            if (analizadorSemantico.hayErrores()) {
                System.out.println("\nSe detectaron errores semánticos. Compilación fallida.");
            } else {
                System.out.println("\n¡Compilación exitosa! No se encontraron errores léxicos, sintácticos ni semánticos.");
            }
        }

        // 6. Mostrar resumen de errores
        System.out.println("\n--- Resumen de Errores ---");
        erroresLexicos.imprimirResumen();
        erroresSintacticos.imprimirResumen();
        System.out.println("Errores semánticos: " + tablaSimbolos.getCantidadErroresSemanticos());
    }
}