package sprockell;

public class Reg extends Operand {
	
	public Which reg;
	
	public Reg(Which reg) {
		this.reg = reg;
	}
	
	public String toString() {
		return reg.toString();
	}

	@Override
	public Type getType() {
		return Type.REG;
	}

	public enum Which {
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
}
