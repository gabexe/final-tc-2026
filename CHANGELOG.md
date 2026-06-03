# Changelog

## 03/06/2026

### Added:

#### 1. Nueva Clase: [TablaSimbolos.java]
- **Representación de Símbolos**: Se estructuró la clase interna `Symbol` para contener propiedades como `name`, `type`, `category` (variable, funcion, parametro, arreglo), `scopeLevel`, `used`, `initialized`, y `duplicate`.
- **Estructura de Scopes**: Se implementó una lista de mapas (`List<Map<String, Symbol>>`) actuando como pila para mantener la jerarquía de ámbitos.
- **Operaciones de Ámbito**: Se habilitaron `enterScope()` y `exitScope()`.
- **Inserción y Resolución**:
  - `define()`: Agrega símbolos al ámbito activo. Si el símbolo ya está presente en el mismo ámbito, lo marca como `duplicate = true`.
  - `resolve()`: Recupera símbolos recorriendo los ámbitos activos desde el más interno al global.
- **Formateo**: Se añadió `printTable()` para imprimir una representación tabular amigable y clara del estado de los símbolos al final de la compilación.

#### 2. Nuevo Listener: [SimbolosListener.java]
- Se extiende `MiLenguajeBaseListener` para poblar y actualizar la tabla de símbolos durante el recorrido del Parse Tree.
- Se capturan las declaraciones de variables (`exitDeclaracion`), parámetros (`exitParametro`) y funciones (`enterFuncion`).
- Se delimitan los ámbitos en las entradas/salidas de funciones (`enterFuncion`/`exitFuncion`) y de bloques de sentencias (`enterBloque`/`exitBloque`).
- Se realiza seguimiento de uso e inicialización al salir de asignaciones (`exitAsignacion`), llamadas a funciones (`exitLlamadaFuncion`) y expresiones generales (`exitExpresion`).

#### 3. Integración en: [App.java]
- Tras la verificación exitosa de errores léxicos y sintácticos, se instancia la tabla de símbolos y el listener.
- Se ejecuta el `ParseTreeWalker` para caminar el Parse Tree de ANTLR.
- Se despliega la tabla resultante en consola llamando a `tablaSimbolos.printTable()`.

---

### Changed

#### [App.java]
- Integración del análisis semántico en el flujo principal de compilación.
- Instanciación de `TablaSimbolos` y `SimbolosListener` tras la generación y validación del AST.
- Ejecución de `ParseTreeWalker` pasando el listener y el árbol sintáctico.
- Impresión en consola de la tabla de símbolos generada mediante `tablaSimbolos.printTable()`.

---

### Verificación y Pruebas Realizadas

#### 1. Programa Correcto (`src/test/ejemplo_correcto.cpp`)
Se ejecutó la compilación con:
```bash
mvn exec:java -Dexec.mainClass="com.compilador.App" -Dexec.args="src/test/ejemplo_correcto.cpp"
```
##### Resultados:
Se construyó exitosamente la tabla con 13 símbolos distribuidos en 4 niveles de ámbito.
- Nivel 0 (Global): `contadorGlobal`, `valorPi`, `inicial`, `activo`, `sumar`, `main`.
- Nivel 1 (Parámetros de `sumar`): `a`, `b`.
- Nivel 2 (Cuerpo de `sumar`/`main`): `resultado`, `estado`, `temp`, `numeros`.
- Nivel 3 (Bloque condicional `if` en `main`): `auxiliar`.
- Se detectó correctamente que `valorPi` e `inicial` fueron inicializados pero no usados.

#### 2. Programa con Errores (`src/test/ejemplo_con_errores.cpp`)
Se ejecutó la compilación con:
```bash
mvn exec:java -Dexec.mainClass="com.compilador.App" -Dexec.args="src/test/ejemplo_con_errores.cpp"
```
##### Resultados:
- Se identificaron **2 declaraciones duplicadas**: `variableGlobal` (en ámbito global) y `variableLocal` (en el ámbito local de `miFuncion`).
- Se detectaron las variables declaradas pero no usadas (`variableNoUsada1`, `variableNoUsada2`, `variableNoUsada3`, `z`) como no inicializadas y no usadas.