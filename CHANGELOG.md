## 26-06-2026 - Interfaz Web, Soporte de Control de Flujo y Refactorizaciones

### Added
1. **Interfaz Web (Web UI): [WebUI.java]**
   - Se implementó un servidor HTTP nativo y una interfaz gráfica interactiva para facilitar la compilación desde el navegador.
   - Se agregaron mejoras visuales a la interfaz: posición del cursor en tiempo real, números de línea, botón de copia de resultados y animaciones de scroll automático.
2. **Soporte Avanzado de Control de Flujo:**
   - Se implementó soporte completo para las sentencias `break` y `continue` dentro de los bucles iterativos, gestionando los saltos mediante pilas de etiquetas en el Generador TAC.

### Changed
1. **Refactorización del Modelo TAC:**
   - Se migró la representación del Código de Tres Direcciones de *Strings* planos a un modelo orientado a objetos (`InstruccionTAC`). Se actualizaron los módulos de optimización para consumir listas de estos objetos.
2. **Refactorización Arquitectónica (SOLID & DRY):**
   - Se extrajo la orquestación del pipeline a la nueva clase `Compilador.java`, liberando a `App.java` de responsabilidades excesivas (SRP).
   - Se modularizó el `AnalizadorSemantico`, eliminando *magic strings* y centralizando las constantes en la clase `TiposLenguaje.java` (DRY).
3. **Reportes Visuales (Colores ANSI):**
   - Se implementaron códigos de color ANSI en `App.java`, `TablaSimbolos.java` y `ManejadorErrores.java` para destacar visualmente resultados exitosos (verde), advertencias (amarillo) y errores críticos (rojo) en la terminal.

---
## 20-06-2026 - Actividad 6 - Parte 2: Reportes Visuales con Colores ANSI
### Added
1. Reportes Visuales (Colores ANSI):
  - Se implementó un sistema de reporte visual utilizando códigos de color ANSI para diferenciar claramente el estado de la compilación en la terminal. Los errores críticos (léxicos, sintácticos y semánticos) se muestran en color ROJO, las advertencias semánticas (variables no usadas) se muestran en color AMARILLO, y los mensajes de éxito se muestran en color VERDE.
### Changed
1. [ManejadorErrores.java]
  - Se agregaron las constantes estáticas `RESET`, `ROJO` y `VERDE` con los códigos de escape ANSI correspondientes. Se modificó el método `syntaxError()` para imprimir los errores léxicos y sintácticos en color rojo. Se actualizó el método `imprimirResumen()` para mostrar el mensaje de éxito en verde cuando no se detectan errores, o en rojo cuando existen errores.
2. [TablaSimbolos.java]
  - Se agregaron las constantes estáticas `RESET`, `ROJO`, `VERDE` y `AMARILLO` con los códigos de escape ANSI correspondientes. Se modificó el método `printDiagnostics()` para imprimir los errores semánticos en color rojo (por `stderr`), las advertencias en color amarillo (por `stdout`), y el mensaje de éxito en verde cuando no existen anomalías semánticas. El resumen final también utiliza colores para destacar la cantidad de errores y warnings.
3. [App.java]
  - Se agregaron las constantes estáticas `RESET` y `VERDE` con los códigos de escape ANSI. Se modificó el mensaje final de compilación exitosa para mostrarse en color verde, brindando una confirmación visual inmediata del éxito del pipeline de compilación.
### Verificación y Pruebas Realizadas
1. Programa Correcto (`src/test/ejemplo_correcto.cpp`)
  - Se ejecutó la compilación para validar la integración del sistema de colores.
  - Resultados:
	- El mensaje final de compilación exitosa se muestra correctamente en color verde.
	- El warning de la variable global `activo` (declarada pero no usada) se muestra en color amarillo.
	- No se generan mensajes en rojo al no existir errores críticos en el código fuente.
1. Programa con Errores (`src/test/ejemplo_con_errores.cpp`)
  - Se ejecutó la compilación para validar la diferenciación visual de errores y advertencias.
  - Resultados:
	- Todos los errores sintácticos y semánticos (variables duplicadas, no declaradas, tipos incompatibles) se muestran claramente en color rojo.
	- Las advertencias de variables no utilizadas se muestran en color amarillo, permitiendo distinguirlas fácilmente de los errores críticos.
	- El resumen final muestra en rojo la cantidad de errores detectados, facilitando la identificación rápida del estado de la compilación.

