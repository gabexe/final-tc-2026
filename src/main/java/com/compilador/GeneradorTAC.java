package com.compilador;

import com.compilador.MiLenguajeParser.*;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.ArrayList;
import java.util.List;

/**
 * Generador de Código de Tres Direcciones (TAC)
 */
public class GeneradorTAC extends MiLenguajeBaseVisitor<String> {
    private int tempCount = 0;
    private int labelCount = 0;
    private final List<InstruccionTAC> instrucciones;
    private final java.util.Stack<String> startLabels = new java.util.Stack<>();
    private final java.util.Stack<String> endLabels = new java.util.Stack<>();

    public GeneradorTAC() {
        this.instrucciones = new ArrayList<>();
    }

    private String newTemp() {
        return "t" + (tempCount++);
    }

    private String newLabel() {
        return "L" + (labelCount++);
    }

    private void emit(InstruccionTAC instruccion) {
        instrucciones.add(instruccion);
    }

    public List<InstruccionTAC> getInstrucciones() {
        return new ArrayList<>(instrucciones);
    }

    @Override
    public String visitPrograma(ProgramaContext ctx) {
        if (ctx.children != null) {
            for (ParseTree child : ctx.children) {
                visit(child);
            }
        }
        return null;
    }

    @Override
    public String visitFuncion(FuncionContext ctx) {
        if (ctx.ID() == null) return null;
        
        // Etiqueta de inicio de función
        emit(new InstruccionTAC(InstruccionTAC.Tipo.ETIQUETA, ctx.ID().getText()));
        
        visit(ctx.bloque());
        
        // Return implícito al final de la función
        emit(new InstruccionTAC(InstruccionTAC.Tipo.RETURN, null));
        return null;
    }

    @Override
    public String visitDeclaracion(DeclaracionContext ctx) {
        if (ctx.declarador() != null) {
            for (DeclaradorContext dec : ctx.declarador()) {
                if (dec.ASSIGN() != null && dec.expresion() != null) {
                    String exprVal = visit(dec.expresion());
                    emit(new InstruccionTAC(InstruccionTAC.Tipo.ASIGNACION, dec.ID().getText(), exprVal, null, null));
                }
            }
        }
        return null;
    }

    @Override
    public String visitBloque(BloqueContext ctx) {
        if (ctx.sentencia() != null) {
            for (SentenciaContext s : ctx.sentencia()) {
                visit(s);
            }
        }
        return null;
    }

    @Override
    public String visitSentencia(SentenciaContext ctx) {
        if (ctx.declaracion() != null) visit(ctx.declaracion());
        else if (ctx.asignacion() != null) visit(ctx.asignacion());
        else if (ctx.llamadaFuncion() != null) visit(ctx.llamadaFuncion());
        else if (ctx.bloque() != null) visit(ctx.bloque());
        else if (ctx.seleccion() != null) visit(ctx.seleccion());
        else if (ctx.iteracion() != null) visit(ctx.iteracion());
        else if (ctx.RETURN() != null) {
            if (ctx.expresion() != null) {
                String retVal = visit(ctx.expresion());
                emit(new InstruccionTAC(InstruccionTAC.Tipo.RETURN, retVal));
            } else {
                emit(new InstruccionTAC(InstruccionTAC.Tipo.RETURN, null));
            }
        }
        else if (ctx.BREAK() != null) {
            if (!endLabels.isEmpty()) emit(new InstruccionTAC(InstruccionTAC.Tipo.SALTO, endLabels.peek()));
        }
        else if (ctx.CONTINUE() != null) {
            if (!startLabels.isEmpty()) emit(new InstruccionTAC(InstruccionTAC.Tipo.SALTO, startLabels.peek()));
        }
        return null;
    }

