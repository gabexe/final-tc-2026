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
        // Al terminar, revisamos variables globales no usadas
        tablaSimbolos.checkGlobalScope();
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
            boolean ok = tablaSimbolos.define(name, type, category, dec);
            if (!ok) {
                tablaSimbolos.addError("Variable '" + name + "' duplicada en el mismo ámbito.", dec);
            }
            if (dec.expresion() != null) {
                String tipoExpr = visit(dec.expresion());
                if (!TablaSimbolos.esCompatibleAsignacion(type, tipoExpr)) {
                    tablaSimbolos.addError("No se puede asignar un valor de tipo '" + tipoExpr + "' a una variable de tipo '" + type + "'.", dec.expresion());
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
        boolean ok = tablaSimbolos.define(name, type, "funcion", ctx);
        if (!ok) {
            tablaSimbolos.addError("Función '" + name + "' duplicada.", ctx);
        }
        tablaSimbolos.enterScope(); 
        String prevFunctionType = currentFunctionType;
        currentFunctionType = type;
        
        TablaSimbolos.Symbol funcSym = tablaSimbolos.resolve(name);
        if (funcSym != null && name.equals("main")) {
            funcSym.used = true; // main siempre se considera usada
        }

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
        boolean ok = tablaSimbolos.define(name, type, category, ctx);
        if (!ok) {
            tablaSimbolos.addError("Parámetro '" + name + "' duplicado.", ctx);
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
                    tablaSimbolos.addError("Una función 'void' no puede retornar un valor.", ctx);
                } else if (!"void".equals(currentFunctionType)) {
                    if (ctx.expresion() == null) {
                        tablaSimbolos.addError("La función debe retornar un valor de tipo '" + currentFunctionType + "'.", ctx);
                    } else if (!TablaSimbolos.esCompatibleAsignacion(currentFunctionType, tipoRetorno)) {
                        tablaSimbolos.addError("El tipo de retorno '" + tipoRetorno + "' no coincide con el tipo de la función '" + currentFunctionType + "'.", ctx);
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
            tablaSimbolos.addError("Variable '" + name + "' no declarada.", ctx);
            int idx = ctx.LBRACKET() != null ? 1 : 0;
            if (ctx.expresion().size() > idx) visit(ctx.expresion(idx));
            return null;
        } else {
            sym.initialized = true;
            sym.used = true;
            String tipoVar = sym.type;
            int exprIndex = 0;
            if (ctx.LBRACKET() != null) {
                if (!"arreglo".equals(sym.category)) {
                    tablaSimbolos.addError("La variable '" + name + "' no es un arreglo.", ctx);
                }
                String tipoIndice = visit(ctx.expresion(0));
                if (!"int".equals(tipoIndice) && !"error".equals(tipoIndice)) {
                    tablaSimbolos.addError("El índice de un arreglo debe ser de tipo entero.", ctx.expresion(0));
                }
                exprIndex = 1;
            } else {
                if ("funcion".equals(sym.category)) {
                    tablaSimbolos.addError("No se puede asignar un valor a la función '" + name + "'.", ctx);
                }
            }
            
            if (ctx.expresion().size() > exprIndex) {
                String tipoExpr = visit(ctx.expresion(exprIndex));
                if (!TablaSimbolos.esCompatibleAsignacion(tipoVar, tipoExpr)) {
                    tablaSimbolos.addError("Tipos incompatibles: no se puede asignar '" + tipoExpr + "' a '" + tipoVar + "'.", ctx.expresion(exprIndex));
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
        if (sym == null) {
            tablaSimbolos.addError("Función '" + name + "' no declarada.", ctx);
            return "error";
        } else if (!"funcion".equals(sym.category)) {
            tablaSimbolos.addError("El identificador '" + name + "' no es una función.", ctx);
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
            if (!TablaSimbolos.esNumerico(tipoCond) && !"bool".equals(tipoCond) && !"error".equals(tipoCond)) {
                tablaSimbolos.addError("La condición del 'if' debe ser numérica o booleana.", ctx.expresion());
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
                if (!TablaSimbolos.esNumerico(tipoCond) && !"bool".equals(tipoCond) && !"error".equals(tipoCond)) {
                    tablaSimbolos.addError("La condición del 'while' debe ser numérica o booleana.", ctx.expresion(0));
                }
            }
            if (ctx.bloque() != null) visit(ctx.bloque());
        } else if (ctx.FOR() != null) {
            tablaSimbolos.enterScope();
            if (ctx.asignacion() != null) visit(ctx.asignacion());
            else if (ctx.declaracion() != null) visit(ctx.declaracion());
            
            if (ctx.expresion() != null && ctx.expresion().size() > 0 && ctx.expresion(0) != null) {
                String tipoCond = visit(ctx.expresion(0));
                if (!TablaSimbolos.esNumerico(tipoCond) && !"bool".equals(tipoCond) && !"error".equals(tipoCond)) {
                    tablaSimbolos.addError("La condición del 'for' debe ser numérica o booleana.", ctx.expresion(0));
                }
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
                if ("error".equals(tipoIzq) || "error".equals(tipoDer)) return "error";
                if (!TablaSimbolos.esNumerico(tipoIzq) || !TablaSimbolos.esNumerico(tipoDer)) {
                    tablaSimbolos.addError("Operación aritmética requiere operandos numéricos.", ctx);
                    return "error";
                }
                if ("double".equals(tipoIzq) || "double".equals(tipoDer)) return "double";
                return "int";
            } else {
                if ("error".equals(tipoIzq) || "error".equals(tipoDer)) return "bool";
                if (!TablaSimbolos.esNumerico(tipoIzq) || !TablaSimbolos.esNumerico(tipoDer)) {
                    if (! ("bool".equals(tipoIzq) && "bool".equals(tipoDer) && (op.equals("==") || op.equals("!=")))) {
                        tablaSimbolos.addError("Operación relacional requiere operandos compatibles.", ctx);
                    }
                }
                return "bool";
            }
        } else if (ctx.ID() != null && ctx.LBRACKET() == null) {
            String name = ctx.ID().getText();
            TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
            if (sym == null) {
                tablaSimbolos.addError("Variable '" + name + "' no declarada.", ctx);
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
                tablaSimbolos.addError("Variable '" + name + "' no declarada.", ctx);
                return "error";
            }
            if (!"arreglo".equals(sym.category)) {
                tablaSimbolos.addError("La variable '" + name + "' no es un arreglo.", ctx);
                return "error";
            }
            sym.used = true;
            if (ctx.expresion() != null && ctx.expresion().size() > 0) {
                String tipoIndice = visit(ctx.expresion(0));
                if (!"int".equals(tipoIndice) && !"error".equals(tipoIndice)) {
                    tablaSimbolos.addError("El índice de un arreglo debe ser de tipo entero.", ctx.expresion(0));
                }
            }
            return sym.type;
        } else if (ctx.LPAREN() != null) {
            if (ctx.expresion() != null && ctx.expresion().size() > 0) return visit(ctx.expresion(0));
        }
        return "void";
    }
}