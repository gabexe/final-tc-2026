// #include <iostream>

int main() {
    // --- ERROR LÉXICO ---
    // El símbolo '@' no existe en el alfabeto de C++ para nombrar variables.
    int cuenta@Usuario = 100; 

    // --- ERROR SINTÁCTICO ---
    // Falta el punto y coma (;) al final de la línea.
    int suma = 10 + 5

    // --- ERROR LÉXICO ---
    // Los identificadores no pueden empezar con números. 
    // El compilador no sabe cómo procesar "2variable".
    int 2variable = 10;

    // --- ERROR SINTÁCTICO ---
    // La estructura del 'if' está mal formada (falta cerrar el paréntesis).
    if (suma > 10 {
        std::cout << "Es mayor" << std::endl;
    }

    // --- ERROR SINTÁCTICO ---
    // Falta cerrar la llave de la función main.
    return 0;
// Aquí debería ir una '}' pero la omitimos.