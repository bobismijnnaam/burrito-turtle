import Sprockell.Sprockell
import Sprockell.System
import Sprockell.TypesEtc

prog = [ Const 6 RegA
       , Const 7 RegB
       , Compute Mul RegA RegB RegC
       , Write RegC stdio
       , EndProg
       ]

main = run 1 prog