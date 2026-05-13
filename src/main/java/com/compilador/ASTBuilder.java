package com.compilador;

import com.compilador.MiLenguajeParser.*;

public class ASTBuilder extends MiLenguajeBaseVisitor<ASTNode> {
    @Override
    public ASTNode visitPrograma(ProgramaContext ctx) {
        ASTNode root = new ASTNode("programa");
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ASTNode hijo = ctx.getChild(i).accept(this);
            if (hijo != null) root.add(hijo);
        }
        return root;
    }

    @Override
    public ASTNode visitDeclaracion(DeclaracionContext ctx) {
        ASTNode decl = new ASTNode("declaracion");
        decl.add(visit(ctx.tipo()));
        for (var dec : ctx.declarador()) decl.add(visit(dec));
        return decl;
    }

    @Override
    public ASTNode visitDeclarador(DeclaradorContext ctx) {
        ASTNode node = new ASTNode("declarador", ctx.ID().getText());
        if (ctx.LBRACKET() != null && ctx.INT_LITERAL() != null) {
            node.add(new ASTNode("arreglo", ctx.INT_LITERAL().getText()));
        }
        if (ctx.ASSIGN() != null) {
            ASTNode asig = new ASTNode("inicializacion");
            asig.add(visit(ctx.expresion()));
            node.add(asig);
        }
        return node;
    }

    @Override
    public ASTNode visitFuncion(FuncionContext ctx) {
        ASTNode fun = new ASTNode("funcion", ctx.ID().getText());
        fun.add(ctx.tipo() != null ? visit(ctx.tipo()) : new ASTNode("void"));
        if (ctx.parametros() != null) fun.add(visit(ctx.parametros()));
        fun.add(visit(ctx.bloque()));
        return fun;
    }

    @Override
    public ASTNode visitParametros(ParametrosContext ctx) {
        ASTNode params = new ASTNode("parametros");
        for (var p : ctx.parametro()) params.add(visit(p));
        return params;
    }

    @Override
    public ASTNode visitParametro(ParametroContext ctx) {
        ASTNode param = new ASTNode("parametro", ctx.ID().getText());
        param.add(visit(ctx.tipo()));
        if (ctx.LBRACKET() != null) param.add(new ASTNode("arreglo"));
        return param;
    }

    @Override
    public ASTNode visitSentencia(SentenciaContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ASTNode hijo = ctx.getChild(i).accept(this);
            if (hijo != null) return hijo;
        }
        return null;
    }

    @Override
    public ASTNode visitAsignacion(AsignacionContext ctx) {
        ASTNode asig = new ASTNode("asignacion");
        asig.add(new ASTNode("id", ctx.ID().getText()));
        if (ctx.LBRACKET() != null) asig.add(visit(ctx.expresion(0)));
        asig.add(visit(ctx.expresion(ctx.LBRACKET() != null ? 1 : 0)));
        return asig;
    }

    @Override
    public ASTNode visitLlamadaFuncion(LlamadaFuncionContext ctx) {
        ASTNode call = new ASTNode("llamadaFuncion", ctx.ID().getText());
        if (ctx.argumentos() != null) call.add(visit(ctx.argumentos()));
        return call;
    }

    @Override
    public ASTNode visitArgumentos(ArgumentosContext ctx) {
        ASTNode args = new ASTNode("argumentos");
        for (var e : ctx.expresion()) args.add(visit(e));
        return args;
    }

    @Override
    public ASTNode visitBloque(BloqueContext ctx) {
        ASTNode block = new ASTNode("bloque");
        for (var s : ctx.sentencia()) block.add(visit(s));
        return block;
    }

    @Override
    public ASTNode visitSeleccion(SeleccionContext ctx) {
        ASTNode sel = new ASTNode("if");
        sel.add(visit(ctx.expresion()));
        sel.add(visit(ctx.bloque(0)));
        if (ctx.ELSE() != null) sel.add(visit(ctx.bloque(1)));
        return sel;
    }

    @Override
    public ASTNode visitIteracion(IteracionContext ctx) {
        ASTNode it = new ASTNode(ctx.WHILE() != null ? "while" : "for");
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ASTNode hijo = ctx.getChild(i).accept(this);
            if (hijo != null) it.add(hijo);
        }
        return it;
    }

    @Override
    public ASTNode visitExpresion(ExpresionContext ctx) {
        if (ctx.op != null) {
            ASTNode op = new ASTNode("op", ctx.op.getText());
            op.add(visit(ctx.expresion(0)));
            op.add(visit(ctx.expresion(1)));
            return op;
        } else if (ctx.ID() != null && ctx.LBRACKET() == null) {
            return new ASTNode("id", ctx.ID().getText());
        } else if (ctx.INT_LITERAL() != null) {
            return new ASTNode("int", ctx.INT_LITERAL().getText());
        } else if (ctx.DOUBLE_LITERAL() != null) {
            return new ASTNode("double", ctx.DOUBLE_LITERAL().getText());
        } else if (ctx.CHAR_LITERAL() != null) {
            return new ASTNode("char", ctx.CHAR_LITERAL().getText());
        } else if (ctx.STRING_LITERAL() != null) {
            return new ASTNode("string", ctx.STRING_LITERAL().getText());
        } else if (ctx.llamadaFuncion() != null) {
            return visit(ctx.llamadaFuncion());
        } else if (ctx.ID() != null && ctx.LBRACKET() != null) {
            ASTNode arr = new ASTNode("arreglo", ctx.ID().getText());
            arr.add(visit(ctx.expresion(0)));
            return arr;
        } else if (ctx.LPAREN() != null) {
            return visit(ctx.expresion(0));
        }
        return null;
    }

    @Override
    public ASTNode visitTipo(TipoContext ctx) {
        return new ASTNode("tipo", ctx.getText());
    }
}
