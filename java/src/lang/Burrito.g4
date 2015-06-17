grammar Burrito;

import BurritoVocab;

program: stat*;

stat: target ASS expr SEMI				#assStat
	| expr IF stat* (ELSE stat*)? END	#ifStat
	| expr WHILE stat* END				#whileStat
	| type target ASS expr SEMI			#typeStat
	;

target: ID;

expr: NOT expr			#notExpr
	| expr DIV expr		#divExpr
	| expr MUL expr		#mulExpr
	| expr POW expr		#powExpr
	| expr MOD expr		#modExpr
	| expr PLUS expr	#plusExpr
	| expr MIN expr		#minExpr
	| expr EQ expr		#eqExpr
	| expr LT expr		#ltExpr
	| expr GT expr		#gtExpr
	| expr LTE expr		#lteExpr
	| expr GTE expr		#gteExpr
	| LPAR expr RPAR	#parExpr
	| ID				#idExpr
	| NUM				#numExpr
	| TRUE				#trueExpr
	| FALSE 			#falseExpr
	;

type: INT	#intType 
	| BOOL 	#boolType
	;
