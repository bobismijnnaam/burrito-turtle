grammar burrito;

program: stat*;

stat: target ASS expr SEMI
	| expr IF stat* (ELSE stat*)? END
	| expr WHILE stat* END
	;

target: ID;

expr: NOT expr
	| expr PLUS expr
	| expr MIN expr
	| expr DIV expr
	| expr MUL expr
	| expr POW expr
	| expr MOD expr
	| expr EQ expr
	| expr LT expr
	| expr GT expr
	| expr LTE expr
	| expr GTE expr
	| LPAR expr RPAR
	| ID
	| NUM
	| TRUE
	| FALSE 
	;

plusOp: PLUS | MIN;

/**
 * Basic language building blocks
 */

WS: [ \t\r\n]+ -> skip;

ID: LETTER (LETTER | DIGIT)*;

fragment LETTER: [a-zA-Z];
fragment DIGIT: [0-9];

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

TRUE: ('t' | 'T') 'rue';
FALSE: ('f' | 'F') 'alse';

NUM: [1-9] DIGIT*;