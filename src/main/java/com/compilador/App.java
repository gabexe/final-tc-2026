package com.compilador;

public class App {
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("No se proporcionaron argumentos. Iniciando Interfaz Gráfica (Web Terminal)...");
            WebUI.start();
        } else {
            try {
                Compilador compilador = new Compilador(args);
                compilador.compilar();
            } catch (Exception e) {
                System.err.println("Ocurrió un error inesperado durante la compilación: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}