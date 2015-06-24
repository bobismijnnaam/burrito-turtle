package sprockell;

import java.util.Map;

import sprockell.Operand.Type;
import sprockell.Sprockell.Op;

public class Instr {
	public Op op;
	public Operand[] args;
	
	public Instr(Op op, Operand... args) {
		this.op = op;
		this.args = args;
	}
	
	public boolean fixLabel(Map<String, Integer> labelMap) {
		boolean result = true;
		
		if (op == Op.Branch) {
			result = ((Target) args[1]).fixLabel(labelMap) && result;
		} else if (op == Op.Jump) {
			result = ((Target) args[0]).fixLabel(labelMap) && result;
		}
		
		for (Operand arg : args) {
			if (arg.getType() == Type.VALUE) {
				Value val = (Value) arg;
				result = val.fixLabel(labelMap) && result;
			}
		}
		
		return result;
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
