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
IONL: '\\';
SCOPE: '#';
END: '.';
SEMI: ';';
COMM: '--';
COMMB: '{';
COMME: '}';
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

WS: [ \t\r\n]+ -> skip;

fragment LETTER: [a-zA-Z];
fragment DIGIT: [0-9];