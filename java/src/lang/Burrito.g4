grammar Burrito;

import BurritoVocab; 

program: (func | decl)*; 
	
decl: type ID ASS expr SEMI;
	
func: sig stat* END;
sig: type ID LPAR (arg (COMMA arg)*)? RPAR;
arg: type ID;		

stat: type ID ASS expr SEMI				#typeAssignStat
	| target ASS expr SEMI				#assStat
	| expr IF block (ELSE block)? END	#ifStat
	| expr WHILE block END				#whileStat
	| type ID SEMI						#typeStat
	| expr? IO newlines SEMI			#outStat
	| RETURN expr? SEMI					#returnStat
	;

block: stat*;

target: ID								#idTarget 
	| ID (LBRA expr RBRA)+				#arrayTarget
	; 
 
expr: NOT expr								#notExpr
	| expr DIV expr							#divExpr
	| expr MUL expr							#mulExpr
	| expr POW expr							#powExpr
	| expr MOD expr							#modExpr
	| expr PLUS expr						#plusExpr
	| expr MIN expr							#minExpr
	| expr EQ expr							#eqExpr
	| expr LT expr							#ltExpr
	| expr GT expr							#gtExpr
	| expr LTE expr							#lteExpr
	| expr GTE expr							#gteExpr
	| expr AND expr							#andExpr
	| expr OR expr							#orExpr
	| expr XOR expr							#xorExpr
	| LPAR expr RPAR						#parExpr
	| ID									#idExpr
	| NUM									#numExpr
	| TRUE									#trueExpr
	| FALSE 								#falseExpr
	| MIN expr								#negExpr
	| ID LPAR (expr (COMMA expr)*)? RPAR	#funcExpr
	| ID (LBRA expr RBRA)+					#arrayExpr
	;

type: INT					#intType 
	| type LBRA NUM RBRA	#arrayType 
	| BOOL 					#boolType
	;
	
newlines: IONL*;
