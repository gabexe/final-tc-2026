package com.compilador;

import java.util.*;
import java.util.regex.*;

public class Optimizador {

    public static List<String> optimizar(List<String> codigo) {
        System.out.println("\n--- Aplicando Optimizaciones ---");

        List<String> resultado = new ArrayList<>(codigo);

        resultado = constantFolding(resultado);
        resultado = commonSubexpressionElimination(resultado);
        resultado = deadCodeElimination(resultado);

        return resultado;
    }

    private static List<String> constantFolding(List<String> codigo) {
        List<String> optimizado = new ArrayList<>();
        Pattern pattern = Pattern.compile("^\\s*(\\w+)\\s*=\\s*(\\w+)\\s*([+\\-*/%])\\s*(\\w+)\\s*$");
        int cambios = 0;

        for (String linea : codigo) {
            Matcher m = pattern.matcher(linea);
            if (m.matches()) {
                String result = m.group(1);
                String op1 = m.group(2);
                String op = m.group(3);
                String op2 = m.group(4);

                if (esEntero(op1) && esEntero(op2)) {
                    int a = Integer.parseInt(op1);
                    int b = Integer.parseInt(op2);
                    Integer res = calcular(a, b, op);
                    if (res != null) {
                        optimizado.add(result + " = " + res);
                        cambios++;
                        continue;
                    }
                }
            }
            optimizado.add(linea);
        }
        System.out.println("[Constant Folding] Eliminadas " + cambios + " operaciones en tiempo de compilación.");
        return optimizado;
    }

    private static List<String> commonSubexpressionElimination(List<String> codigo) {
        List<String> optimizado = new ArrayList<>();
        Map<String, String> expresiones = new HashMap<>();
        Pattern pattern = Pattern.compile("^\\s*(\\w+)\\s*=\\s*(\\w+)\\s*([+\\-*/%])\\s*(\\w+)\\s*$");
        int cambios = 0;

        for (String linea : codigo) {
            Matcher m = pattern.matcher(linea);
            if (m.matches()) {
                String result = m.group(1);
                String op1 = m.group(2);
                String op = m.group(3);
                String op2 = m.group(4);

                String clave = op1 + " " + op + " " + op2;

                if (expresiones.containsKey(clave)) {
                    String temporalPrevio = expresiones.get(clave);
                    optimizado.add(result + " = " + temporalPrevio);
                    cambios++;
                } else {
                    expresiones.put(clave, result);
                    optimizado.add(linea);
                }
            } else {
                Pattern asignacion = Pattern.compile("^\\s*(\\w+)\\s*=\\s*(\\w+)\\s*$");
                Matcher m2 = asignacion.matcher(linea);
                if (m2.matches()) {
                    String var = m2.group(1);
                    expresiones.entrySet().removeIf(e -> e.getValue().equals(var) || e.getKey().contains(var));
                }
                optimizado.add(linea);
            }
        }
        System.out.println("[CSE] Eliminadas " + cambios + " subexpresiones comunes.");
        return optimizado;
    }

    private static List<String> deadCodeElimination(List<String> codigo) {
        Set<String> usadas = new HashSet<>();
        Pattern usoPattern = Pattern.compile("\\b([a-zA-Z_]\\w*|t\\d+)\\b");

        for (String linea : codigo) {
            if (linea.trim().endsWith(":") || linea.trim().startsWith("goto") ||
                linea.trim().startsWith("if ") || linea.trim().startsWith("param") ||
                linea.trim().startsWith("return") || linea.trim().startsWith("call")) {
                Matcher m = usoPattern.matcher(linea);
                while (m.find()) usadas.add(m.group(1));
                continue;
            }

            Pattern asignacion = Pattern.compile("^\\s*(\\w+)\\s*=\\s*(.*)$");
            Matcher m = asignacion.matcher(linea);
            if (m.matches()) {
                String ladoDerecho = m.group(2);
                Matcher uso = usoPattern.matcher(ladoDerecho);
                while (uso.find()) usadas.add(uso.group(1));
            }
        }

        List<String> optimizado = new ArrayList<>();
        int eliminadas = 0;
        Pattern tempPattern = Pattern.compile("^\\s*(t\\d+)\\s*=.*$");

        for (String linea : codigo) {
            Matcher m = tempPattern.matcher(linea);
            if (m.matches()) {
                String temp = m.group(1);
                if (!usadas.contains(temp)) {
                    eliminadas++;
                    continue;
                }
            }
            optimizado.add(linea);
        }
        System.out.println("[Dead Code] Eliminadas " + eliminadas + " instrucciones de código muerto.");
        return optimizado;
    }

    private static boolean esEntero(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static Integer calcular(int a, int b, String op) {
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