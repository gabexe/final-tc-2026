package com.compilador;

import java.util.HashMap;
import java.util.Map;

public class DetectorVariablesInnecesarias {
    private final Map<String, VariableInfo> variables = new HashMap<>();
    private int eliminadas = 0;

    public void analizar(ASTNode ast) {
        recolectarVariables(ast);
        contarUsos(ast);
        eliminarInnecesarias(ast);
        System.out.println("[Agente ML] Variables innecesarias detectadas y eliminadas: " + eliminadas);
    }

    private void recolectarVariables(ASTNode node) {
        if (node == null) return;
        if ("declaracion".equals(node.nombre)) {
            for (ASTNode hijo : node.hijos) {
                if ("declarador".equals(hijo.nombre) && hijo.valor != null) {
                    String nombreVar = hijo.valor;
                    ASTNode valorInicial = buscarInicializacion(hijo);
                    boolean esLiteral = valorInicial != null && esLiteral(valorInicial);
                    variables.put(nombreVar, new VariableInfo(nombreVar, esLiteral, valorInicial, 0));
                }
            }
        }
        for (ASTNode hijo : node.hijos) {
            recolectarVariables(hijo);
        }
    }

    private ASTNode buscarInicializacion(ASTNode declarador) {
        for (ASTNode hijo : declarador.hijos) {
            if ("inicializacion".equals(hijo.nombre) && !hijo.hijos.isEmpty()) {
                return hijo.hijos.get(0);
            }
        }
        return null;
    }

    private boolean esLiteral(ASTNode node) {
        return "int".equals(node.nombre) || "double".equals(node.nombre) ||
               "char".equals(node.nombre) || "string".equals(node.nombre);
    }

    private void contarUsos(ASTNode node) {
        if (node == null) return;
        if ("id".equals(node.nombre) && node.valor != null) {
            VariableInfo info = variables.get(node.valor);
            if (info != null) {
                info.usos++;
            }
        }
        for (ASTNode hijo : node.hijos) {
            contarUsos(hijo);
        }
    }

    private void eliminarInnecesarias(ASTNode node) {
        if (node == null) return;
        for (int i = 0; i < node.hijos.size(); i++) {
            ASTNode hijo = node.hijos.get(i);
            if ("declaracion".equals(hijo.nombre)) {
                boolean eliminarDecl = true;
                for (ASTNode d : hijo.hijos) {
                    if ("declarador".equals(d.nombre) && d.valor != null) {
                        VariableInfo info = variables.get(d.valor);
                        if (info != null && clasificar(info)) {
                            eliminadas++;
                            String valStr = (info.valorInicial != null && info.valorInicial.valor != null) ? info.valorInicial.valor : "expresion";
                            System.out.println("  -> [ML] Eliminando variable innecesaria: '" + info.nombre + "' (valor: " + valStr + ", usos: " + info.usos + ")");
                        } else {
                            eliminarDecl = false;
                        }
                    } else if (!"tipo".equals(d.nombre)) {
                        eliminarDecl = false;
                    }
                }
                if (eliminarDecl) {
                    node.hijos.remove(i);
                    i--;
                }
            } else {
                eliminarInnecesarias(hijo);
            }
        }
    }

    private boolean clasificar(VariableInfo info) {
        if (info.esLiteral && info.usos <= 1) {
            return true;
        }
        return false;
    }

    private static class VariableInfo {
        String nombre;
        boolean esLiteral;
        ASTNode valorInicial;
        int usos;

        VariableInfo(String nombre, boolean esLiteral, ASTNode valorInicial, int usos) {
            this.nombre = nombre;
            this.esLiteral = esLiteral;
            this.valorInicial = valorInicial;
            this.usos = usos;
        }
    }
}