package com.compilador;

import java.util.List;
import java.util.ArrayList;

// Nodo del AST
public class ASTNode {
    public final String nombre;
    public final List<ASTNode> hijos = new ArrayList<>();
    public final String valor;

    public ASTNode(String nombre) {
        this(nombre, null);
    }
    public ASTNode(String nombre, String valor) {
        this.nombre = nombre;
        this.valor = valor;
    }
    public void add(ASTNode hijo) {
        hijos.add(hijo);
    }
    @Override
    public String toString() {
        return toString(0);
    }
    private String toString(int nivel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nivel; i++) sb.append("  ");
        sb.append(nombre);
        if (valor != null) sb.append(": ").append(valor);
        sb.append("\n");
        for (ASTNode hijo : hijos) {
            sb.append(hijo.toString(nivel + 1));
        }
        return sb.toString();
    }
}
