import Sprockell.Sprockell
import Sprockell.System
import Sprockell.TypesEtc

prog = [ Const 6 RegA
       , Const 7 RegB
       , Compute Mul RegA RegB RegC
       , Write RegC stdio
       , ComputeI Add 97 RegB RegC
       , Write RegC stdio
       , IntCopy RegC RegD
       , Write RegD stdio
       , Const 180 RegA
       , Const 2 RegB
       , Compute Div RegA RegB RegA
       , Write RegA stdio
       , Const 179 RegA
       , Compute Div RegA RegB RegA
       , Write RegA stdio
       , Write RegA stdio
       , Write RegA stdio
       , Write RegA stdio
       , Write RegA stdio
       , Write RegA stdio
       , Write RegA stdio
       , Write RegA stdio
       , Write RegA stdio
       , Write RegA stdio
       , Nop
       , Nop
       , Nop
       , Nop
       , Nop
       , Nop
       , Nop
       , Nop
       , Nop
       , Nop
       , Nop
       , Nop
       , Nop
       , Nop
       , Nop
       , Nop
       , Nop
       , Nop
       , Nop
       , Nop
       , EndProg
       ]

main = run 1 prog
