package com.compilador;

import java.util.List;

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

        TablaSimbolos tablaSimbolos = new TablaSimbolos();

        // 5. Construir y mostrar el AST si no hay errores
        if (!erroresLexicos.hayErrores() && !erroresSintacticos.hayErrores()) {
            System.out.println("\n--- Árbol Sintáctico (Parse Tree) ---");
            System.out.println(tree.toStringTree(parser));

            System.out.println("\n--- Árbol de Sintaxis Abstracta (AST) ---");
            ASTBuilder builder = new ASTBuilder();
            ASTNode ast = tree.accept(builder);
            System.out.println(ast);

            // 5b. Ejecutar Análisis Semántico
            System.out.println("\n--- Ejecutando Análisis Semántico ---");
            AnalizadorSemantico analizadorSemantico = new AnalizadorSemantico(tablaSimbolos);
            analizadorSemantico.visit(tree);
            
            // 5c. Imprimir Diagnóstico (Errores y Warnings con línea/columna)
            tablaSimbolos.printDiagnostics();
            
            // 5d. Imprimir Tabla de Símbolos
            tablaSimbolos.printTable();

            if (analizadorSemantico.hayErrores()) {
                System.out.println("\nSe detectaron errores semánticos. Compilación fallida.");
            } else {
                System.out.println("\n¡Compilación exitosa! No se encontraron errores léxicos, sintácticos ni semánticos.");
                
                System.out.println("\n--- Generación de Código Intermedio (TAC) ---");
                GeneradorTAC generadorTAC = new GeneradorTAC();
                generadorTAC.visit(tree);
                generadorTAC.printTAC();

                System.out.println("\n--- Fase 5: Optimización de Código ---");
                List<String> codigoSinOptimizar = generadorTAC.getCodigo();
                List<String> codigoOptimizado = Optimizador.optimizar(codigoSinOptimizar);

                System.out.println("\n--- Código TAC Optimizado ---");
                for (String instr : codigoOptimizado) {
                    System.out.println(instr);
                }
                System.out.println("----------------------------------------\n");
            }
        } else {
            System.out.println("\nNo se pudo continuar con el análisis semántico debido a errores léxicos o sintácticos previos.");
        }

        // 6. Mostrar resumen de errores
        System.out.println("\n--- Resumen Final de Errores ---");
        erroresLexicos.imprimirResumen();
        erroresSintacticos.imprimirResumen();
        System.out.println("Errores semánticos críticos: " + tablaSimbolos.getCantidadErroresSemanticos());
    }
}