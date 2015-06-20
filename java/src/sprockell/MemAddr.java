package sprockell;

public class MemAddr extends Operand {
	
	public Form memAddr;
	public Reg.Which reg;
	public Addr addr = null;
	public String sAddr = null;
	
	public MemAddr(Addr addr) {
		this.memAddr = Form.Addr;
		this.addr = addr;
	}
	
	public MemAddr(Reg.Which reg) {
		this.memAddr = Form.Deref;
		this.reg = reg;
	}
	
	public MemAddr(String sAddr) {
		this.memAddr = Form.SAddr;
		this.sAddr = sAddr;
	}
	
	public String toString() {
		switch(memAddr) {
		case Addr:
			return "(" + memAddr.toString() + " " + addr.toString() + ")"; 
		case SAddr:
			return sAddr + " ";
		case Deref:
			return "(" + memAddr.toString() + " " + reg.toString() + ")";
		}
		
		return "";
	}

	@Override
	public Operand.Type getType() {
		return Operand.Type.MEMADDR;
	}
	
	enum Form {
		Addr,
		SAddr,
		Deref
	}
}
