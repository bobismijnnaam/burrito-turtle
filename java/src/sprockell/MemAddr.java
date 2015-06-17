package sprockell;

public class MemAddr extends Operand {
	
	public Sprockell.MemAddr memAddr;
	public Sprockell.Reg reg;
	public Addr addr;
	public String sAddr;
	
	public MemAddr(Addr addr) {
		this.memAddr = Sprockell.MemAddr.Addr;
		this.addr = addr;
	}
	
	public MemAddr(Sprockell.Reg reg) {
		this.memAddr = Sprockell.MemAddr.Deref;
		this.reg = reg;
	}
	
	public MemAddr(String sAddr) {
		this.sAddr = sAddr;
	}
	
	public String toString() {
		if (reg == null && sAddr == null) {
			return "(" + memAddr.toString() + " " + addr.toString() + ")"; 
		} else if (sAddr != null) {
			return sAddr + " ";
		} else {
			return "(" + memAddr.toString() + " " + reg.toString() + ")";
		}
	}
}
