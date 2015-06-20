package sprockell;

public class Operator extends Operand {
	
	public Which operator;
	
	public Operator(Which operator) {
		this.operator = operator;
	}
	
	public String toString() {
		return operator.toString() + " ";
	}

	@Override
	public Type getType() {
		return Type.OPERATOR;
	}

	public enum Which {
		Add,
		Sub,
		Mul,
		Div,
		Mod,
		Equal,
		NEq,
		Gt,
		Lt,
		GtE,
		LtE,
		And,
		Or,
		Xor,
		LShift,
		RShift
	}
}
