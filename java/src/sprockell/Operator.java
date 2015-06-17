package sprockell;

public class Operator extends Operand {
	
	public Sprockell.Operator operator;
	
	public Operator(Sprockell.Operator operator) {
		this.operator = operator;
	}
	
	public String toString() {
		return operator.toString() + " ";
	}

	@Override
	public Type getType() {
		return Type.OPERATOR;
	}
}