---
## 20-06-2026 - Actividad 6 - Parte 1: Salidas del Compilador e Incorporación de IA/Agentes
### Added
1. Nueva Interfaz: [OptimizationAgent.java]
  - Definición del Contrato para Agentes de Optimización: Se crea la interfaz base `OptimizationAgent` con el método `optimize(ASTNode node)` que deberán implementar todos los agentes inteligentes del sistema, estableciendo un contrato común para la aplicación de transformaciones sobre el código.
2. Nuevo Agente IA sobre TAC: [AgenteOptimizadorTAC.java]
  - Implementación de Agente Inteligente sobre Código Intermedio: Se desarrolla un agente basado en reglas que opera directamente sobre la lista de instrucciones TAC, aplicando tres técnicas de optimización de forma secuencial:
	  - Constant Folding Avanzado: Detecta y pliega operaciones aritméticas entre literales enteros mediante expresiones regulares, reemplazando instrucciones como `t10 = 40 + 2` por `t10 = 42` en tiempo de compilación.
	  - Propagación de Constantes: Identifica variables que almacenan valores constantes y propaga dichos valores en las instrucciones posteriores donde se utilizan, eliminando referencias innecesarias a variables intermedias.
	  - Eliminación de Asignaciones Redundantes: Detecta y descarta asignaciones triviales del tipo `x = x` o reasignaciones idénticas consecutivas que no aportan valor semántico al programa.
3. Nuevo Clasificador ML: [DetectorVariablesInnecesarias.java]
  - Agente de Machine Learning para Detección de Variables Muertas: Se implementa un clasificador basado en árbol de decisión que extrae características del AST para identificar variables candidatas a eliminación. El agente analiza tres atributos por cada variable declarada: si su valor inicial es un literal, el conteo total de usos en el programa, y aplica la regla aprendida "eliminar si es literal y tiene ≤ 1 uso". Incluye reporte detallado en consola con el nombre, valor y cantidad de usos de cada variable eliminada.
4. Nueva Clase Utilitaria: [GeneradorArchivos.java]
  - Generación Persistente de Salidas del Compilador: Se implementa un módulo dedicado a la escritura de archivos físicos que crea automáticamente una carpeta `output/` en la raíz del proyecto y genera dos archivos por cada compilación exitosa:
	  - `[nombre_archivo].tac`: Contiene el código intermedio de tres direcciones sin optimizar.
	  - `[nombre_archivo].opt`: Contiene el código TAC final después de aplicar el pipeline completo de agentes IA y optimizador clásico.
### Changed
1. [App.java]
  - Integración del Pipeline de Agentes IA: Se reestructura el flujo de compilación exitosa para incorporar la nueva Fase 4.5 de optimización inteligente. El nuevo orden de ejecución es: Generación TAC → Agente IA sobre TAC → Detector ML sobre AST → Optimizador Clásico → Generación de Archivos de Salida.
2. Nuevos Bloques de Salida Formateada: Se agregan los separadores visuales `--- Fase 4.5: Agentes IA sobre el TAC ---` y `--- Fase 6: Generación de Archivos de Salida ---` para distinguir claramente cada etapa del pipeline en la consola.
### Verificación y Pruebas Realizadas
1. Programa Correcto (`src/test/ejemplo_correcto.cpp`)
  - Se ejecutó la compilación completa para validar la integración de los nuevos módulos.
  - **Resultados**:
	- El Agente IA sobre TAC detectó y aplicó 2 optimizaciones (1 operación plegada + 1 propagación de constante).
	- El Detector ML analizó el AST correctamente sin eliminar variables legítimas del programa de prueba.
	- Se generaron exitosamente los archivos `output/ejemplo_correcto.tac` y `output/ejemplo_correcto.opt` con el código intermedio antes y después de las optimizaciones.
	- El pipeline completo mantiene la integridad semántica del programa mientras reduce instrucciones redundantes.
2. Programa con Errores (`src/test/ejemplo_con_errores.cpp`)
  - Se ejecutó la compilación para validar la protección del nuevo pipeline.
  - **Resultados**:
	- Al detectarse errores semánticos en fases previas, la compilación se aborta antes de llegar a los agentes IA.
	- No se generan archivos de salida en la carpeta `output/` cuando el código fuente contiene errores críticos.
	- El comportamiento es consistente con la teoría de compiladores: las fases de optimización y síntesis solo se ejecutan sobre código validado semánticamente.

