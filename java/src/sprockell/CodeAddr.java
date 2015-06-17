package sprockell;

public class CodeAddr extends Operand {
	public int codeAddr;
	
	public CodeAddr (int codeAddr) {
		this.codeAddr = codeAddr;
	}
	
	public String toString() {
		return codeAddr + " ";
	}
}
