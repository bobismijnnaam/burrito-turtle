package sprockell;

public class Addr extends Operand {
	
	public int addr;
	
	public Addr(int addr) {
		this.addr = addr;
	}

	public String toString() {
		return addr + " ";
	}
}