---
## 18-06-26 - Optimización de Código Intermedio

### Added
1. **Nueva Clase: [Optimizador.java]**
   - **Implementación de la Fase de Optimización de Código:** Se crea una nueva clase encargada de aplicar optimizaciones locales al Código de Tres Direcciones (TAC) generado, basándose en las técnicas descritas en el material teórico de Generación y Optimización de Código.
   - **Constant Folding (Plegado de Constantes):** Se evalúan en tiempo de compilación todas las expresiones aritméticas cuyos operandos sean constantes enteras (ej. `t0 = 40 + 2` se transforma en `t0 = 42`), evitando cálculos innecesarios en tiempo de ejecución.
   - **Common Subexpression Elimination (CSE):** Se detectan expresiones matemáticas idénticas calculadas previamente y se reutiliza el temporal que ya almacena su resultado (ej. dos asignaciones `t1 = a + 3` y `t2 = a + 3` se convierten en `t1 = a + 3` y `t2 = t1`), eliminando redundancias.
   - **Dead Code Elimination (Eliminación de Código Muerto):** Se identifican los temporales cuyo valor nunca es leído en instrucciones posteriores y se descartan las asignaciones correspondientes, limpiando el TAC de instrucciones inútiles.
   - **Manejo de Invalidación de Expresiones:** El análisis CSE detecta reasignaciones de variables y limpia automáticamente las expresiones dependientes del mapa de subexpresiones, garantizando la corrección semántica del código optimizado.
2. **Integración en: [App.java]**
   - **Nueva Fase 5 - Optimización de Código:** Se incorpora una fase adicional al pipeline de compilación que solo se ejecuta tras superar exitosamente el análisis léxico, sintáctico, semántico y la generación de TAC. Se obtiene el código intermedio sin optimizar, se aplica la cadena de optimizaciones y se imprime el TAC resultante.
   - **Salida Formateada:** Se agregan los bloques `--- Fase 5: Optimización de Código ---` y `--- Código TAC Optimizado ---` para visualizar claramente las transformaciones aplicadas al código intermedio.

### Changed
1. **[GeneradorTAC.java]**
   - **Nuevo Método Público `getCodigo()`:** Se expone la lista interna de instrucciones TAC generadas para que la nueva fase de optimización pueda consumirla sin necesidad de re-parsear el código fuente ni duplicar la lógica de generación.

### Verificación y Pruebas Realizadas
1. **Programa Correcto (`src/test/ejemplo_correcto.cpp`)**
   - Se ejecutó la compilación para validar la integración completa de la fase de optimización.
   - **Resultados:**
     - La nueva fase se ejecuta correctamente tras la generación del TAC, aplicando las tres técnicas implementadas en secuencia.
     - El pipeline mantiene su integridad: la optimización solo se ejecuta cuando no existen errores léxicos, sintácticos ni semánticos críticos.
     - Los conteos de operaciones eliminadas por cada técnica se imprimen correctamente en consola.
     - El código TAC optimizado mantiene la semántica original del programa mientras reduce instrucciones redundantes.
2. **Programa con Errores (`src/test/ejemplo_con_errores.cpp`)**
   - Se ejecutó la compilación para validar la protección de la nueva fase.
   - **Resultados:**
     - Al detectarse errores semánticos en fases previas, la compilación se aborta antes de llegar a la fase de optimización.
     - No se generan transformaciones sobre código intermedio inválido, preservando la coherencia del pipeline de compilación.

---

## 10/06/2026 - Código Intermedio Parte 2

### Added
1. **Mejoras en: [GeneradorTAC.java]**
   - **Traducción Directa de Condiciones (Backpatching Implícito):** Se implementó el método auxiliar `generarCondicion()` que traduce expresiones relacionales (ej. `a < b`) directamente a saltos condicionales (`if a < b goto L1`), eliminando la generación ineficiente de temporales booleanos intermedios.
   - **Soporte Completo de Bucles (`while` y `for`):** Se completó la lógica de iteración generando correctamente las etiquetas de inicio, condición, cuerpo, actualización y salida, asegurando que los bucles `for` manejen sus tres componentes de forma secuencial en TAC.
   - **Manejo Robusto de Funciones:** Se estandarizó la emisión de instrucciones `param` antes de cada `call`, y se añadió un `return` implícito al final de cada definición de función para garantizar un flujo de control seguro incluso si el código fuente no lo especifica explícitamente.

