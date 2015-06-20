package sprockell;

public abstract class Operand {
	public static enum Type {
		REG,
		MEMADDR,
		TARGET,
		OPERATOR,
		VALUE,
		STRING
		;
	}
	
	public abstract Type getType();
}
