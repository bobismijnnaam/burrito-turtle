import Sprockell.System

prog = [
		Const 2 RegA ,
		Const 3 RegB ,
		Compute Add RegA RegB RegC ,
		Write RegC stdio ,
		EndProg 
		]

main = run 1 prog