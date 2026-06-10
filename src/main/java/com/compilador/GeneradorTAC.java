package com.compilador;

import com.compilador.MiLenguajeParser.*;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.ArrayList;
import java.util.List;

public class GeneradorTAC extends MiLenguajeBaseVisitor<String> {
    private int tempCount = 0;
    private int labelCount = 0;
    private List<String> codigo;

    public GeneradorTAC() {
        this.codigo = new ArrayList<>();
    }

    private String newTemp() {
        return "t" + (tempCount++);
    }

    private String newLabel() {
        return "L" + (labelCount++);
    }

    public void emit(String instruction) {
        codigo.add(instruction);
    }

    public void printTAC() {
        System.out.println("\n--- Código de Tres Direcciones (TAC) ---");
        for (String instr : codigo) {
            System.out.println(instr);
        }
        System.out.println("----------------------------------------\n");
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
        emit("\n" + ctx.ID().getText() + ":");
        if (ctx.parametros() != null) {
            for (ParametroContext p : ctx.parametros().parametro()) {
                emit("param " + p.ID().getText());
            }
        }
        visit(ctx.bloque());
        return null;
    }

    @Override
    public String visitDeclaracion(DeclaracionContext ctx) {
        if (ctx.declarador() != null) {
            for (DeclaradorContext dec : ctx.declarador()) {
                if (dec.ASSIGN() != null && dec.expresion() != null) {
                    String exprVal = visit(dec.expresion());
                    emit(dec.ID().getText() + " = " + exprVal);
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
                emit("return " + retVal);
            } else {
                emit("return");
            }
        }
        return null;
    }

    @Override
    public String visitAsignacion(AsignacionContext ctx) {
        if (ctx.ID() == null) return null;
        int exprIndex = (ctx.LBRACKET() != null) ? 1 : 0;
        if (ctx.expresion().size() > exprIndex) {
            String exprVal = visit(ctx.expresion(exprIndex));
            if (ctx.LBRACKET() != null) {
                String index = visit(ctx.expresion(0));
                emit(ctx.ID().getText() + "[" + index + "] = " + exprVal);
            } else {
                emit(ctx.ID().getText() + " = " + exprVal);
            }
        }
        return null;
    }

    @Override
    public String visitSeleccion(SeleccionContext ctx) {
        String cond = visit(ctx.expresion());
        String lTrue = newLabel();
        String lFalse = newLabel();
        String lEnd = (ctx.ELSE() != null) ? newLabel() : lFalse;

        emit("if " + cond + " != 0 goto " + lTrue);
        emit("goto " + lFalse);

        emit(lTrue + ":");
        visit(ctx.bloque(0));

        if (ctx.ELSE() != null) {
            emit("goto " + lEnd);
            emit(lFalse + ":");
            visit(ctx.bloque(1));
            emit(lEnd + ":");
        } else {
            emit(lFalse + ":");
        }
        return null;
    }

        @Override
    public String visitIteracion(IteracionContext ctx) {
        if (ctx.WHILE() != null) {
            String lStart = newLabel();
            String lTrue = newLabel();
            String lFalse = newLabel();

            emit(lStart + ":");
            // [CORRECCIÓN] Usamos el índice (0) para obtener el ExpresionContext de la Lista
            String cond = visit(ctx.expresion(0)); 
            emit("if " + cond + " != 0 goto " + lTrue);
            emit("goto " + lFalse);

            emit(lTrue + ":");
            visit(ctx.bloque());
            emit("goto " + lStart);

            emit(lFalse + ":");
        } else if (ctx.FOR() != null) {
            if (ctx.asignacion() != null) visit(ctx.asignacion());
            else if (ctx.declaracion() != null) visit(ctx.declaracion());

            String lStart = newLabel();
            String lTrue = newLabel();
            String lFalse = newLabel();
            String lUpdate = newLabel();

            emit(lStart + ":");
            if (ctx.expresion().size() > 0 && ctx.expresion(0) != null) {
                String cond = visit(ctx.expresion(0));
                emit("if " + cond + " != 0 goto " + lTrue);
                emit("goto " + lFalse);
            } else {
                emit("goto " + lTrue);
            }

            emit(lTrue + ":");
            visit(ctx.bloque());

            emit(lUpdate + ":");
            if (ctx.expresion().size() > 1 && ctx.expresion(1) != null) {
                visit(ctx.expresion(1));
            }
            emit("goto " + lStart);

            emit(lFalse + ":");
        }
        return null;
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
            emit("param " + arg);
        }
        String temp = newTemp();
        emit(temp + " = call " + ctx.ID().getText() + ", " + args.size());
        return temp;
    }

    @Override
    public String visitExpresion(ExpresionContext ctx) {
        if (ctx.op != null) {
            String left = visit(ctx.expresion(0));
            String right = visit(ctx.expresion(1));
            String op = ctx.op.getText();

            // 1. Expresiones Aritméticas
            if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%")) {
                String temp = newTemp();
                emit(temp + " = " + left + " " + op + " " + right);
                return temp;
            } 
            // 2. Expresiones Lógicas / Relacionales
            else {
                String temp = newTemp();
                String lTrue = newLabel();
                String lEnd = newLabel();
                emit("if " + left + " " + op + " " + right + " goto " + lTrue);
                emit(temp + " = 0");
                emit("goto " + lEnd);
                emit(lTrue + ":");
                emit(temp + " = 1");
                emit(lEnd + ":");
                return temp;
            }
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
            emit(temp + " = " + ctx.ID().getText() + "[" + index + "]");
            return temp;
        } else if (ctx.LPAREN() != null) {
            return visit(ctx.expresion(0));
        }
        return "0";
    }
}