package com.compilador;

import java.util.List;

public class AgenteOptimizador implements OptimizationAgent {
    private int optimizacionesAplicadas = 0;

    @Override
    public void optimize(ASTNode node) {
        if (node == null) return;
        constantFolding(node);
        System.out.println("[Agente IA] Constant Folding aplicado: " + optimizacionesAplicadas + " operaciones simplificadas en el AST.");
    }

    private void constantFolding(ASTNode node) {
        List<ASTNode> hijos = node.hijos;
        for (int i = 0; i < hijos.size(); i++) {
            ASTNode hijo = hijos.get(i);
            constantFolding(hijo);
            if (esOperacionConLiterales(hijo)) {
                ASTNode reemplazo = plegarConstantes(hijo);
                if (reemplazo != null) {
                    hijos.set(i, reemplazo);
                    optimizacionesAplicadas++;
                }
            }
        }
    }

    private boolean esOperacionConLiterales(ASTNode node) {
        if (!"op".equals(node.nombre)) return false;
        if (node.hijos.size() != 2) return false;
        ASTNode izq = node.hijos.get(0);
        ASTNode der = node.hijos.get(1);
        return esLiteral(izq) && esLiteral(der);
    }

    private boolean esLiteral(ASTNode node) {
        return "int".equals(node.nombre) || "double".equals(node.nombre);
    }

    private ASTNode plegarConstantes(ASTNode node) {
        String operador = node.valor;
        ASTNode izq = node.hijos.get(0);
        ASTNode der = node.hijos.get(1);
        try {
            if ("int".equals(izq.nombre) && "int".equals(der.nombre)) {
                int a = Integer.parseInt(izq.valor);
                int b = Integer.parseInt(der.valor);
                Integer resultado = calcularEntero(a, b, operador);
                if (resultado != null) {
                    return new ASTNode("int", resultado.toString());
                }
            } else if (("int".equals(izq.nombre) || "double".equals(izq.nombre)) &&
                       ("int".equals(der.nombre) || "double".equals(der.nombre))) {
                double a = Double.parseDouble(izq.valor);
                double b = Double.parseDouble(der.valor);
                Double resultado = calcularDouble(a, b, operador);
                if (resultado != null) {
                    if (resultado == Math.floor(resultado) && !Double.isInfinite(resultado) &&
                        "int".equals(izq.nombre) && "int".equals(der.nombre)) {
                        return new ASTNode("int", resultado.intValue() + "");
                    }
                    return new ASTNode("double", resultado.toString());
                }
            }
        } catch (NumberFormatException e) {
        }
        return null;
    }

    private Integer calcularEntero(int a, int b, String op) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/": return (b != 0) ? a / b : null;
            case "%": return (b != 0) ? a % b : null;
            default: return null;
        }
    }

    private Double calcularDouble(double a, double b, String op) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/": return (b != 0.0) ? a / b : null;
            case "%": return (b != 0.0) ? a % b : null;
            default: return null;
        }
    }

    public int getOptimizacionesAplicadas() {
        return optimizacionesAplicadas;
    }
}