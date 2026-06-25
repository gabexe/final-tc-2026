package com.compilador;

import java.util.*;

/**
 * Optimizador de Código Intermedio.
 */
public class Optimizador {

    public static List<InstruccionTAC> optimizar(List<InstruccionTAC> codigo) {
        System.out.println("\n--- Aplicando Optimizaciones ---");

        List<InstruccionTAC> resultado = new ArrayList<>(codigo);

        resultado = constantFolding(resultado);
        resultado = commonSubexpressionElimination(resultado);
        resultado = deadCodeElimination(resultado);

        return resultado;
    }

    private static List<InstruccionTAC> constantFolding(List<InstruccionTAC> codigo) {
        List<InstruccionTAC> optimizado = new ArrayList<>();
        int cambios = 0;

        for (InstruccionTAC inst : codigo) {
            if (inst.getTipo() == InstruccionTAC.Tipo.ASIGNACION && 
                inst.getOperador() != null &&
                !inst.getOperador().equals("[]") && !inst.getOperador().equals("[]=")) {
                
                String op1 = inst.getOperando1();
                String op2 = inst.getOperando2();
                
                if (UtilsOperaciones.esEntero(op1) && UtilsOperaciones.esEntero(op2)) {
                    int a = Integer.parseInt(op1);
                    int b = Integer.parseInt(op2);
                    Integer res = UtilsOperaciones.calcular(a, b, inst.getOperador());
                    
                    if (res != null) {
                        optimizado.add(new InstruccionTAC(InstruccionTAC.Tipo.ASIGNACION, inst.getDestino(), String.valueOf(res), null, null));
                        cambios++;
                        continue;
                    }
                }
            }
            optimizado.add(inst);
        }
        System.out.println("[Constant Folding] Eliminadas " + cambios + " operaciones en tiempo de compilación.");
        return optimizado;
    }

    private static List<InstruccionTAC> commonSubexpressionElimination(List<InstruccionTAC> codigo) {
        List<InstruccionTAC> optimizado = new ArrayList<>();
        Map<String, String> expresiones = new HashMap<>();
        int cambios = 0;

        for (InstruccionTAC inst : codigo) {
            if (inst.getTipo() == InstruccionTAC.Tipo.ASIGNACION) {
                if (inst.getOperador() != null && !inst.getOperador().equals("[]") && !inst.getOperador().equals("[]=")) {
                    String clave = inst.getOperando1() + " " + inst.getOperador() + " " + inst.getOperando2();
                    
                    if (expresiones.containsKey(clave)) {
                        String temporalPrevio = expresiones.get(clave);
                        optimizado.add(new InstruccionTAC(InstruccionTAC.Tipo.ASIGNACION, inst.getDestino(), temporalPrevio, null, null));
                        cambios++;
                    } else {
                        expresiones.put(clave, inst.getDestino());
                        optimizado.add(inst);
                    }
                } else {
                    // Si es asignación simple o de arreglo, limpiar la tabla si se sobreescribe una variable
                    String dest = inst.getDestino();
                    expresiones.entrySet().removeIf(e -> e.getValue().equals(dest) || e.getKey().contains(dest));
                    optimizado.add(inst);
                }
            } else {
                optimizado.add(inst);
            }
        }
        System.out.println("[CSE] Eliminadas " + cambios + " subexpresiones comunes.");
        return optimizado;
    }

    private static List<InstruccionTAC> deadCodeElimination(List<InstruccionTAC> codigo) {
        Set<String> usadas = new HashSet<>();

        // Paso 1: Recolectar variables usadas
        for (InstruccionTAC inst : codigo) {
            switch (inst.getTipo()) {
                case ASIGNACION:
                    if (inst.getOperando1() != null && !UtilsOperaciones.esEntero(inst.getOperando1())) usadas.add(inst.getOperando1());
                    if (inst.getOperando2() != null && !UtilsOperaciones.esEntero(inst.getOperando2())) usadas.add(inst.getOperando2());
                    // Para arreglos (destino[op1] = op2), operando1 es el indice, destino es usado si es lectura, etc.
                    // Simplified: just add operando1 and 2
                    break;
                case SALTO_CONDICIONAL:
                    if (inst.getOperando1() != null && !UtilsOperaciones.esEntero(inst.getOperando1())) usadas.add(inst.getOperando1());
                    if (inst.getOperando2() != null && !UtilsOperaciones.esEntero(inst.getOperando2())) usadas.add(inst.getOperando2());
                    break;
                case PARAM:
                case RETURN:
                    if (inst.getOperando1() != null && !UtilsOperaciones.esEntero(inst.getOperando1())) usadas.add(inst.getOperando1());
                    break;
                case CALL:
                    if (inst.getOperando2() != null && !UtilsOperaciones.esEntero(inst.getOperando2())) usadas.add(inst.getOperando2());
                    break;
                default:
                    break;
            }
        }

        // Paso 2: Eliminar asignaciones a temporales no usadas
        List<InstruccionTAC> optimizado = new ArrayList<>();
        int eliminadas = 0;

        for (InstruccionTAC inst : codigo) {
            if (inst.getTipo() == InstruccionTAC.Tipo.ASIGNACION && inst.getDestino() != null) {
                if (inst.getDestino().startsWith("t") && !usadas.contains(inst.getDestino())) {
                    eliminadas++;
                    continue;
                }
            }
            if (inst.getTipo() == InstruccionTAC.Tipo.CALL && inst.getDestino() != null) {
                if (inst.getDestino().startsWith("t") && !usadas.contains(inst.getDestino())) {
                    // Se deja la llamada, solo ignoramos su retorno
                    // Opcionalmente se podria dejar igual para efectos secundarios
                    optimizado.add(new InstruccionTAC(InstruccionTAC.Tipo.CALL, null, inst.getOperando1(), null, inst.getOperando2()));
                    continue;
                }
            }
            optimizado.add(inst);
        }
        System.out.println("[Dead Code] Eliminadas " + eliminadas + " instrucciones de código muerto.");
        return optimizado;
    }
}