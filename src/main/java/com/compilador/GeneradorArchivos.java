package com.compilador;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class GeneradorArchivos {

    private static final String OUTPUT_DIR = "output";

    public static void generarSalidas(List<String> codigoIntermedio, List<String> codigoOptimizado, String archivoFuente) {
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }

        String nombreBase = new File(archivoFuente).getName().replaceAll("\\.[^.]*$", "");
        String pathTAC = OUTPUT_DIR + File.separator + nombreBase + ".tac";
        String pathOPT = OUTPUT_DIR + File.separator + nombreBase + ".opt";

        escribirArchivo(pathTAC, codigoIntermedio, "Código Intermedio (TAC)");
        escribirArchivo(pathOPT, codigoOptimizado, "Código Optimizado");

        System.out.println("Archivos de salida generados:");
        System.out.println("  -> " + pathTAC);
        System.out.println("  -> " + pathOPT);
    }

    private static void escribirArchivo(String path, List<String> lineas, String titulo) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write("# " + titulo);
            writer.newLine();
            writer.write("# Generado automáticamente por el compilador");
            writer.newLine();
            writer.newLine();
            for (String linea : lineas) {
                writer.write(linea);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error al escribir el archivo " + path + ": " + e.getMessage());
        }
    }
}