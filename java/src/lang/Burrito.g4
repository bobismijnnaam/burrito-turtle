grammar Burrito;

import BurritoVocab;

program: stat*;

stat: type ID ASS expr SEMI				#typeAssignStat
	| target ASS expr SEMI				#assStat
	| expr IF block (ELSE block)? END	#ifStat
	| expr WHILE block END				#whileStat
	| type ID SEMI						#typeStat
	;

block: stat*;

target: ID								#idTarget
	| ID LBRA expr RBRA					#arrayTarget
	;

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
	| type LBRA NUM RBRA #arrayType
	| BOOL 	#boolType
	;
