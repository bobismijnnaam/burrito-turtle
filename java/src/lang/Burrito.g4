grammar Burrito;

import BurritoVocab;

program: stat*;

stat: target ASS expr SEMI
	| expr IF stat* (ELSE stat*)? END
	| expr WHILE stat* END
	| type target ASS expr SEMI
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

type: INT | BOOL;
