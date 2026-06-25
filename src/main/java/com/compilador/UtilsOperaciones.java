package com.compilador;

/**
 * Clase utilitaria para operaciones comunes dentro del compilador
 * Implementa el principio DRY (Don't Repeat Yourself) para cálculos
 */
public class UtilsOperaciones {

    /**
     * Evalúa una operación aritmética básica
     *
     * @param a  Primer operando entero
     * @param b  Segundo operando entero
     * @param op El operador (+, -, *, /, %)
     * @return El resultado de la operación o null si ocurre un error (ej. división por cero)
     */
    public static Integer calcular(int a, int b, String op) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/": return (b != 0) ? a / b : null;
            case "%": return (b != 0) ? a % b : null;
            default: return null;
        }
    }

    /**
     * Verifica si una cadena de texto representa un número entero válido
     *
     * @param s La cadena a verificar
     * @return true si es entero, false en caso contrario
     */
    public static boolean esEntero(String s) {
        if (s == null || s.isEmpty()) return false;
        try {
            Integer.parseInt(s.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
