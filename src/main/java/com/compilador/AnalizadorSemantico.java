package com.compilador;

import com.compilador.MiLenguajeParser.*;
import org.antlr.v4.runtime.tree.ParseTree;

public class AnalizadorSemantico extends MiLenguajeBaseVisitor<String> {
    private final TablaSimbolos tablaSimbolos;
    private String currentFunctionType = null;

    public AnalizadorSemantico(TablaSimbolos tablaSimbolos) {
        this.tablaSimbolos = tablaSimbolos;
    }

    public boolean hayErrores() {
        return tablaSimbolos.hayErroresSemanticos();
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
    public String visitDeclaracion(DeclaracionContext ctx) {
        if (ctx.tipo() == null) return null;
        String type = ctx.tipo().getText();
        for (DeclaradorContext dec : ctx.declarador()) {
            if (dec.ID() == null) continue;
            String name = dec.ID().getText();
            String category = (dec.LBRACKET() != null) ? "arreglo" : "variable";
            
            boolean ok = tablaSimbolos.define(name, type, category);
            if (!ok) {
                //tablaSimbolos.addErrorSemantico(); // Duplicada en el mismo scope
                // El duplicateCount de la Tabla de Símbolos ya lo está registrando correctamente.
            }

            if (dec.expresion() != null) {
                String tipoExpr = visit(dec.expresion());
                if (!TablaSimbolos.esCompatibleAsignacion(type, tipoExpr)) {
                    tablaSimbolos.addErrorSemantico(); // Incompatibilidad de tipos
                }
                TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
                if (sym != null) sym.initialized = true;
            }
        }
        return null;
    }

    @Override
    public String visitFuncion(FuncionContext ctx) {
        if (ctx.ID() == null) return null;
        String name = ctx.ID().getText();
        String type = (ctx.tipo() != null) ? ctx.tipo().getText() : "void";
        
        boolean ok = tablaSimbolos.define(name, type, "funcion");
        if (!ok) {
            tablaSimbolos.addErrorSemantico();
        }

        tablaSimbolos.enterScope(); // Nuevo ámbito para la función
        
        String prevFunctionType = currentFunctionType;
        currentFunctionType = type;

        if (ctx.parametros() != null) visit(ctx.parametros());
        visit(ctx.bloque());
        
        currentFunctionType = prevFunctionType;
        tablaSimbolos.exitScope();
        return null;
    }

    @Override
    public String visitParametros(ParametrosContext ctx) {
        if (ctx.parametro() != null) {
            for (ParametroContext p : ctx.parametro()) visit(p);
        }
        return null;
    }

    @Override
    public String visitParametro(ParametroContext ctx) {
        if (ctx.ID() == null || ctx.tipo() == null) return null;
        String name = ctx.ID().getText();
        String type = ctx.tipo().getText();
        String category = (ctx.LBRACKET() != null) ? "arreglo" : "parametro";
        
        boolean ok = tablaSimbolos.define(name, type, category);
        if (!ok) {
            tablaSimbolos.addErrorSemantico();
        } else {
            TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
            if (sym != null) sym.initialized = true;
        }
        return null;
    }

    @Override
    public String visitBloque(BloqueContext ctx) {
        tablaSimbolos.enterScope();
        if (ctx.sentencia() != null) {
            for (SentenciaContext s : ctx.sentencia()) visit(s);
        }
        tablaSimbolos.exitScope();
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
            String tipoRetorno = "void";
            if (ctx.expresion() != null) tipoRetorno = visit(ctx.expresion());
            
            if (currentFunctionType != null) {
                if ("void".equals(currentFunctionType) && ctx.expresion() != null) {
                    tablaSimbolos.addErrorSemantico();
                } else if (!"void".equals(currentFunctionType)) {
                    if (ctx.expresion() == null || !TablaSimbolos.esCompatibleAsignacion(currentFunctionType, tipoRetorno)) {
                        tablaSimbolos.addErrorSemantico();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String visitAsignacion(AsignacionContext ctx) {
        if (ctx.ID() == null) return null;
        String name = ctx.ID().getText();
        TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
        
        if (sym == null) {
            tablaSimbolos.addErrorSemantico(); // Fuera de ámbito o no declarada
        } else {
            sym.initialized = true;
            String tipoVar = sym.type;
            int exprIndex = 0;
            
            if (ctx.LBRACKET() != null) {
                String tipoIndice = visit(ctx.expresion(0));
                if (!"int".equals(tipoIndice)) tablaSimbolos.addErrorSemantico();
                exprIndex = 1;
            }
            
            if (ctx.expresion().size() > exprIndex) {
                String tipoExpr = visit(ctx.expresion(exprIndex));
                if (!TablaSimbolos.esCompatibleAsignacion(tipoVar, tipoExpr)) {
                    tablaSimbolos.addErrorSemantico();
                }
            }
        }
        return null;
    }

    @Override
    public String visitLlamadaFuncion(LlamadaFuncionContext ctx) {
        if (ctx.ID() == null) return "error";
        String name = ctx.ID().getText();
        TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
        
        if (sym == null || !"funcion".equals(sym.category)) {
            tablaSimbolos.addErrorSemantico(); // No declarada o no es función
            return "error";
        } else {
            sym.used = true;
        }
        
        if (ctx.argumentos() != null && ctx.argumentos().expresion() != null) {
            for (ExpresionContext e : ctx.argumentos().expresion()) visit(e);
        }
        return sym.type;
    }

    @Override
    public String visitSeleccion(SeleccionContext ctx) {
        if (ctx.expresion() != null) {
            String tipoCond = visit(ctx.expresion());
            if (!TablaSimbolos.esNumerico(tipoCond) && !"bool".equals(tipoCond)) {
                tablaSimbolos.addErrorSemantico();
            }
        }
        if (ctx.bloque() != null && ctx.bloque().size() > 0) visit(ctx.bloque(0));
        if (ctx.ELSE() != null && ctx.bloque().size() > 1) visit(ctx.bloque(1));
        return null;
    }

    @Override
    public String visitIteracion(IteracionContext ctx) {
        if (ctx.WHILE() != null) {
            if (ctx.expresion() != null && ctx.expresion().size() > 0) {
                String tipoCond = visit(ctx.expresion(0));
                if (!TablaSimbolos.esNumerico(tipoCond) && !"bool".equals(tipoCond)) tablaSimbolos.addErrorSemantico();
            }
            if (ctx.bloque() != null) visit(ctx.bloque());
        } else if (ctx.FOR() != null) {
            tablaSimbolos.enterScope(); 
            if (ctx.asignacion() != null) visit(ctx.asignacion());
            else if (ctx.declaracion() != null) visit(ctx.declaracion());
            
            if (ctx.expresion() != null && ctx.expresion().size() > 0 && ctx.expresion(0) != null) {
                 String tipoCond = visit(ctx.expresion(0));
                 if (!TablaSimbolos.esNumerico(tipoCond) && !"bool".equals(tipoCond)) tablaSimbolos.addErrorSemantico();
            }
            if (ctx.expresion() != null && ctx.expresion().size() > 1 && ctx.expresion(1) != null) {
                 visit(ctx.expresion(1)); 
            }
            if (ctx.bloque() != null) visit(ctx.bloque());
            tablaSimbolos.exitScope();
        }
        return null;
    }

    @Override
    public String visitExpresion(ExpresionContext ctx) {
        if (ctx.op != null) {
            String tipoIzq = visit(ctx.expresion(0));
            String tipoDer = visit(ctx.expresion(1));
            String op = ctx.op.getText();
            
            if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%")) {
                if (!TablaSimbolos.esNumerico(tipoIzq) || !TablaSimbolos.esNumerico(tipoDer)) {
                    tablaSimbolos.addErrorSemantico();
                    return "error";
                }
                if ("double".equals(tipoIzq) || "double".equals(tipoDer)) return "double";
                return "int";
            } else {
                if (!TablaSimbolos.esNumerico(tipoIzq) || !TablaSimbolos.esNumerico(tipoDer)) {
                     tablaSimbolos.addErrorSemantico();
                }
                return "bool";
            }
        } else if (ctx.ID() != null && ctx.LBRACKET() == null) {
            String name = ctx.ID().getText();
            TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
            if (sym == null) {
                tablaSimbolos.addErrorSemantico(); // Variable fuera de ámbito
                return "error";
            }
            sym.used = true;
            return sym.type;
        } else if (ctx.INT_LITERAL() != null) {
            return "int";
        } else if (ctx.DOUBLE_LITERAL() != null) {
            return "double";
        } else if (ctx.CHAR_LITERAL() != null) {
            return "char";
        } else if (ctx.STRING_LITERAL() != null) {
            return "string";
        } else if (ctx.llamadaFuncion() != null) {
            return visit(ctx.llamadaFuncion());
        } else if (ctx.ID() != null && ctx.LBRACKET() != null) {
            String name = ctx.ID().getText();
            TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
            if (sym == null) {
                tablaSimbolos.addErrorSemantico();
                return "error";
            }
            if (!"arreglo".equals(sym.category)) tablaSimbolos.addErrorSemantico();
            sym.used = true;
            if (ctx.expresion() != null && ctx.expresion().size() > 0) {
                String tipoIndice = visit(ctx.expresion(0));
                if (!"int".equals(tipoIndice)) tablaSimbolos.addErrorSemantico();
            }
            return sym.type; 
        } else if (ctx.LPAREN() != null) {
            if (ctx.expresion() != null && ctx.expresion().size() > 0) return visit(ctx.expresion(0));
        }
        return "void";
    }
}