### Changed
1. **[GeneradorTAC.java]**
   - **Refactorización de `visitSeleccion`:** Se reemplazó la evaluación de condición por valor con la nueva llamada a `generarCondicion()`, optimizando el código generado para bloques `if-else`.
   - **Refactorización de `visitIteracion`:** Se estructuró la generación de etiquetas para `while` y `for` alineándola con los estándares de compiladores modernos, evitando saltos redundantes.
   - **Limpieza en `visitExpresion`:** Se delegó la lógica de condiciones complejas al nuevo método auxiliar, mejorando la legibilidad y mantenibilidad del visitor.

### Verificación y Pruebas Realizadas
1. **Programa Correcto (`src/test/ejemplo_correcto.cpp`)**
   - Se ejecutó la compilación para validar la optimización del código intermedio generado.
   - **Resultados:**
     - El bloque `if (estado > 0)` ahora genera saltos directos sin temporales booleanos innecesarios.
     - Las llamadas a funciones (`sumar`) emiten correctamente la secuencia `param` -> `call` -> asignación de retorno.
     - El TAC resultante es más compacto, legible y fiel a la teoría de traducción dirigida por la sintaxis.
     - La compilación continúa siendo exitosa con 0 errores y las advertencias esperadas.
2. **Integridad del Pipeline**
   - Se validó que la fase de generación de código intermedio solo se ejecuta tras superar exitosamente los análisis léxico, sintáctico y semántico.
   - Los programas con errores semánticos siguen abortando la compilación antes de llegar al generador TAC, evitando la producción de código intermedio inválido.

---

## 10/06/2026 - Código Intermedio Parte 1

### Added
1. **Nueva Clase: [GeneradorTAC.java]**
   - **Implementación del Generador de Código de Tres Direcciones (TAC):** Se crea un visitor (`MiLenguajeBaseVisitor<String>`) que traduce el AST a código intermedio independiente de la máquina.
   - **Manejo de Temporales y Etiquetas:** Se incorporan contadores automáticos (`t0, t1...` y `L0, L1...`) para gestionar resultados intermedios y puntos de salto en el flujo de control.
   - **Expresiones Aritméticas y Lógicas:** Se traducen operaciones binarias a instrucciones TAC simples. Las expresiones relacionales generan saltos condicionales (`if ... goto`) asignando 1 o 0 a un temporal según corresponda.
   - **Estructuras de Control:** Se implementa la generación de código para `if/else`, `while` y `for` utilizando etiquetas y saltos incondicionales (`goto`) para delimitar bloques verdaderos, falsos y de actualización.
   - **Soporte para Funciones y Arrays:** Se emiten instrucciones `param`, `call` y accesos a memoria con índices (`arr[index]`) para soportar llamadas a funciones y arreglos definidos en la gramática.

### Changed
1. **[App.java]**
   - **Integración de la Fase de Código Intermedio:** Se añade la instanciación y ejecución del `GeneradorTAC` dentro del bloque de compilación exitosa (solo si no hay errores semánticos), respetando el pipeline estándar de compilación.
   - **Salida Formateada:** Se agrega la impresión del bloque `--- Código de Tres Direcciones (TAC) ---` al finalizar la compilación válida para visualizar la traducción generada.

### Verificación y Pruebas Realizadas
1. **Programa Correcto (`src/test/ejemplo_correcto.cpp`)**
   - Se ejecutó la compilación para validar la generación de código intermedio.
   - **Resultados:**
     - El generador produce correctamente instrucciones TAC para declaraciones globales, funciones (`sumar`), operaciones aritméticas complejas, accesos a arrays (`numeros[i]`) y estructuras de control (`if`).
     - Los temporales y etiquetas se generan de forma secuencial y consistente.
     - La fase solo se ejecuta tras superar exitosamente los análisis léxico, sintáctico y semántico.
