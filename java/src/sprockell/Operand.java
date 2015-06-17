package sprockell;

public abstract class Operand {

	/** Enumeration of all available operand types. */
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
