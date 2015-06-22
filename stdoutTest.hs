import Sprockell.System

prog = [
		Const 12345 RegE,
		Const 10 RegD,
		Const 0 RegB,
		Compute LtE Zero RegE RegC,
		Branch RegC (Abs 8),
		Const 45 RegC,
		Write RegC stdio ,
		Compute Sub Zero RegE RegE,
		Compute Mod RegE RegD RegC,
		Push RegC,
		Const 10 RegC,
		Compute Div RegD RegC RegD,
		Pop RegC,
		Compute Div RegC RegD RegC,
		Push RegC,
		Const 10 RegC,
		Compute Mul RegC RegD RegD,
		Const 1 RegC,
		Compute Add RegC RegB RegB,
		Compute GtE RegD RegE RegC,
		Branch RegC (Abs 24),
		Const 10 RegC,
		Compute Mul RegC RegD RegD,
		Jump (Abs 8),
		Const 48 RegD,
		Const 1 RegE,
		Pop RegC,
		Compute Sub RegB RegE RegB,
		Compute Add RegC RegD RegC,
		Write RegC stdio ,
		Compute Equal Zero RegB RegC,
		Branch RegC (Abs 33),
		Jump (Abs 26),
		Nop ,
		Const 0 RegE,
		Const 10 RegD,
		Const 0 RegB,
		Compute LtE Zero RegE RegC,
		Branch RegC (Abs 42),
		Const 45 RegC,
		Write RegC stdio ,
		Compute Sub Zero RegE RegE,
		Compute Mod RegE RegD RegC,
		Push RegC,
		Const 10 RegC,
		Compute Div RegD RegC RegD,
		Pop RegC,
		Compute Div RegC RegD RegC,
		Push RegC,
		Const 10 RegC,
		Compute Mul RegC RegD RegD,
		Const 1 RegC,
		Compute Add RegC RegB RegB,
		Compute GtE RegD RegE RegC,
		Branch RegC (Abs 58),
		Const 10 RegC,
		Compute Mul RegC RegD RegD,
		Jump (Abs 42),
		Const 48 RegD,
		Const 1 RegE,
		Pop RegC,
		Compute Sub RegB RegE RegB,
		Compute Add RegC RegD RegC,
		Write RegC stdio ,
		Compute Equal Zero RegB RegC,
		Branch RegC (Abs 67),
		Jump (Abs 60),
		Nop ,
		Const 987654321 RegE,
		Const 10 RegD,
		Const 0 RegB,
		Compute LtE Zero RegE RegC,
		Branch RegC (Abs 76),
		Const 45 RegC,
		Write RegC stdio ,
		Compute Sub Zero RegE RegE,
		Compute Mod RegE RegD RegC,
		Push RegC,
		Const 10 RegC,
		Compute Div RegD RegC RegD,
		Pop RegC,
		Compute Div RegC RegD RegC,
		Push RegC,
		Const 10 RegC,
		Compute Mul RegC RegD RegD,
		Const 1 RegC,
		Compute Add RegC RegB RegB,
		Compute GtE RegD RegE RegC,
		Branch RegC (Abs 92),
		Const 10 RegC,
		Compute Mul RegC RegD RegD,
		Jump (Abs 76),
		Const 48 RegD,
		Const 1 RegE,
		Pop RegC,
		Compute Sub RegB RegE RegB,
		Compute Add RegC RegD RegC,
		Write RegC stdio ,
		Compute Equal Zero RegB RegC,
		Branch RegC (Abs 101),
		Jump (Abs 94),
		Nop ,
		Const 98765432 RegE,
		Compute Sub Zero RegE RegE,
		Const 10 RegD,
		Const 0 RegB,
		Compute LtE Zero RegE RegC,
		Branch RegC (Abs 111),
		Const 45 RegC,
		Write RegC stdio ,
		Compute Sub Zero RegE RegE,
		Compute Mod RegE RegD RegC,
		Push RegC,
		Const 10 RegC,
		Compute Div RegD RegC RegD,
		Pop RegC,
		Compute Div RegC RegD RegC,
		Push RegC,
		Const 10 RegC,
		Compute Mul RegC RegD RegD,
		Const 1 RegC,
		Compute Add RegC RegB RegB,
		Compute GtE RegD RegE RegC,
		Branch RegC (Abs 127),
		Const 10 RegC,
		Compute Mul RegC RegD RegD,
		Jump (Abs 111),
		Const 48 RegD,
		Const 1 RegE,
		Pop RegC,
		Compute Sub RegB RegE RegB,
		Compute Add RegC RegD RegC,
		Write RegC stdio ,
		Compute Equal Zero RegB RegC,
		Branch RegC (Abs 136),
		Jump (Abs 129),
		Nop ,
		Const 12345999 RegE,
		Compute Sub Zero RegE RegE,
		Const 10 RegD,
		Const 0 RegB,
		Compute LtE Zero RegE RegC,
		Branch RegC (Abs 146),
		Const 45 RegC,
		Write RegC stdio ,
		Compute Sub Zero RegE RegE,
		Compute Mod RegE RegD RegC,
		Push RegC,
		Const 10 RegC,
		Compute Div RegD RegC RegD,
		Pop RegC,
		Compute Div RegC RegD RegC,
		Push RegC,
		Const 10 RegC,
		Compute Mul RegC RegD RegD,
		Const 1 RegC,
		Compute Add RegC RegB RegB,
		Compute GtE RegD RegE RegC,
		Branch RegC (Abs 162),
		Const 10 RegC,
		Compute Mul RegC RegD RegD,
		Jump (Abs 146),
		Const 48 RegD,
		Const 1 RegE,
		Pop RegC,
		Compute Sub RegB RegE RegB,
		Compute Add RegC RegD RegC,
		Write RegC stdio ,
		Compute Equal Zero RegB RegC,
		Branch RegC (Abs 171),
		Jump (Abs 164),
		Nop ,
		Const 0 RegE,
		Compute Sub Zero RegE RegE,
		Const 10 RegD,
		Const 0 RegB,
		Compute LtE Zero RegE RegC,
		Branch RegC (Abs 181),
		Const 45 RegC,
		Write RegC stdio ,
		Compute Sub Zero RegE RegE,
		Compute Mod RegE RegD RegC,
		Push RegC,
		Const 10 RegC,
		Compute Div RegD RegC RegD,
		Pop RegC,
		Compute Div RegC RegD RegC,
		Push RegC,
		Const 10 RegC,
		Compute Mul RegC RegD RegD,
		Const 1 RegC,
		Compute Add RegC RegB RegB,
		Compute GtE RegD RegE RegC,
		Branch RegC (Abs 197),
		Const 10 RegC,
		Compute Mul RegC RegD RegD,
		Jump (Abs 181),
		Const 48 RegD,
		Const 1 RegE,
		Pop RegC,
		Compute Sub RegB RegE RegB,
		Compute Add RegC RegD RegC,
		Write RegC stdio ,
		Compute Equal Zero RegB RegC,
		Branch RegC (Abs 206),
		Jump (Abs 199),
		Nop ,
		Const 1 RegE,
		Compute Equal Zero RegE RegD,
		Branch RegD (Abs 219),
		Const 116 RegD,
		Write RegD stdio ,
		Const 114 RegD,
		Write RegD stdio ,
		Const 117 RegD,
		Write RegD stdio ,
		Const 101 RegD,
		Write RegD stdio ,
		Jump (Abs 230),
		Nop ,
		Const 102 RegD,
		Write RegD stdio ,
		Const 97 RegD,
		Write RegD stdio ,
		Const 108 RegD,
		Write RegD stdio ,
		Const 115 RegD,
		Write RegD stdio ,
		Const 101 RegD,
		Write RegD stdio ,
		Nop ,
		Const 0 RegE,
		Compute Equal Zero RegE RegD,
		Branch RegD (Abs 243),
		Const 116 RegD,
		Write RegD stdio ,
		Const 114 RegD,
		Write RegD stdio ,
		Const 117 RegD,
		Write RegD stdio ,
		Const 101 RegD,
		Write RegD stdio ,
		Jump (Abs 254),
		Nop ,
		Const 102 RegD,
		Write RegD stdio ,
		Const 97 RegD,
		Write RegD stdio ,
		Const 108 RegD,
		Write RegD stdio ,
		Const 115 RegD,
		Write RegD stdio ,
		Const 101 RegD,
		Write RegD stdio ,
		Nop ,
		EndProg 
		]
main = run 1 prog
