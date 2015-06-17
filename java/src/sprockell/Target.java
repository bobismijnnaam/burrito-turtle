package sprockell;

public class Target extends Operand {
	
	public Sprockell.Target target;
	public CodeAddr codeAddr;
	public Sprockell.Reg reg;
	
	public Target(Sprockell.Target target, CodeAddr codeAddr) {
		this.target = target;
		this.codeAddr = codeAddr;
	}
	
	public Target(Sprockell.Reg reg) {
		this.target = Sprockell.Target.Ind;
		this.reg = reg;
	}
	
	public String toString() {
		if (reg == null) {
			return "(" + target.toString() + " " + codeAddr.toString() + ")";
		} else {
			return "(" + target.toString() + " " + reg.toString() + ")";
		}
	}
	
}
