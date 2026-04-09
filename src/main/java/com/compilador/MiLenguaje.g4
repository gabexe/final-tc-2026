grammar MiLenguaje;

programa: EOF ; // las reglas de Parser deben ir en minusculas -> eliminar cuando se añadan reglas, esto es solo una linea temporal para no romper el programa

// Tokens
INT: 'int';
DOUBLE: 'double';
CHAR: 'char';
BOOL: 'bool';
STRING: 'string';
VOID: 'void';
IF: 'if';
ELSE: 'else';
WHILE: 'while';
FOR: 'for';
RETURN: 'return';
TRUE: 'true';
FALSE: 'false';

// Identificadores
ID: [a-zA-Z_] [a-zA-Z_0-9]*;

// Numeros
NUMBER: [0-9]+;

// Operadores
ASSIGN: '=';
PLUS: '+';
MINUS: '-';
STAR: '*';
DIV: '/';
MOD: '%';

// Separadores
LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
LBRACKET: '[';
RBRACKET: ']';
SEMI: ';';
COMMA: ',';

// Manejo de espacios y comentarios
WS: [ \t\r\n]+ -> skip;
LINE_COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;
