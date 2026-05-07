## Como ejecutar (07-05-26)


```bash
# Ejecutar codigo sin errores
mvn clean compile && mvn exec:java -Dexec.mainClass="com.compilador.App" -Dexec.args="src/test/ejemplo_correcto.cpp"
```
```bash
# Ejecutar codigo con errores
mvn clean compile && mvn exec:java -Dexec.mainClass="com.compilador.App" -Dexec.args="src/test/ejemplo_con_errores.cpp"
```
```bash
# Limpiar los archivos compilados
mvn clean
```