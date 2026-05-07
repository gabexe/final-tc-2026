package com.compilador;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link MiLenguajeParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface MiLenguajeVisitor<T> extends ParseTreeVisitor<T> {
	T visitPrograma(MiLenguajeParser.ProgramaContext ctx);
	T visitDeclaracion(MiLenguajeParser.DeclaracionContext ctx);
	T visitDeclarador(MiLenguajeParser.DeclaradorContext ctx);
	T visitFuncion(MiLenguajeParser.FuncionContext ctx);
	T visitParametros(MiLenguajeParser.ParametrosContext ctx);
	T visitParametro(MiLenguajeParser.ParametroContext ctx);
	T visitSentencia(MiLenguajeParser.SentenciaContext ctx);
	T visitAsignacion(MiLenguajeParser.AsignacionContext ctx);
	T visitLlamadaFuncion(MiLenguajeParser.LlamadaFuncionContext ctx);
	T visitArgumentos(MiLenguajeParser.ArgumentosContext ctx);
	T visitBloque(MiLenguajeParser.BloqueContext ctx);
	T visitSeleccion(MiLenguajeParser.SeleccionContext ctx);
	T visitIteracion(MiLenguajeParser.IteracionContext ctx);
	T visitExpresion(MiLenguajeParser.ExpresionContext ctx);
	T visitTipo(MiLenguajeParser.TipoContext ctx);
}