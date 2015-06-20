package sprockell;

import java.util.Map;

import sprockell.Sprockell.Op;

public class Instr {
	public Op op;
	public Operand[] args;
	
	public Instr(Op op, Operand... args) {
		this.op = op;
		this.args = args;
	}
	
	public void fixLabel(Map<String, Integer> labelMap) {
		if (op == Op.Branch) {
			((Target) args[1]).fixLabel(labelMap);
		} else if (op == Op.Jump) {
			((Target) args[0]).fixLabel(labelMap);
		}
	}
	
	public String toString() {
		String result = op.toString() + " ";
		
		for (int i = 0; i < args.length; i++) {
			result += args[i].toString();
			
			if (i < args.length - 1) {
				result += " ";
			}
		}
		
		return result;
	}
}