    @Override
    public String visitAsignacion(AsignacionContext ctx) {
        if (ctx.ID() == null) return null;
        boolean esArreglo = ctx.LBRACKET() != null;
        int exprIndex = esArreglo ? 1 : 0;
        
        if (ctx.expresion().size() > exprIndex) {
            String exprVal = visit(ctx.expresion(exprIndex));
            if (esArreglo) {
                String index = visit(ctx.expresion(0));
                emit(new InstruccionTAC(InstruccionTAC.Tipo.ASIGNACION, ctx.ID().getText(), index, "[]=", exprVal));
            } else {
                emit(new InstruccionTAC(InstruccionTAC.Tipo.ASIGNACION, ctx.ID().getText(), exprVal, null, null));
            }
        }
        return null;
    }

    /**
     * Traduce condiciones directamente a saltos condicionales, evitando temporales booleanos
     */
    private void generarCondicion(ExpresionContext ctx, String lTrue, String lFalse) {
        if (ctx.op != null) {
            String op = ctx.op.getText();
            if (esOperadorRelacional(op)) {
                String left = visit(ctx.expresion(0));
                String right = visit(ctx.expresion(1));
                emit(InstruccionTAC.saltoCondicional(left, op, right, lTrue));
                emit(new InstruccionTAC(InstruccionTAC.Tipo.SALTO, lFalse));
                return;
            }
        }
        // Condición no relacional (ej. flag booleana)
        String val = visit(ctx);
        emit(InstruccionTAC.saltoCondicional(val, "!=", "0", lTrue));
        emit(new InstruccionTAC(InstruccionTAC.Tipo.SALTO, lFalse));
    }

    private boolean esOperadorRelacional(String op) {
        return op.equals(">") || op.equals("<") || op.equals(">=") || op.equals("<=") || op.equals("==") || op.equals("!=");
    }

    @Override
    public String visitSeleccion(SeleccionContext ctx) {
        String lTrue = newLabel();
        String lFalse = newLabel();
        String lEnd = (ctx.ELSE() != null) ? newLabel() : lFalse;
        
        generarCondicion(ctx.expresion(), lTrue, lFalse);
        
        emit(new InstruccionTAC(InstruccionTAC.Tipo.ETIQUETA, lTrue));
        visit(ctx.bloque(0));
        
        if (ctx.ELSE() != null) {
            emit(new InstruccionTAC(InstruccionTAC.Tipo.SALTO, lEnd));
            emit(new InstruccionTAC(InstruccionTAC.Tipo.ETIQUETA, lFalse));
            visit(ctx.bloque(1));
            emit(new InstruccionTAC(InstruccionTAC.Tipo.ETIQUETA, lEnd));
        } else {
            emit(new InstruccionTAC(InstruccionTAC.Tipo.ETIQUETA, lFalse));
        }
        return null;
    }

    @Override
    public String visitIteracion(IteracionContext ctx) {
        if (ctx.WHILE() != null) {
            generarWhileTAC(ctx);
        } else if (ctx.FOR() != null) {
            generarForTAC(ctx);
        }
        return null;
    }

    private void generarWhileTAC(IteracionContext ctx) {
        String lStart = newLabel();
        String lTrue = newLabel();
        String lFalse = newLabel();
        
        startLabels.push(lStart);
        endLabels.push(lFalse);

        emit(new InstruccionTAC(InstruccionTAC.Tipo.ETIQUETA, lStart));
        generarCondicion(ctx.expresion(0), lTrue, lFalse);
        
        emit(new InstruccionTAC(InstruccionTAC.Tipo.ETIQUETA, lTrue));
        visit(ctx.bloque());
        emit(new InstruccionTAC(InstruccionTAC.Tipo.SALTO, lStart));
        emit(new InstruccionTAC(InstruccionTAC.Tipo.ETIQUETA, lFalse));

        startLabels.pop();
        endLabels.pop();
    }

