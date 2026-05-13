grammar MiLenguaje;

// Reglas del Parser (Sintáctico)
programa: (declaracion | funcion | sentencia)* EOF ;

declaracion
	: tipo declarador (COMMA declarador)* SEMI
	;

declarador
	: ID (LBRACKET NUMBER RBRACKET)?
	;

funcion
	: tipo ID LPAREN parametros? RPAREN bloque
	| VOID ID LPAREN parametros? RPAREN bloque
	;

parametros
	: parametro (COMMA parametro)*
	;

parametro
	: tipo ID (LBRACKET RBRACKET)?
	;

sentencia
	: declaracion
	| asignacion
	| llamadaFuncion SEMI
	| bloque
	| seleccion
	| iteracion
	| RETURN expresion? SEMI
	;

asignacion
	: (ID | ID LBRACKET expresion RBRACKET) ASSIGN expresion SEMI
	;

llamadaFuncion
	: ID LPAREN argumentos? RPAREN
	;

argumentos
	: expresion (COMMA expresion)*
	;

bloque
	: LBRACE sentencia* RBRACE
	;

seleccion
	: IF LPAREN expresion RPAREN bloque (ELSE bloque)?
	;

iteracion
	: WHILE LPAREN expresion RPAREN bloque
	| FOR LPAREN (asignacion | declaracion |) expresion? SEMI expresion? RPAREN bloque
	;

expresion
	: expresion op=('*'|'/'|'%') expresion
	| expresion op=('+'|'-') expresion
	| expresion op=('>'|'<'|'>='|'<='|'=='|'!=') expresion
	| ID
	| NUMBER
	| CHAR_LITERAL
	| STRING_LITERAL
	| llamadaFuncion
	| ID LBRACKET expresion RBRACKET
	| LPAREN expresion RPAREN
	;

tipo
	: INT
	| DOUBLE
	| CHAR
	| BOOL
	| STRING
	;

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

// Números decimales y enteros
NUMBER: [0-9]+ ('.' [0-9]+)?;

// Caracteres
CHAR_LITERAL: '\'' (ESC_SEQ | ~['\\\r\n]) '\'';

// Cadenas
STRING_LITERAL: '"' (ESC_SEQ | ~["\\\r\n])* '"';

fragment ESC_SEQ: '\\' [btnr'"\\];

// Operadores aritméticos
ASSIGN: '=';
PLUS: '+';
MINUS: '-';
STAR: '*';
DIV: '/';
MOD: '%';

// Operadores relacionales
GT: '>';
LT: '<';
GE: '>=';
LE: '<=';
EQ: '==';
NEQ: '!=';

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
