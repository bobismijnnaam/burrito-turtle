lexer grammar BurritoVocab;

/**
 * Basic language building blocks
 */

RETURN: '<-';
START: '->';
LOCK: 'lock';
UNLOCK: 'unlock';
LENGTH: 'len';

PLUS: '+';
MIN: '-';
MUL: '*';
DIV: '/';
MOD: '%';
POW: '**';

EQ: '==';
LT: '<';
GT: '>';
LTE: '<=';
GTE: '>=';
NOT: '~';

ASS: '=';
ELSE: '!';
IF: '?';
WHILE: '@';
IO: '|';
IONL: '\\'; // This is just ONE backslash! Antlr wants each backslash escaped with backslash
SCOPE: '#';
END: '.';
SEMI: ';';
COLON: ':';
LPAR: '(';
RPAR: ')';
LBRA: '[';
RBRA: ']';

CHARACTER: '\'' (~'\'' | '\\' '\'') '\''; 

AND: '&&';
OR: '||';
XOR: '^';

INT: 'int';
BOOL: 'bool';
CHAR: 'char';
LOCKT: 'Lock';
VOID: 'void';
AUTO: '$';
LOCAL: '&';
GLOBAL: '*';
DEREF: '_';

TRUE: ('t' | 'T') 'rue';
FALSE: ('f' | 'F') 'alse';

NUM: [1-9] DIGIT* | '0';

ID: LETTER (LETTER | DIGIT)*;

COMMA: ',';

ONELINECOMMENT: '>>' (~('\r'|'\n'))* -> skip;
MULTILINECOMMENT: '{' .*? '}' -> skip; 
WS: [ \t\r\n]+ -> skip;

fragment LETTER: [a-zA-Z];
fragment DIGIT: [0-9];
