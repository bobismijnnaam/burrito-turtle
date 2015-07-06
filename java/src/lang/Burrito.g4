grammar Burrito;

import BurritoVocab; 

program: (func | decl | imp)*;  

imp: IMPORT PATH SEMI;  
decl: type ID (ASS expr)? SEMI; 
	
func: sig stat* END;
sig: type NOT? ID LPAR (arg (COMMA arg)*)? RPAR;
arg: type ID;

stat: type ID ASS expr SEMI						#typeAssignStat 
	| expr PLUS ASS expr SEMI					#plusAssStat
	| expr MIN ASS expr SEMI					#minAssStat
	| expr MUL ASS expr SEMI					#mulAssStat
	| expr DIV ASS expr SEMI					#divAssStat
	| expr ASS expr SEMI						#assStat
	| expr IF block (ELSE block)? END			#ifStat
	| expr WHILE block END						#whileStat
	| type ID SEMI								#typeStat
	| expr? IO NOT? newlines SEMI				#outStat
	| RETURN expr? SEMI							#returnStat
	| expr SEMI									#exprStat
	| START ID SEMI								#startStat
	| LOCK ID SEMI								#lockStat
	| UNLOCK ID	SEMI							#unlockStat
	| '<' expr '>' (litExpr COLON block)* (DEFAULT block)? END	#switchStat
	;

block: stat*;

//target://  ID								#idTarget 
//	// | ID (LBRA expr RBRA)+				#arrayTarget
//	| expr								#exprTarget
//	; 

// target: expr;   
 
expr: PTR ID								#deferExpr
	| expr PTR								#derefExpr
	| NOT expr								#notExpr
	| expr (PLUS)+							#incExpr
	| expr (MIN)+							#decExpr
	| expr DIV expr							#divExpr
	| expr MUL expr							#mulExpr
	| expr POW expr							#powExpr
	| expr MOD expr							#modExpr
	| expr PLUS expr						#plusExpr
	| expr MIN expr							#minExpr
	| expr EQ expr							#eqExpr
	| expr NEQ expr							#neqExpr
	| expr LT expr							#ltExpr
	| expr GT expr							#gtExpr
	| expr LTE expr							#lteExpr
	| expr GTE expr							#gteExpr
	| expr AND expr							#andExpr
	| expr OR expr							#orExpr
	| expr XOR expr							#xorExpr
	| LPAR expr RPAR						#parExpr
	| ID									#idExpr
	| litExpr								#literalExpr
	| MIN expr								#negExpr
	| ID LPAR (expr (COMMA expr)*)? RPAR	#funcExpr
	| expr LBRA expr RBRA 					#arrayExpr
	;
	
litExpr: MIN? NUM							#numExpr
	| CHARACTER								#characterExpr
	| TRUE									#trueExpr
	| FALSE 								#falseExpr
	| STRING								#stringExpr
	| LBRA litExpr (COMMA litExpr)* RBRA	#seqExpr
	;

type: INT					#intType 
	| BOOL 					#boolType
	| CHAR					#charType
	| LOCKT					#lockType
	| VOID					#voidType	
	| type PTR				#pointerType
	| type LBRA NUM? RBRA	#arrayType  
	;
	
newlines: IONL*;
