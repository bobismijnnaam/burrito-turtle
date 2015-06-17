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
SCOPE: '#';
END: '.';
SEMI: ';';
COMM: '--';
COMMB: '{';
COMME: '}';
LPAR: '(';
RPAR: ')';

INT: 'int';
BOOL: 'bool';
AUTO: '$';

TRUE: ('t' | 'T') 'rue';
FALSE: ('f' | 'F') 'alse';

NUM: [1-9] DIGIT*;

ID: LETTER (LETTER | DIGIT)*;

WS: [ \t\r\n]+ -> skip;

fragment LETTER: [a-zA-Z];
fragment DIGIT: [0-9];
