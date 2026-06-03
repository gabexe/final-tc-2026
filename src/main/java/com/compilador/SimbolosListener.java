package com.compilador;

import com.compilador.MiLenguajeParser.*;

public class SimbolosListener extends MiLenguajeBaseListener {
    private final TablaSimbolos tablaSimbolos;

    public SimbolosListener(TablaSimbolos tablaSimbolos) {
        this.tablaSimbolos = tablaSimbolos;
    }

    @Override
    public void exitDeclaracion(DeclaracionContext ctx) {
        if (ctx.tipo() == null) return;
        String type = ctx.tipo().getText();
        for (DeclaradorContext dec : ctx.declarador()) {
            if (dec.ID() == null) continue;
            String name = dec.ID().getText();
            String category = (dec.LBRACKET() != null) ? "arreglo" : "variable";
            
            tablaSimbolos.define(name, type, category);
            
            // Si hay inicialización directa
            if (dec.ASSIGN() != null) {
                TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
                if (sym != null) {
                    sym.initialized = true;
                }
            }
        }
    }

    @Override
    public void enterFuncion(FuncionContext ctx) {
        if (ctx.ID() == null) return;
        String name = ctx.ID().getText();
        String type = (ctx.tipo() != null) ? ctx.tipo().getText() : "void";
        
        tablaSimbolos.define(name, type, "funcion");
        
        // Entrar al scope de la función (contendrá parámetros y cuerpo)
        tablaSimbolos.enterScope();
    }

    @Override
    public void exitFuncion(FuncionContext ctx) {
        tablaSimbolos.exitScope();
    }

    @Override
    public void exitParametro(ParametroContext ctx) {
        if (ctx.ID() == null || ctx.tipo() == null) return;
        String name = ctx.ID().getText();
        String type = ctx.tipo().getText();
        String category = (ctx.LBRACKET() != null) ? "arreglo" : "parametro";
        
        tablaSimbolos.define(name, type, category);
        
        // Los parámetros se consideran inicializados al entrar a la función
        TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
        if (sym != null) {
            sym.initialized = true;
        }
    }

    @Override
    public void enterBloque(BloqueContext ctx) {
        // En C++, los bloques entran en un nuevo ámbito de scope.
        // Si el bloque es el cuerpo directo de una función, ya entramos a un scope en enterFuncion.
        // Pero para simplificar y mantener la consistencia del stack de scopes,
        // entramos a un scope por cada bloque {}.
        tablaSimbolos.enterScope();
    }

    @Override
    public void exitBloque(BloqueContext ctx) {
        tablaSimbolos.exitScope();
    }

    @Override
    public void exitAsignacion(AsignacionContext ctx) {
        if (ctx.ID() == null) return;
        String name = ctx.ID().getText();
        TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
        if (sym != null) {
            sym.initialized = true;
        }
    }

    @Override
    public void exitLlamadaFuncion(LlamadaFuncionContext ctx) {
        if (ctx.ID() == null) return;
        String name = ctx.ID().getText();
        TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
        if (sym != null) {
            sym.used = true;
        }
    }

    @Override
    public void exitExpresion(ExpresionContext ctx) {
        // Si la expresión es simplemente un identificador de variable o acceso a arreglo
        if (ctx.ID() != null) {
            String name = ctx.ID().getText();
            TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
            if (sym != null) {
                sym.used = true;
            }
        }
    }
}