2. **Programa con Errores (`src/test/ejemplo_con_errores.cpp`)**
   - Se ejecutó la compilación para validar la protección de la fase de síntesis.
   - **Resultados:**
     - Al detectar errores semánticos (variables duplicadas, no declaradas, etc.), el compilador aborta antes de llegar al generador TAC.
     - No se genera código intermedio basura ni se producen excepciones por símbolos faltantes en la tabla.
     - El comportamiento es consistente con la teoría de compiladores: la síntesis solo ocurre si el análisis fue 100% exitoso.

---

## 10/06/2026 - Fix

### Fixed
1. **[SimbolosListener.java]**
   - Se corrigió la invocación a `TablaSimbolos.define()` para pasar el `ParserRuleContext` correspondiente, alineándola con la firma actual y eliminando el error de compilación.

---

## 09/06/2026 - Sistema de Diagnóstico Semántico Detallado

### Added
1. **Sistema de Diagnóstico Semántico Detallado: [TablaSimbolos.java]**
   - **Nueva Clase Interna `Diagnostic`:** Se implementó una estructura para almacenar la severidad (`ERROR` o `WARNING`), el mensaje descriptivo, y la ubicación exacta (línea y columna) de cada anomalía encontrada.
   - **Métodos de Reporte:** Se agregaron `addError()` y `addWarning()` que extraen automáticamente la línea y columna del `ParserRuleContext` de ANTLR.
   - **Detección Automática de Código Muerto:** Se incorporó lógica en `exitScope()` y el nuevo método `checkGlobalScope()` para emitir warnings automáticamente cuando una variable, parámetro o arreglo es declarado pero nunca utilizado.
   - **Impresión Formateada:** Nuevo método `printDiagnostics()` que ordena los reportes por línea/columna, imprime los errores por `stderr` (consola de errores) y los warnings por `stdout`, mostrando un resumen final de la cantidad de cada uno.

### Changed
1. **[AnalizadorSemantico.java]**
   - **Mensajes Descriptivos:** Se reemplazaron las llamadas genéricas a `addErrorSemantico()` por llamadas a `addError()` y `addWarning()` pasando el contexto (`ctx`) y un mensaje específico (ej. "Variable 'x' duplicada en el mismo ámbito", "El índice de un arreglo debe ser de tipo entero").
   - **Corrección de Falsos Positivos:** En `visitAsignacion()`, ahora las variables que reciben una asignación también se marcan con `used = true`. Esto evita warnings incorrectos de "variable no usada" cuando una variable global o local solo se escribe pero no se lee posteriormente.
   - **Validación de Variables Globales:** Al finalizar `visitPrograma()`, se invoca a `checkGlobalScope()` para auditar las variables del nivel 0.
2. **[App.java]**
   - **Reordenamiento del Flujo de Salida:** Se ajustó el orden de impresión para que primero se muestre el `--- Diagnóstico Semántico ---` (con sus errores y warnings) y a continuación la `--- Tabla de Símbolos ---`, brindando una lectura más lógica de los resultados de la compilación.
   - **Manejo de Warnings vs Errores:** La compilación ahora solo se marca como "fallida" si `hayErroresSemanticos()` detecta severidad `ERROR`. Los `WARNING` se informan pero permiten que el mensaje final sea `¡Compilación exitosa!`.
3. **[SimbolosListener.java]**
   - **Adaptación de Firmas:** Se actualizó el listener (aunque no se use en el flujo principal) para pasar el `ParserRuleContext` al método `define()` de la Tabla de Símbolos, asegurando que el proyecto compile correctamente sin romper la compatibilidad.
4. **[TablaSimbolos.java]**
   - **Firma de `define()`:** Se modificó para recibir el `ParserRuleContext` y poder guardar la referencia al nodo del AST, permitiendo reportar la línea y columna exacta si la variable queda sin usar al cerrar su scope.
   - **Deprecación de Contador Simple:** Se eliminó el antiguo contador entero `erroresSemanticos` en favor de la lista de objetos `Diagnostic`.

### Verificación y Pruebas Realizadas
1. **Programa Correcto (`src/test/ejemplo_correcto.cpp`)**
   - Se ejecutó la compilación para validar la nueva distinción entre errores y advertencias.
   - **Resultados:**
     - El diagnóstico semántico reporta correctamente `[WARNING]` para la variable global `activo` (declarada pero nunca usada).
     - Variables como `valorPi` e `inicial` ya no generan falsos positivos, ya que las asignaciones en `main` ahora las marcan correctamente como usadas.
     - El resumen final indica `0 error(es), 1 advertencia(s)` y la compilación se considera exitosa al no haber errores críticos.
     - El flujo de consola muestra primero el Diagnóstico y luego la Tabla de Símbolos.
