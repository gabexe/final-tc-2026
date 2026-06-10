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

            tablaSimbolos.define(name, type, category, dec);

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

        tablaSimbolos.define(name, type, "funcion", ctx);

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

        tablaSimbolos.define(name, type, category, ctx);

        TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
        if (sym != null) {
            sym.initialized = true;
        }
    }

    @Override
    public void enterBloque(BloqueContext ctx) {
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
        if (ctx.ID() != null) {
            String name = ctx.ID().getText();
            TablaSimbolos.Symbol sym = tablaSimbolos.resolve(name);
            if (sym != null) {
                sym.used = true;
            }
        }
    }
}
