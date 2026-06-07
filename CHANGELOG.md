# Changelog

## 07/06/2026

### Added:

#### 1. Nueva Clase: [AnalizadorSemantico.java]
- **Implementación del patrón Visitor**: Se crea un analizador semántico (`MiLenguajeBaseVisitor<String>`) que aplica Traducción Dirigida por la Sintaxis con atributos sintetizados (flujo bottom-up).
- **Verificación de Tipos**: Se validan operaciones aritméticas, relacionales, compatibilidad en asignaciones y tipos de retorno en funciones.
- **Verificación de Ámbito**: Se comprueba la correcta resolución de identificadores (variables y funciones) asegurando que estén declarados en el scope correspondiente antes de su uso.
- **Manejo Silencioso de Errores**: Se acumulan las anomalías (incompatibilidades, variables fuera de ámbito) en un contador interno, cumpliendo estrictamente con la restricción de NO reportar detalles específicos (línea, columna, nombre de variable) ni distinguir entre errores críticos y warnings.

### Changed:

#### [TablaSimbolos.java]
- Se agregaron métodos para el control y consulta de errores semánticos (`addErrorSemantico()`, `hayErroresSemanticos()`, `getCantidadErroresSemanticos()`).
- Se incorporaron métodos estáticos auxiliares para la evaluación de compatibilidad de tipos (`esNumerico()`, `esCompatibleAsignacion()`).

#### [App.java]
- **Refactorización del Flujo Semántico**: Se reemplazó el uso del `SimbolosListener` (con `ParseTreeWalker`) por la instanciación y ejecución del nuevo `AnalizadorSemantico` (Visitor).
- **Corrección de Scope**: Se movió la declaración de la variable `tablaSimbolos` fuera del bloque condicional de errores léxicos/sintácticos para solucionar el error de compilación y hacerla accesible en el resumen final.
- **Resumen de Errores Unificado**: Se actualizó el bloque `--- Resumen de Errores ---` para incluir la impresión de la cantidad total de errores semánticos detectados por el Visitor y los duplicados de la Tabla de Símbolos.

### Verificación y Pruebas Realizadas
1. Programa Correcto (`src/test/ejemplo_correcto.cpp`)
- Se ejecutó la compilación integrando el nuevo Visitor Semántico.
- **Resultados**: El analizador valida todas las operaciones, asignaciones y ámbitos correctamente sin sumar errores al contador. El resumen final muestra `Errores semánticos: 0` y el mensaje de `¡Compilación exitosa! No se encontraron errores léxicos, sintácticos ni semánticos.`.

2. Programa con Errores (`src/test/ejemplo_con_errores.cpp`)
- Se ejecutó la compilación para validar la detección de anomalías bajo las nuevas restricciones.
- **Resultados**: 
  - Se detectan silenciosamente errores de ámbito como asignaciones a variables no declaradas (`variableFantasma`, `w`, `variableFinal`) e intentos de asignación a funciones (`miFuncion = 10;`).
  - El resumen de la Tabla de Símbolos continúa mostrando correctamente las 2 declaraciones duplicadas.
  - El bloque `--- Resumen de Errores ---` muestra la suma total de errores semánticos acumulados.
  - La compilación se marca como fallida (`Se detectaron errores semánticos. Compilación fallida.`) sin emitir ningún detalle específico por consola, respetando las restricciones de diseño de la sesión.

---

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