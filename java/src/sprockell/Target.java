package sprockell;

import java.util.Map;

import comp.Type.Int;

public class Target extends Operand {
	public Form target;
	public CodeAddr codeAddr = null;
	public Reg.Which reg = null;
	public String label = null;
	
	public Target(Form target, CodeAddr codeAddr) {
		this.target = target;
		this.codeAddr = codeAddr;
	}
	
	public Target(Reg.Which reg) {
		this.target = Form.Ind;
		this.reg = reg;
	}
	
	public Target(String label) {
		this.target = Form.Lbl;
		this.label = label;
	}
	
	public boolean fixLabel(Map<String, Integer> labelMap) {
		if (target == Form.Lbl) {
			if (!labelMap.containsKey(label)) {
				System.out.println(labelMap);
				System.out.println("Undefined label \"" + label + "\" found");
				return false;
			} else {
				int dst = labelMap.get(label);
				
				target = Form.Abs;
				codeAddr = new CodeAddr(dst);
				label = null;
				return true;
			}
		} else {
			return true;
		}
	}
	
	public String toString() {
		switch (target) {
		case Abs:
		case Rel:
			return "(" + target.toString() + " " + codeAddr.toString() + ")";
		case Ind:
			return "(" + target.toString() + " " + reg.toString() + ")";
		case Lbl:
			return "(" + target.toString() + " " + label + ")";
		}
		
		return "";
	}

	@Override
	public Type getType() {
		return Type.TARGET;
	}

	public enum Form {
		Abs,
		Rel,
		Ind,
		Lbl
	}
}
