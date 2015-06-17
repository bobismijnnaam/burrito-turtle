package comp;

abstract public class Type {
	/** The singleton instance of the {@link Bool} type. */
	public static final Type BOOL = new Bool();
	/** The singleton instance of the {@link Int} type. */
	public static final Type INT = new Int();
	
	public static final int INT_SIZE = 4;

	private final TypeKind kind;

	public Type(TypeKind kind) {
		this.kind = kind;
	}

	public TypeKind getKind() {
		return this.kind;
	}
	
	/** returns the size (in bytes) of a value of this type. */
	abstract public int size();
	
	static public class Bool extends Type {
		private Bool() {
			super(TypeKind.BOOL);
		}

		@Override
		public int size() {
			return INT_SIZE;
		}

		@Override
		public String toString() {
			return "Boolean";
		}
	}

	static public class Int extends Type {
		private Int() {
			super(TypeKind.INT);
		}

		@Override
		public int size() {
			return INT_SIZE;
		}

		@Override
		public String toString() {
			return "Integer";
		}
	}
}
