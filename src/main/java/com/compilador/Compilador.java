package com.compilador;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;
import java.util.ArrayList;

/**
 * Clase orquestadora principal del compilador
 * Centraliza las distintas fases de compilación (Léxica, Sintáctica, Semántica, Generación de TAC y Optimización)
 */
public class Compilador {
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String ROJO = "\u001B[31m";

    private final String archivoFuente;
    private final CharStream input;
    private ManejadorErrores erroresLexicos;
    private ManejadorErrores erroresSintacticos;
    private TablaSimbolos tablaSimbolos;

    public Compilador(String[] args) throws Exception {
        if (args.length > 0) {
            this.archivoFuente = args[0];
            this.input = CharStreams.fromFileName(args[0]);
        } else {
            this.archivoFuente = "stdin.cpp";
            System.out.println("Ingrese el código fuente:");
            this.input = CharStreams.fromStream(System.in);
        }
    }

    /**
     * Ejecuta el pipeline completo de compilación
     */
    public void compilar() {
        System.out.println("\n--- Iniciando Compilación ---");

        // 1. Fase Léxica y Sintáctica
        ParseTree tree = ejecutarFaseLexicoSintactica();

        if (erroresLexicos.hayErrores() || erroresSintacticos.hayErrores()) {
            System.out.println("\nNo se pudo continuar con el análisis semántico debido a errores léxicos o sintácticos previos.");
            imprimirResumenErrores();
            return;
        }

        // 2. Construcción AST
        ASTNode ast = construirAST(tree);

        // 3. Fase Semántica
        boolean okSemantico = ejecutarFaseSemantica(tree);
        tablaSimbolos.printDiagnostics();
        tablaSimbolos.printTable();

        if (!okSemantico) {
            System.out.println(ROJO + "\nSe detectaron errores semánticos. Compilación fallida." + RESET);
            imprimirResumenErrores();
            return;
        }

        System.out.println(VERDE + "\n✓ ¡Compilación exitosa! No se encontraron errores léxicos, sintácticos ni semánticos." + RESET);

        // 4. Fase de Generación de TAC
        List<InstruccionTAC> codigoOriginalInst = generarTAC(tree);
        List<String> codigoOriginalStr = formatearTAC(codigoOriginalInst);
        imprimirTAC("TAC sin optimizar", codigoOriginalStr);

        // 5. Fase de Optimización (IA + Clásica)
        List<InstruccionTAC> codigoOptimizadoInst = optimizarTAC(tree, codigoOriginalInst);
        List<String> codigoOptimizadoStr = formatearTAC(codigoOptimizadoInst);
        imprimirTAC("Código TAC Final Optimizado", codigoOptimizadoStr);

        // 6. Generación de Archivos de Salida
        System.out.println("\n--- Fase 6: Generación de Archivos de Salida ---");
        GeneradorArchivos.generarSalidas(codigoOriginalStr, codigoOptimizadoStr, archivoFuente);

        imprimirResumenErrores();
    }

    private ParseTree ejecutarFaseLexicoSintactica() {
        MiLenguajeLexer lexer = new MiLenguajeLexer(input);
        erroresLexicos = new ManejadorErrores("Léxico");
        lexer.removeErrorListeners();
        lexer.addErrorListener(erroresLexicos);
        
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        MiLenguajeParser parser = new MiLenguajeParser(tokens);
        erroresSintacticos = new ManejadorErrores("Sintáctico");
        parser.removeErrorListeners();
        parser.addErrorListener(erroresSintacticos);

        ParseTree tree = parser.programa();
        
        if (!erroresLexicos.hayErrores() && !erroresSintacticos.hayErrores()) {
            System.out.println("\n--- Árbol Sintáctico (Parse Tree) ---");
            System.out.println(tree.toStringTree(parser));
        }
        
        return tree;
    }

    private ASTNode construirAST(ParseTree tree) {
        System.out.println("\n--- Árbol de Sintaxis Abstracta (AST) ---");
        ASTBuilder builder = new ASTBuilder();
        ASTNode ast = tree.accept(builder);
        System.out.println(ast);
        return ast;
    }

    private boolean ejecutarFaseSemantica(ParseTree tree) {
        System.out.println("\n--- Ejecutando Análisis Semántico ---");
        tablaSimbolos = new TablaSimbolos();
        AnalizadorSemantico analizadorSemantico = new AnalizadorSemantico(tablaSimbolos);
        analizadorSemantico.visit(tree);
        return !analizadorSemantico.hayErrores();
    }

    private List<InstruccionTAC> generarTAC(ParseTree tree) {
        System.out.println("\n--- Fase 4: Generación de Código Intermedio (TAC) ---");
        GeneradorTAC generadorTAC = new GeneradorTAC();
        generadorTAC.visit(tree);
        return generadorTAC.getInstrucciones();
    }
    
    private List<String> formatearTAC(List<InstruccionTAC> instrucciones) {
        List<String> output = new ArrayList<>();
        for (InstruccionTAC i : instrucciones) {
            output.add(i.toString());
        }
        return output;
    }

    private List<InstruccionTAC> optimizarTAC(ParseTree tree, List<InstruccionTAC> codigoOriginal) {
        System.out.println("\n--- Fase 4.5: Agentes IA sobre el TAC ---");
        AgenteOptimizadorTAC agenteIA = new AgenteOptimizadorTAC();
        List<InstruccionTAC> codigoConIA = agenteIA.optimizeTAC(codigoOriginal);

        DetectorVariablesInnecesarias detector = new DetectorVariablesInnecesarias();
        ASTBuilder builderIA = new ASTBuilder();
        ASTNode astIA = tree.accept(builderIA);
        detector.analizar(astIA);

        System.out.println("\n--- Fase 5: Optimización Clásica (Post-Agente IA) ---");
        return Optimizador.optimizar(codigoConIA);
    }

    private void imprimirTAC(String titulo, List<String> codigo) {
        System.out.println("\n--- " + titulo + " ---");
        for (String instr : codigo) {
            System.out.println(instr);
        }
        System.out.println("----------------------------------------\n");
    }

    private void imprimirResumenErrores() {
        System.out.println("\n--- Resumen Final de Errores ---");
        if (erroresLexicos != null) erroresLexicos.imprimirResumen();
        if (erroresSintacticos != null) erroresSintacticos.imprimirResumen();
        if (tablaSimbolos != null) {
            System.out.println("Errores semánticos críticos: " + tablaSimbolos.getCantidadErroresSemanticos());
        }
    }
}
