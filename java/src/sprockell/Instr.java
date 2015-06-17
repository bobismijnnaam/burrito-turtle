package sprockell;

import sprockell.Sprockell.Op;

public class Instr {
	
	public Op op;
	public Operand[] args;
	
	public Instr(Op op, Operand... args) {
		this.op = op;
		this.args = args;
	}
	
	public String toString() {
		String op = "";
		op += this.op.toString() + " ";
		
		for (int i = 0; i < args.length; i++) {
			op += args[i].toString() + "";
		}
		
		return op;
	}
	
}
