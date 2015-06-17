package sprockell;

public class Value extends Operand {
	private int value;
	
	public Value(int value) {
		this.value = value;
	}
	
	public String toString() {
		return value + " ";
	}
}