2. **Programa con Errores (`src/test/ejemplo_con_errores.cpp`)**
   - Se ejecutó la compilación para validar el reporte detallado de anomalías críticas y no críticas.
   - **Resultados:**
     - Se emiten `[ERROR]` detallados con línea y columna para variables no declaradas (`variableFantasma`, `w`), variables duplicadas (`variableGlobal`, `variableLocal`) e intentos de asignación a funciones (`miFuncion = 10;`).
     - Se emiten `[WARNING]` precisos para las variables declaradas pero no utilizadas (`variableNoUsada1`, `variableNoUsada2`, `variableNoUsada3`, `z`).
     - El resumen final indica múltiples errores y advertencias, y la compilación se marca correctamente como fallida debido a la presencia de errores críticos.

---

## 06/06/2026 - Analizador Semántico

### Added
1. **Nueva Clase: [AnalizadorSemantico.java]**
   - **Implementación del patrón Visitor:** Se crea un analizador semántico (`MiLenguajeBaseVisitor<String>`) que aplica Traducción Dirigida por la Sintaxis con atributos sintetizados (flujo bottom-up).
   - **Verificación de Tipos:** Se validan operaciones aritméticas, relacionales, compatibilidad en asignaciones y tipos de retorno en funciones.
   - **Verificación de Ámbito:** Se comprueba la correcta resolución de identificadores (variables y funciones) asegurando que estén declarados en el scope correspondiente antes de su uso.
   - **Manejo Silencioso de Errores:** Se acumulan las anomalías (incompatibilidades, variables fuera de ámbito) en un contador interno, no reportando detalles específicos (línea, columna, nombre de variable) ni distinguiendo entre errores críticos y warnings.

### Changed
1. **[TablaSimbolos.java]**
   - Se agregaron métodos para el control y consulta de errores semánticos (`addErrorSemantico()`, `hayErroresSemanticos()`, `getCantidadErroresSemanticos()`).
   - Se incorporaron métodos estáticos auxiliares para la evaluación de compatibilidad de tipos (`esNumerico()`, `esCompatibleAsignacion()`).
2. **[App.java]**
   - **Refactorización del Flujo Semántico:** Se reemplazó el uso del `SimbolosListener` (con `ParseTreeWalker`) por la instanciación y ejecución del nuevo `AnalizadorSemantico` (Visitor).
   - **Corrección de Scope:** Se movió la declaración de la variable `tablaSimbolos` fuera del bloque condicional de errores léxicos/sintácticos para solucionar el error de compilación y hacerla accesible en el resumen final.
   - **Resumen de Errores Unificado:** Se actualizó el bloque `--- Resumen de Errores ---` para incluir la impresión de la cantidad total de errores semánticos detectados por el Visitor y los duplicados de la Tabla de Símbolos.

### Verificación y Pruebas Realizadas
1. **Programa Correcto (`src/test/ejemplo_correcto.cpp`)**
   - Se ejecutó la compilación integrando el nuevo Visitor Semántico.
   - **Resultados:** El analizador valida todas las operaciones, asignaciones y ámbitos correctamente sin sumar errores al contador. El resumen final muestra `Errores semánticos: 0` y el mensaje de `¡Compilación exitosa! No se encontraron errores léxicos, sintácticos ni semánticos.`.
2. **Programa con Errores (`src/test/ejemplo_con_errores.cpp`)**
   - Se ejecutó la compilación para validar la detección de anomalías bajo las nuevas restricciones.
   - **Resultados:**
     - Se detectan silenciosamente errores de ámbito como asignaciones a variables no declaradas (`variableFantasma`, `w`, `variableFinal`) e intentos de asignación a funciones (`miFuncion = 10;`).
     - El resumen de la Tabla de Símbolos continúa mostrando correctamente las 2 declaraciones duplicadas.
     - El bloque `--- Resumen de Errores ---` muestra la suma total de errores semánticos acumulados.
     - La compilación se marca como fallida (`Se detectaron errores semánticos. Compilación fallida.`) sin emitir ningún detalle específico por consola, respetando las restricciones de diseño de la sesión.

