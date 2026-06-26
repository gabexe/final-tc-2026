package com.compilador;

import java.util.ArrayList;
import java.util.List;

/**
 * Agente optimizador (IA mock) sobre Código Intermedios
 * Utliza InstruccionTAC
 */
public class AgenteOptimizadorTAC implements OptimizationAgent {
    private int optimizaciones = 0;
    private List<InstruccionTAC> codigoOptimizado;

    @Override
    public void optimize(ASTNode node) {
    }

    public List<InstruccionTAC> optimizeTAC(List<InstruccionTAC> codigo) {
        this.codigoOptimizado = new ArrayList<>(codigo);
        System.out.println("[Agente IA - TAC] Iniciando optimización sobre código intermedio...");
        aplicarConstantFoldingAvanzado();
        aplicarPropagacionConstantes();
        eliminarAsignacionesRedundantes();
        System.out.println("[Agente IA - TAC] Total de optimizaciones aplicadas: " + optimizaciones);
        return codigoOptimizado;
    }

    private void aplicarConstantFoldingAvanzado() {
        List<InstruccionTAC> resultado = new ArrayList<>();
        int cambios = 0;

        for (InstruccionTAC inst : codigoOptimizado) {
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
                        resultado.add(new InstruccionTAC(InstruccionTAC.Tipo.ASIGNACION, inst.getDestino(), String.valueOf(res), null, null));
                        cambios++;
                        continue;
                    }
                }
            }
            resultado.add(inst);
        }

        codigoOptimizado = resultado;
        optimizaciones += cambios;
        System.out.println("  [CF Avanzado] " + cambios + " operaciones plegadas por agente IA");
    }

    private void aplicarPropagacionConstantes() {
        java.util.Map<String, String> constantes = new java.util.HashMap<>();
        int cambios = 0;
        List<InstruccionTAC> resultado = new ArrayList<>();

        for (InstruccionTAC inst : codigoOptimizado) {
            // Limpiar constantes al toparse con una etiqueta
            if (inst.getTipo() == InstruccionTAC.Tipo.ETIQUETA) {
                constantes.clear();
                resultado.add(inst);
                continue;
            }

            if (inst.getTipo() == InstruccionTAC.Tipo.ASIGNACION) {
                String op1 = inst.getOperando1();
                String op2 = inst.getOperando2();
                
                boolean modificado = false;
                if (op1 != null && constantes.containsKey(op1)) {
                    op1 = constantes.get(op1);
                    modificado = true;
                }
                if (op2 != null && constantes.containsKey(op2)) {
                    op2 = constantes.get(op2);
                    modificado = true;
                }

                if (modificado) {
                    inst = new InstruccionTAC(InstruccionTAC.Tipo.ASIGNACION, inst.getDestino(), op1, inst.getOperador(), op2);
                    cambios++;
                }

                // Actualizar el estado de la variable destino
                if (inst.getOperador() == null && UtilsOperaciones.esEntero(inst.getOperando1())) {
                    constantes.put(inst.getDestino(), inst.getOperando1());
                } else {
                    constantes.remove(inst.getDestino());
                }
            } else if (inst.getTipo() == InstruccionTAC.Tipo.PARAM || inst.getTipo() == InstruccionTAC.Tipo.RETURN || inst.getTipo() == InstruccionTAC.Tipo.SALTO_CONDICIONAL) {
                // Propagar también a params, returns y condicionales
                String op1 = inst.getOperando1();
                String op2 = inst.getOperando2();
                boolean modificado = false;
                if (op1 != null && constantes.containsKey(op1)) { op1 = constantes.get(op1); modificado = true; }
                if (op2 != null && constantes.containsKey(op2)) { op2 = constantes.get(op2); modificado = true; }
                if (modificado) {
                    if (inst.getTipo() == InstruccionTAC.Tipo.SALTO_CONDICIONAL) {
                        inst = InstruccionTAC.saltoCondicional(op1, inst.getOperador(), op2, inst.getDestino());
                    } else {
                        inst = new InstruccionTAC(inst.getTipo(), inst.getDestino(), op1, inst.getOperador(), op2);
                    }
                    cambios++;
                }
            }

            resultado.add(inst);
        }

        codigoOptimizado = resultado;
        optimizaciones += cambios;
        System.out.println("  [Propagación] " + cambios + " variables propagadas como constantes");
    }

    private void eliminarAsignacionesRedundantes() {
        List<InstruccionTAC> resultado = new ArrayList<>();
        java.util.Map<String, String> ultimasAsignaciones = new java.util.HashMap<>();
        int cambios = 0;

        for (InstruccionTAC inst : codigoOptimizado) {
            if (inst.getTipo() == InstruccionTAC.Tipo.ASIGNACION && inst.getOperador() == null) {
                String var = inst.getDestino();
                String valor = inst.getOperando1();
                
                if (var.equals(valor) || (ultimasAsignaciones.containsKey(var) && ultimasAsignaciones.get(var).equals(valor))) {
                    cambios++;
                    continue;
                }
                ultimasAsignaciones.put(var, valor);
            } else if (inst.getTipo() == InstruccionTAC.Tipo.ASIGNACION) {
                ultimasAsignaciones.remove(inst.getDestino());
            }
            resultado.add(inst);
        }

        codigoOptimizado = resultado;
        optimizaciones += cambios;
        System.out.println("  [Redundantes] " + cambios + " asignaciones redundantes eliminadas");
    }
}