lexer grammar BurritoVocab;

/**
 * Basic language building blocks
 */

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
LPAR: '(';
RPAR: ')'; 

AND: '&&';
OR: '||';
XOR: '^';

INT: 'int';
BOOL: 'bool';
AUTO: '$';

TRUE: ('t' | 'T') 'rue';
FALSE: ('f' | 'F') 'alse';

NUM: [1-9] DIGIT* | '0';

ID: LETTER (LETTER | DIGIT)*;

ONELINECOMMENT: '--' (~('\r'|'\n'))* -> skip;
MULTILINECOMMENT: '{' .*? '}' -> skip; 
WS: [ \t\r\n]+ -> skip;

fragment LETTER: [a-zA-Z];
fragment DIGIT: [0-9];