---

## 03/06/2026 - Tabla de Símbolos y Listener

### Added
1. **Nueva Clase: [TablaSimbolos.java]**
   - **Representación de Símbolos:** Se estructuró la clase interna `Symbol` para contener propiedades como `name`, `type`, `category` (variable, funcion, parametro, arreglo), `scopeLevel`, `used`, `initialized`, y `duplicate`.
   - **Estructura de Scopes:** Se implementó una lista de mapas (`List<Map<String, Symbol>>`) actuando como pila para mantener la jerarquía de ámbitos.
   - **Operaciones de Ámbito:** Se habilitaron `enterScope()` y `exitScope()`.
   - **Inserción y Resolución:**
     - `define()`: Agrega símbolos al ámbito activo. Si el símbolo ya está presente en el mismo ámbito, lo marca como `duplicate = true`.
     - `resolve()`: Recupera símbolos recorriendo los ámbitos activos desde el más interno al global.
   - **Formateo:** Se añadió `printTable()` para imprimir una representación tabular amigable y clara del estado de los símbolos al final de la compilación.
2. **Nuevo Listener: [SimbolosListener.java]**
   - Se extiende `MiLenguajeBaseListener` para poblar y actualizar la tabla de símbolos durante el recorrido del Parse Tree.
   - Se capturan las declaraciones de variables (`exitDeclaracion`), parámetros (`exitParametro`) y funciones (`enterFuncion`).
   - Se delimitan los ámbitos en las entradas/salidas de funciones (`enterFuncion`/`exitFuncion`) y de bloques de sentencias (`enterBloque`/`exitBloque`).
   - Se realiza seguimiento de uso e inicialización al salir de asignaciones (`exitAsignacion`), llamadas a funciones (`exitLlamadaFuncion`) y expresiones generales (`exitExpresion`).
3. **Integración en: [App.java]**
   - Tras la verificación exitosa de errores léxicos y sintácticos, se instancia la tabla de símbolos y el listener.
   - Se ejecuta el `ParseTreeWalker` para caminar el Parse Tree de ANTLR.
   - Se despliega la tabla resultante en consola llamando a `tablaSimbolos.printTable()`.

### Changed
1. **[App.java]**
   - Integración del análisis semántico en el flujo principal de compilación.
   - Instanciación de `TablaSimbolos` y `SimbolosListener` tras la generación y validación del AST.
   - Ejecución de `ParseTreeWalker` pasando el listener y el árbol sintáctico.
   - Impresión en consola de la tabla de símbolos generada mediante `tablaSimbolos.printTable()`.

### Verificación y Pruebas Realizadas
1. **Programa Correcto (`src/test/ejemplo_correcto.cpp`)**
   - Se ejecutó la compilación con: `mvn exec:java -Dexec.mainClass="com.compilador.App" -Dexec.args="src/test/ejemplo_correcto.cpp"`
   - **Resultados:**
     - Se construyó exitosamente la tabla con 13 símbolos distribuidos en 4 niveles de ámbito.
     - Nivel 0 (Global): `contadorGlobal`, `valorPi`, `inicial`, `activo`, `sumar`, `main`.
     - Nivel 1 (Parámetros de `sumar`): `a`, `b`.
     - Nivel 2 (Cuerpo de `sumar`/`main`): `resultado`, `estado`, `temp`, `numeros`.
     - Nivel 3 (Bloque condicional `if` en `main`): `auxiliar`.
     - Se detectó correctamente que `valorPi` e `inicial` fueron inicializados pero no usados.
2. **Programa con Errores (`src/test/ejemplo_con_errores.cpp`)**
   - Se ejecutó la compilación con: `mvn exec:java -Dexec.mainClass="com.compilador.App" -Dexec.args="src/test/ejemplo_con_errores.cpp"`
   - **Resultados:**
     - Se identificaron 2 declaraciones duplicadas: `variableGlobal` (en ámbito global) y `variableLocal` (en el ámbito local de `miFuncion`).
     - Se detectaron las variables declaradas pero no usadas (`variableNoUsada1`, `variableNoUsada2`, `variableNoUsada3`, `z`) como no inicializadas y no usadas.