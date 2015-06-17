package sprockell;

import static sprockell.Operand.Type.REG;
import static sprockell.Operand.Type.MEMADDR;
import static sprockell.Operand.Type.TARGET;
import static sprockell.Operand.Type.OPERATOR;
import static sprockell.Operand.Type.VALUE;
import static sprockell.Operand.Type.STRING;

public class Sprockell {
	
	public enum Op {
		/** Operator, Reg, Reg => Reg */
		Compute(2, OPERATOR, REG, REG),
		/** Value => Reg */
		Const(1, VALUE, REG),
		/** Reg, Target */
		Branch(1, REG, TARGET),
		/** -> Target */
		Jump(0, TARGET),
		/** MemAddr => Reg */
		Load(1, MEMADDR, REG),
		/** Reg => MemAddr */
		Store(1, REG, MEMADDR),
		/** Reg */
		Push(1, REG),
		/** => Reg */
		Pop(0, REG),
		/** MemAddr */
		Read(1, MEMADDR),
		/** => Reg */
		Receive(0, REG),
		/** Reg => MemAddr */
		Write(1, MEMADDR),
		/** MemAddr */
		TestAndSet(1, MEMADDR),
		/** End of program */
		EndProg(0),
		/** Nop */
		Nop(0),
		/** String*/
		Debug(1, STRING);
		
		private Op(int arguments, Operand.Type... sig) {}
	}
	
	public enum Reg {
		Zero,
		PC,
		SP,
		SPID,
		RegA,
		RegB,
		RegC,
		RegD,
		RegE
	}
	
	public enum MemAddr {
		Addr,
		Deref
	}
	
	public enum Target {
		Abs,
		Rel,
		Ind
	}
	
	public enum Operator {
		Add,
		Sub,
		Mul,
		Div,
		Mod,
		Equal,
		NEq,
		Gt,
		Lt,
		GtE,
		LtE,
		And,
		Or,
		Xor,
		LShift,
		RShift
	}

}


