package com.compilador;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgenteOptimizadorTAC implements OptimizationAgent {
    private int optimizaciones = 0;
    private List<String> codigoOptimizado;

    @Override
    public void optimize(ASTNode node) {
    }

    public List<String> optimizeTAC(List<String> codigo) {
        this.codigoOptimizado = new ArrayList<>(codigo);
        System.out.println("[Agente IA - TAC] Iniciando optimización sobre código intermedio...");
        aplicarConstantFoldingAvanzado();
        aplicarPropagacionConstantes();
        eliminarAsignacionesRedundantes();
        System.out.println("[Agente IA - TAC] Total de optimizaciones aplicadas: " + optimizaciones);
        return codigoOptimizado;
    }

    private void aplicarConstantFoldingAvanzado() {
        Pattern pattern = Pattern.compile("^\\s*(\\w+)\\s*=\\s*(-?\\d+)\\s*([+\\-*/%])\\s*(-?\\d+)\\s*$");
        List<String> resultado = new ArrayList<>();
        int cambios = 0;

        for (String linea : codigoOptimizado) {
            Matcher m = pattern.matcher(linea);
            if (m.matches()) {
                String destino = m.group(1);
                int a = Integer.parseInt(m.group(2));
                String op = m.group(3);
                int b = Integer.parseInt(m.group(4));
                Integer res = calcular(a, b, op);
                if (res != null) {
                    resultado.add(destino + " = " + res);
                    cambios++;
                    continue;
                }
            }
            resultado.add(linea);
        }

        codigoOptimizado = resultado;
        optimizaciones += cambios;
        System.out.println("  [CF Avanzado] " + cambios + " operaciones plegadas por agente IA");
    }

    private void aplicarPropagacionConstantes() {
        Pattern asignacion = Pattern.compile("^\\s*(\\w+)\\s*=\\s*(-?\\d+)\\s*$");
        java.util.Map<String, String> constantes = new java.util.HashMap<>();
        int cambios = 0;

        for (String linea : codigoOptimizado) {
            Matcher m = asignacion.matcher(linea);
            if (m.matches()) {
                String var = m.group(1);
                String val = m.group(2);
                constantes.put(var, val);
            }
        }

        if (constantes.isEmpty()) return;

        List<String> resultado = new ArrayList<>();
        Pattern opPattern = Pattern.compile("^\\s*(\\w+)\\s*=\\s*(\\w+)\\s*([+\\-*/%])\\s*(\\w+)\\s*$");

        for (String linea : codigoOptimizado) {
            Matcher m = opPattern.matcher(linea);
            if (m.matches()) {
                String dest = m.group(1);
                String op1 = m.group(2);
                String op = m.group(3);
                String op2 = m.group(4);

                String val1 = constantes.getOrDefault(op1, op1);
                String val2 = constantes.getOrDefault(op2, op2);

                if (!val1.equals(op1) || !val2.equals(op2)) {
                    resultado.add(dest + " = " + val1 + " " + op + " " + val2);
                    cambios++;
                    continue;
                }
            }
            resultado.add(linea);
        }

        codigoOptimizado = resultado;
        optimizaciones += cambios;
        System.out.println("  [Propagación] " + cambios + " variables propagadas como constantes");
    }

    private void eliminarAsignacionesRedundantes() {
        List<String> resultado = new ArrayList<>();
        java.util.Map<String, String> ultimasAsignaciones = new java.util.HashMap<>();
        int cambios = 0;

        Pattern asignacion = Pattern.compile("^\\s*(\\w+)\\s*=\\s*(\\w+)\\s*$");

        for (String linea : codigoOptimizado) {
            Matcher m = asignacion.matcher(linea);
            if (m.matches()) {
                String var = m.group(1);
                String valor = m.group(2);
                if (var.equals(valor) || (ultimasAsignaciones.containsKey(var) && ultimasAsignaciones.get(var).equals(valor))) {
                    cambios++;
                    continue;
                }
                ultimasAsignaciones.put(var, valor);
            } else {
                ultimasAsignaciones.clear();
            }
            resultado.add(linea);
        }

        codigoOptimizado = resultado;
        optimizaciones += cambios;
        System.out.println("  [Redundantes] " + cambios + " asignaciones redundantes eliminadas");
    }

    private Integer calcular(int a, int b, String op) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/": return (b != 0) ? a / b : null;
            case "%": return (b != 0) ? a % b : null;
            default: return null;
        }
    }
}