    private void generarForTAC(IteracionContext ctx) {
        if (ctx.asignacion() != null) visit(ctx.asignacion());
        else if (ctx.declaracion() != null) visit(ctx.declaracion());
        
        String lStart = newLabel();
        String lTrue = newLabel();
        String lFalse = newLabel();
        String lUpdate = newLabel();
        
        startLabels.push(lUpdate);
        endLabels.push(lFalse);
        
        emit(new InstruccionTAC(InstruccionTAC.Tipo.ETIQUETA, lStart));
        
        if (ctx.expresion().size() > 0 && ctx.expresion(0) != null) {
            generarCondicion(ctx.expresion(0), lTrue, lFalse);
        } else {
            emit(new InstruccionTAC(InstruccionTAC.Tipo.SALTO, lTrue));
        }
        
        emit(new InstruccionTAC(InstruccionTAC.Tipo.ETIQUETA, lTrue));
        visit(ctx.bloque());
        
        emit(new InstruccionTAC(InstruccionTAC.Tipo.ETIQUETA, lUpdate));
        if (ctx.expresion().size() > 1 && ctx.expresion(1) != null) {
            visit(ctx.expresion(1));
        }
        emit(new InstruccionTAC(InstruccionTAC.Tipo.SALTO, lStart));
        emit(new InstruccionTAC(InstruccionTAC.Tipo.ETIQUETA, lFalse));
        
        startLabels.pop();
        endLabels.pop();
    }

    @Override
    public String visitLlamadaFuncion(LlamadaFuncionContext ctx) {
        if (ctx.ID() == null) return "0";
        List<String> args = new ArrayList<>();
        if (ctx.argumentos() != null) {
            for (ExpresionContext e : ctx.argumentos().expresion()) {
                args.add(visit(e));
            }
        }
        for (String arg : args) {
            emit(new InstruccionTAC(InstruccionTAC.Tipo.PARAM, arg));
        }
        String temp = newTemp();
        emit(new InstruccionTAC(InstruccionTAC.Tipo.CALL, temp, ctx.ID().getText(), null, String.valueOf(args.size())));
        return temp;
    }

    @Override
    public String visitExpresion(ExpresionContext ctx) {
        if (ctx.op != null) {
            return procesarOperacion(ctx);
        } else if (ctx.ID() != null && ctx.LBRACKET() == null) {
            return ctx.ID().getText();
        } else if (ctx.INT_LITERAL() != null) {
            return ctx.INT_LITERAL().getText();
        } else if (ctx.DOUBLE_LITERAL() != null) {
            return ctx.DOUBLE_LITERAL().getText();
        } else if (ctx.CHAR_LITERAL() != null) {
            return ctx.CHAR_LITERAL().getText();
        } else if (ctx.STRING_LITERAL() != null) {
            return ctx.STRING_LITERAL().getText();
        } else if (ctx.llamadaFuncion() != null) {
            return visit(ctx.llamadaFuncion());
        } else if (ctx.ID() != null && ctx.LBRACKET() != null) {
            String index = visit(ctx.expresion(0));
            String temp = newTemp();
            emit(new InstruccionTAC(InstruccionTAC.Tipo.ASIGNACION, temp, ctx.ID().getText(), "[]", index));
            return temp;
        } else if (ctx.LPAREN() != null) {
            return visit(ctx.expresion(0));
        }
        return "0";
    }

    private String procesarOperacion(ExpresionContext ctx) {
        String left = visit(ctx.expresion(0));
        String right = visit(ctx.expresion(1));
        String op = ctx.op.getText();
        
        if (esOperadorAritmetico(op)) {
            String temp = newTemp();
            emit(new InstruccionTAC(InstruccionTAC.Tipo.ASIGNACION, temp, left, op, right));
            return temp;
        } else {
            // Evaluando expresión relacional/lógica como valor booleano (0 o 1)
            String temp = newTemp();
            String lTrue = newLabel();
            String lEnd = newLabel();
            emit(InstruccionTAC.saltoCondicional(left, op, right, lTrue));
            emit(new InstruccionTAC(InstruccionTAC.Tipo.ASIGNACION, temp, "0", null, null));
            emit(new InstruccionTAC(InstruccionTAC.Tipo.SALTO, lEnd));
            emit(new InstruccionTAC(InstruccionTAC.Tipo.ETIQUETA, lTrue));
            emit(new InstruccionTAC(InstruccionTAC.Tipo.ASIGNACION, temp, "1", null, null));
            emit(new InstruccionTAC(InstruccionTAC.Tipo.ETIQUETA, lEnd));
            return temp;
        }
    }

    private boolean esOperadorAritmetico(String op) {
        return op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%");
    }
}