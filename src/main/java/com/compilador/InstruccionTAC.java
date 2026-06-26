package com.compilador;

/**
 * Representa una instrucción de Código de Tres Direcciones (TAC)
 * Facilita el análisis y optimización del código intermedio mediante una estructura orientada a objetos en lugar de texto sin formato
 */
public class InstruccionTAC {
    public enum Tipo {
        ASIGNACION,
        SALTO,
        SALTO_CONDICIONAL,
        ETIQUETA,
        PARAM,
        CALL,
        RETURN,
        RAW // Para casos complejos no previstos
    }

    private Tipo tipo;
    private String destino;
    private String operando1;
    private String operador;
    private String operando2;
    private String textoRaw;

    // Constructor para ETIQUETA, SALTO, PARAM, RETURN simples
    public InstruccionTAC(Tipo tipo, String operando1) {
        this.tipo = tipo;
        this.operando1 = operando1;
    }

    // Constructor para ASIGNACION (ej. x = y o x = y + z)
    public InstruccionTAC(Tipo tipo, String destino, String operando1, String operador, String operando2) {
        this.tipo = tipo;
        this.destino = destino;
        this.operando1 = operando1;
        this.operador = operador;
        this.operando2 = operando2;
    }

    // Constructor para SALTO_CONDICIONAL (ej. if x < y goto L1)
    // destino sería la etiqueta L1, operando1 y 2 son las variables, operador es la condición
    public static InstruccionTAC saltoCondicional(String op1, String operador, String op2, String etiquetaDestino) {
        InstruccionTAC inst = new InstruccionTAC(Tipo.SALTO_CONDICIONAL, null, op1, operador, op2);
        inst.setDestino(etiquetaDestino);
        return inst;
    }

    // Constructor Raw fallback
    public static InstruccionTAC raw(String texto) {
        InstruccionTAC inst = new InstruccionTAC(Tipo.RAW, null);
        inst.textoRaw = texto;
        return inst;
    }

    public Tipo getTipo() { return tipo; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getOperando1() { return operando1; }
    public void setOperando1(String operando1) { this.operando1 = operando1; }
    public String getOperador() { return operador; }
    public String getOperando2() { return operando2; }
    public void setOperando2(String operando2) { this.operando2 = operando2; }

    @Override
    public String toString() {
        if (tipo == Tipo.RAW) return textoRaw;
        
        switch (tipo) {
            case ETIQUETA:
                return "\n" + operando1 + ":";
            case ASIGNACION:
                if (operador == null && operando2 == null) {
                    return destino + " = " + operando1;
                } else if ("[]".equals(operador)) {
                    return destino + " = " + operando1 + "[" + operando2 + "]";
                } else if ("[]=".equals(operador)) { // destino[operando1] = operando2
                    return destino + "[" + operando1 + "] = " + operando2;
                }
                return destino + " = " + operando1 + " " + operador + " " + operando2;
            case SALTO_CONDICIONAL:
                return "if " + operando1 + " " + operador + " " + operando2 + " goto " + destino;
            case SALTO:
                return "goto " + operando1;
            case PARAM:
                return "param " + operando1;
            case CALL:
                if (destino == null) return "call " + operando1 + ", " + operando2;
                return destino + " = call " + operando1 + ", " + operando2;
            case RETURN:
                return operando1 == null ? "return" : "return " + operando1;
        }
        return "";
    }
}
