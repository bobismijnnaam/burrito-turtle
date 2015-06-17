package sprockell;

public class Reg extends Operand {
	
	public Sprockell.Reg reg;
	
	public Reg(Sprockell.Reg reg) {
		this.reg = reg;
	}
	
	public String toString() {
		return reg.toString() + " ";
	}
}
