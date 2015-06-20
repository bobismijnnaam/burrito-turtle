package sprockell;

public class MemAddr extends Operand {
	
	public Form memAddr;
	public Reg.Which reg;
	public Addr addr;
	public String sAddr;
	
	public MemAddr(Addr addr) {
		this.memAddr = Form.Addr;
		this.addr = addr;
	}
	
	public MemAddr(Reg.Which reg) {
		this.memAddr = Form.Deref;
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

	@Override
	public Operand.Type getType() {
		return Operand.Type.MEMADDR;
	}
	
	enum Form {
		Addr,
		Deref
	}
}
