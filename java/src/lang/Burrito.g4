grammar Burrito;

import BurritoVocab;

program: stat*;

stat: type target ASS expr SEMI			#typeStat
	| target ASS expr SEMI				#assStat
	| expr IF block (ELSE block)? END	#ifStat
	| expr WHILE block END				#whileStat
	| expr IO SEMI						#outStat
	;

block: stat*;

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
	| MIN expr			#negExpr
	;

type: INT	#intType 
	| BOOL 	#boolType
	;
