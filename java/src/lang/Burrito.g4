grammar Burrito;

import BurritoVocab; 

program: (func | decl)*; 
	
decl: type ID (ASS expr)? SEMI;
	
func: sig stat* END;
sig: type NOT? ID LPAR (arg (COMMA arg)*)? RPAR;
arg: type ID			#plainArg
	| type LBRA RBRA	#anyArrayArg
	;

stat: type ID ASS expr SEMI					#typeAssignStat
	| target ASS expr SEMI					#assStat
	| target PLUS ASS expr SEMI				#plusAssStat
	| target MIN ASS expr SEMI				#minAssStat
	| target MUL ASS expr SEMI				#mulAssStat
	| target DIV ASS expr SEMI				#divAssStat
	| expr IF block (ELSE block)? END		#ifStat
	| expr WHILE block END					#whileStat
	| type ID SEMI							#typeStat
	| expr? IO NOT? newlines SEMI			#outStat
	| RETURN expr? SEMI						#returnStat
	| expr SEMI								#exprStat
	| START ID SEMI							#startStat
	| LOCK ID SEMI							#lockStat
	| UNLOCK ID	SEMI						#unlockStat
	;

block: stat*;

target: ID								#idTarget 
	| ID (LBRA expr RBRA)+				#arrayTarget
	| ID DEREF+							#ptrTarget
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
	| LENGTH LPAR ID RPAR					#lenExpr
	;

type: INT					#intType 
	| BOOL 					#boolType
	| CHAR					#charType
	| LOCKT					#lockType
	| VOID					#voidType	
	| type LBRA NUM RBRA	#arrayType  
	;
	
newlines: IONL*;
