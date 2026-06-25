package com.compilador;

import com.compilador.MiLenguajeParser.*;
import org.antlr.v4.runtime.tree.ParseTree;

public class AnalizadorSemantico extends MiLenguajeBaseVisitor<String> {
    private final TablaSimbolos tablaSimbolos;
    private String currentFunctionType = null;
    private int loopDepth = 0;

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
            String category = (dec.LBRACKET() != null) ? TiposLenguaje.CAT_ARREGLO : TiposLenguaje.CAT_VARIABLE;
            
            boolean registrado = tablaSimbolos.define(name, type, category, dec);
            if (!registrado) {
                tablaSimbolos.addError("Variable '" + name + "' duplicada en el mismo ámbito.", dec);
            }
            
            if (dec.expresion() != null) {
                validarAsignacionEnDeclaracion(type, dec);
            }
        }
        return null;
    }

    private void validarAsignacionEnDeclaracion(String type, DeclaradorContext dec) {
        String tipoExpr = visit(dec.expresion());
        if (!TiposLenguaje.esCompatibleAsignacion(type, tipoExpr)) {
            tablaSimbolos.addError("No se puede asignar un valor de tipo '" + tipoExpr + "' a una variable de tipo '" + type + "'.", dec.expresion());
        }
        TablaSimbolos.Symbol sym = tablaSimbolos.resolve(dec.ID().getText());
        if (sym != null) sym.initialized = true;
    }

    @Override
    public String visitFuncion(FuncionContext ctx) {
        if (ctx.ID() == null) return null;
        
        String name = ctx.ID().getText();
        String type = (ctx.tipo() != null) ? ctx.tipo().getText() : TiposLenguaje.VOID;
        
        boolean registrado = tablaSimbolos.define(name, type, TiposLenguaje.CAT_FUNCION, ctx);
        if (!registrado) {
            tablaSimbolos.addError("Función '" + name + "' duplicada.", ctx);
        }
        
        tablaSimbolos.enterScope(); 
        String prevFunctionType = currentFunctionType;
        currentFunctionType = type;
        
        TablaSimbolos.Symbol funcSym = tablaSimbolos.resolve(name);
        if (funcSym != null && name.equals("main")) {
            funcSym.used = true; // main se considera siempre usada
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
        String category = (ctx.LBRACKET() != null) ? TiposLenguaje.CAT_ARREGLO : TiposLenguaje.CAT_PARAMETRO;
        
        boolean registrado = tablaSimbolos.define(name, type, category, ctx);
        if (!registrado) {
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
        else if (ctx.RETURN() != null) validarRetorno(ctx);
        else if (ctx.BREAK() != null) {
            if (loopDepth == 0) tablaSimbolos.addError("'break' solo puede usarse dentro de un bucle.", ctx);
        }
        else if (ctx.CONTINUE() != null) {
            if (loopDepth == 0) tablaSimbolos.addError("'continue' solo puede usarse dentro de un bucle.", ctx);
        }
        return null;
    }

    private void validarRetorno(SentenciaContext ctx) {
        String tipoRetorno = TiposLenguaje.VOID;
        if (ctx.expresion() != null) tipoRetorno = visit(ctx.expresion());
        
        if (currentFunctionType != null) {
            if (TiposLenguaje.VOID.equals(currentFunctionType) && ctx.expresion() != null) {
                tablaSimbolos.addError("Una función 'void' no puede retornar un valor.", ctx);
            } else if (!TiposLenguaje.VOID.equals(currentFunctionType)) {
                if (ctx.expresion() == null) {
                    tablaSimbolos.addError("La función debe retornar un valor de tipo '" + currentFunctionType + "'.", ctx);
                } else if (!TiposLenguaje.esCompatibleAsignacion(currentFunctionType, tipoRetorno)) {
                    tablaSimbolos.addError("El tipo de retorno '" + tipoRetorno + "' no coincide con el tipo de la función '" + currentFunctionType + "'.", ctx);
                }
            }
        }
    }

    @Override
    public String visitAsignacion(AsignacionContext ctx) {
        if (ctx.ID() == null) return null;
        String name = ctx.ID().getText();
        TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
        
        if (sym == null) {
            tablaSimbolos.addError("Variable '" + name + "' no declarada.", ctx);
            evaluarExpresionesFallback(ctx);
            return null;
        }
        
        sym.initialized = true;
        sym.used = true;
        
        int exprIndex = validarCategoriaAsignacion(ctx, sym, name);
        if (ctx.expresion().size() > exprIndex) {
            String tipoExpr = visit(ctx.expresion(exprIndex));
            if (!TiposLenguaje.esCompatibleAsignacion(sym.type, tipoExpr)) {
                tablaSimbolos.addError("Tipos incompatibles: no se puede asignar '" + tipoExpr + "' a '" + sym.type + "'.", ctx.expresion(exprIndex));
            }
        }
        return null;
    }

    private void evaluarExpresionesFallback(AsignacionContext ctx) {
        int idx = ctx.LBRACKET() != null ? 1 : 0;
        if (ctx.expresion().size() > idx) visit(ctx.expresion(idx));
    }

    private int validarCategoriaAsignacion(AsignacionContext ctx, TablaSimbolos.Symbol sym, String name) {
        int exprIndex = 0;
        if (ctx.LBRACKET() != null) {
            if (!TiposLenguaje.CAT_ARREGLO.equals(sym.category)) {
                tablaSimbolos.addError("La variable '" + name + "' no es un arreglo.", ctx);
            }
            String tipoIndice = visit(ctx.expresion(0));
            if (!TiposLenguaje.INT.equals(tipoIndice) && !TiposLenguaje.ERROR.equals(tipoIndice)) {
                tablaSimbolos.addError("El índice de un arreglo debe ser de tipo entero.", ctx.expresion(0));
            }
            exprIndex = 1;
        } else if (TiposLenguaje.CAT_FUNCION.equals(sym.category)) {
            tablaSimbolos.addError("No se puede asignar un valor a la función '" + name + "'.", ctx);
        }
        return exprIndex;
    }

    @Override
    public String visitLlamadaFuncion(LlamadaFuncionContext ctx) {
        if (ctx.ID() == null) return TiposLenguaje.ERROR;
        String name = ctx.ID().getText();
        TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
        
        if (sym == null) {
            tablaSimbolos.addError("Función '" + name + "' no declarada.", ctx);
            return TiposLenguaje.ERROR;
        } else if (!TiposLenguaje.CAT_FUNCION.equals(sym.category)) {
            tablaSimbolos.addError("El identificador '" + name + "' no es una función.", ctx);
            return TiposLenguaje.ERROR;
        }
        
        sym.used = true;
        if (ctx.argumentos() != null && ctx.argumentos().expresion() != null) {
            for (ExpresionContext e : ctx.argumentos().expresion()) visit(e);
        }
        return sym.type;
    }

    @Override
    public String visitSeleccion(SeleccionContext ctx) {
        if (ctx.expresion() != null) {
            validarCondicionBooleana(visit(ctx.expresion()), ctx.expresion(), "del 'if'");
        }
        if (ctx.bloque() != null && ctx.bloque().size() > 0) visit(ctx.bloque(0));
        if (ctx.ELSE() != null && ctx.bloque().size() > 1) visit(ctx.bloque(1));
        return null;
    }

    @Override
    public String visitIteracion(IteracionContext ctx) {
        loopDepth++;
        if (ctx.WHILE() != null) {
            if (ctx.expresion() != null && ctx.expresion().size() > 0) {
                validarCondicionBooleana(visit(ctx.expresion(0)), ctx.expresion(0), "del 'while'");
            }
            if (ctx.bloque() != null) visit(ctx.bloque());
        } else if (ctx.FOR() != null) {
            tablaSimbolos.enterScope();
            if (ctx.asignacion() != null) visit(ctx.asignacion());
            else if (ctx.declaracion() != null) visit(ctx.declaracion());
            
            if (ctx.expresion() != null && ctx.expresion().size() > 0 && ctx.expresion(0) != null) {
                validarCondicionBooleana(visit(ctx.expresion(0)), ctx.expresion(0), "del 'for'");
            }
            if (ctx.expresion() != null && ctx.expresion().size() > 1 && ctx.expresion(1) != null) {
                visit(ctx.expresion(1));
            }
            if (ctx.bloque() != null) visit(ctx.bloque());
            tablaSimbolos.exitScope();
        }
        loopDepth--;
        return null;
    }

    private void validarCondicionBooleana(String tipoCond, ExpresionContext ctx, String contextoStr) {
        if (!TiposLenguaje.esNumerico(tipoCond) && !TiposLenguaje.BOOL.equals(tipoCond) && !TiposLenguaje.ERROR.equals(tipoCond)) {
            tablaSimbolos.addError("La condición " + contextoStr + " debe ser numérica o booleana.", ctx);
        }
    }

    @Override
    public String visitExpresion(ExpresionContext ctx) {
        if (ctx.op != null) {
            return evaluarOperacionBinaria(ctx);
        } else if (ctx.ID() != null && ctx.LBRACKET() == null) {
            return evaluarIdentificador(ctx);
        } else if (ctx.INT_LITERAL() != null) {
            return TiposLenguaje.INT;
        } else if (ctx.DOUBLE_LITERAL() != null) {
            return TiposLenguaje.DOUBLE;
        } else if (ctx.CHAR_LITERAL() != null) {
            return TiposLenguaje.CHAR;
        } else if (ctx.STRING_LITERAL() != null) {
            return TiposLenguaje.STRING;
        } else if (ctx.llamadaFuncion() != null) {
            return visit(ctx.llamadaFuncion());
        } else if (ctx.ID() != null && ctx.LBRACKET() != null) {
            return evaluarAccesoArreglo(ctx);
        } else if (ctx.LPAREN() != null && ctx.expresion() != null && ctx.expresion().size() > 0) {
            return visit(ctx.expresion(0));
        }
        return TiposLenguaje.VOID;
    }

    private String evaluarOperacionBinaria(ExpresionContext ctx) {
        String tipoIzq = visit(ctx.expresion(0));
        String tipoDer = visit(ctx.expresion(1));
        String op = ctx.op.getText();
        
        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%")) {
            if (TiposLenguaje.ERROR.equals(tipoIzq) || TiposLenguaje.ERROR.equals(tipoDer)) return TiposLenguaje.ERROR;
            if (!TiposLenguaje.esNumerico(tipoIzq) || !TiposLenguaje.esNumerico(tipoDer)) {
                tablaSimbolos.addError("Operación aritmética requiere operandos numéricos.", ctx);
                return TiposLenguaje.ERROR;
            }
            if (TiposLenguaje.DOUBLE.equals(tipoIzq) || TiposLenguaje.DOUBLE.equals(tipoDer)) return TiposLenguaje.DOUBLE;
            return TiposLenguaje.INT;
        } else {
            if (TiposLenguaje.ERROR.equals(tipoIzq) || TiposLenguaje.ERROR.equals(tipoDer)) return TiposLenguaje.BOOL;
            if (!TiposLenguaje.esNumerico(tipoIzq) || !TiposLenguaje.esNumerico(tipoDer)) {
                if (!(TiposLenguaje.BOOL.equals(tipoIzq) && TiposLenguaje.BOOL.equals(tipoDer) && (op.equals("==") || op.equals("!=")))) {
                    tablaSimbolos.addError("Operación relacional requiere operandos compatibles.", ctx);
                }
            }
            return TiposLenguaje.BOOL;
        }
    }

    private String evaluarIdentificador(ExpresionContext ctx) {
        String name = ctx.ID().getText();
        TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
        if (sym == null) {
            tablaSimbolos.addError("Variable '" + name + "' no declarada.", ctx);
            return TiposLenguaje.ERROR;
        }
        sym.used = true;
        return sym.type;
    }

    private String evaluarAccesoArreglo(ExpresionContext ctx) {
        String name = ctx.ID().getText();
        TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
        if (sym == null) {
            tablaSimbolos.addError("Variable '" + name + "' no declarada.", ctx);
            return TiposLenguaje.ERROR;
        }
        if (!TiposLenguaje.CAT_ARREGLO.equals(sym.category)) {
            tablaSimbolos.addError("La variable '" + name + "' no es un arreglo.", ctx);
            return TiposLenguaje.ERROR;
        }
        sym.used = true;
        if (ctx.expresion() != null && ctx.expresion().size() > 0) {
            String tipoIndice = visit(ctx.expresion(0));
            if (!TiposLenguaje.INT.equals(tipoIndice) && !TiposLenguaje.ERROR.equals(tipoIndice)) {
                tablaSimbolos.addError("El índice de un arreglo debe ser de tipo entero.", ctx.expresion(0));
            }
        }
        return sym.type;
    }
}