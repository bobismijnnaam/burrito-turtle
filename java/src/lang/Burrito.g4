grammar Burrito;

import BurritoVocab; 

program: (func | decl)*; 
	
decl: type ID ASS expr SEMI;
	
func: sig stat* END;
sig: type ID LPAR (arg (COMMA arg)*)? RPAR;
arg: type ID;		

stat: type ID ASS expr SEMI					#typeAssignStat
	| target ASS expr SEMI					#assStat
	| target PLUS ASS expr SEMI				#plusAssStat
	| target MIN ASS expr SEMI				#minAssStat
	| target MUL ASS expr SEMI				#mulAssStat
	| target DIV ASS expr SEMI				#divAssStat
	| expr IF block (ELSE block)? END		#ifStat
	| expr WHILE block END					#whileStat
	| type ID SEMI							#typeStat
	| expr? IO newlines SEMI				#outStat
	| RETURN expr? SEMI						#returnStat
	| expr SEMI								#exprStat
	;

block: stat*;

target: ID								#idTarget 
	| ID (LBRA expr RBRA)+				#arrayTarget
	| ID PTR+							#ptrTarget
	; 
 
expr: NOT expr								#notExpr
	| target (PLUS)+						#incExpr
	| target (MIN)+							#decExpr
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
	| CHARACTER								#characterExpr
	| TRUE									#trueExpr
	| FALSE 								#falseExpr
	| MIN expr								#negExpr
	| ID LPAR (expr (COMMA expr)*)? RPAR	#funcExpr
	| ID (LBRA expr RBRA)+					#arrayExpr
	;

type: INT					#intType 
	| BOOL 					#boolType
	| CHAR					#charType
	| type LBRA NUM RBRA	#arrayType  
	;
	
newlines: IONL*;
