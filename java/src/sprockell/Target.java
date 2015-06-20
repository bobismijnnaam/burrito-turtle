package sprockell;

public class Target extends Operand {
	
	public Form target;
	public CodeAddr codeAddr;
	public Reg.Which reg;
	
	public Target(Form target, CodeAddr codeAddr) {
		this.target = target;
		this.codeAddr = codeAddr;
	}
	
	public Target(Reg.Which reg) {
		this.target = Form.Ind;
		this.reg = reg;
	}
	
	public String toString() {
		if (reg == null) {
			return "(" + target.toString() + " " + codeAddr.toString() + ")";
		} else {
			return "(" + target.toString() + " " + reg.toString() + ")";
		}
	}

	@Override
	public Type getType() {
		return Type.TARGET;
	}

	public enum Form {
		Abs,
		Rel,
		Ind
	}
	
	
}
