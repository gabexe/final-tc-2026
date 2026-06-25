package com.compilador;

/**
 * Constantes para los tipos de datos manejados por el compilador.
 * Implementa el principio Clean Code de eliminar "Magic Strings" (valores repetidos y no documentados)
 */

public class TiposLenguaje {
    public static final String INT = "int";
    public static final String DOUBLE = "double";
    public static final String CHAR = "char";
    public static final String STRING = "string";
    public static final String BOOL = "bool";
    public static final String VOID = "void";
    public static final String ERROR = "error";

    public static final String CAT_VARIABLE = "variable";
    public static final String CAT_FUNCION = "funcion";
    public static final String CAT_PARAMETRO = "parametro";
    public static final String CAT_ARREGLO = "arreglo";

    /**
     * Verifica si un tipo de dato es numérico (entero o decimal)
     */
    public static boolean esNumerico(String type) {
        return INT.equals(type) || DOUBLE.equals(type);
    }

    /**
     * Verifica si dos tipos son compatibles para una operación de asignación
     */
    public static boolean esCompatibleAsignacion(String tipoVar, String tipoExpr) {
        if (tipoVar == null || tipoExpr == null || ERROR.equals(tipoVar) || ERROR.equals(tipoExpr)) {
            return false;
        }
        if (tipoVar.equals(tipoExpr)) {
            return true;
        }
        // Permite asignación entre numéricos (conversión implícita)
        return esNumerico(tipoVar) && esNumerico(tipoExpr);
    }
}
