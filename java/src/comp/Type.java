package comp;

abstract public class Type {
	public static final int INT_SIZE = 1;
	
	/** returns the size (in bytes) of a value of this type. */
	abstract public int size();
	
	static public class Bool extends Type {
		@Override
		public int size() {
			return INT_SIZE;
		}

		@Override
		public String toString() {
			return "bool";
		}
	}

	static public class Int extends Type {
		@Override
		public int size() {
			return INT_SIZE;
		}

		@Override
		public String toString() {
			return "int";
		}
	}
	
	static public class Array extends Type {
		public Type elemType;
		private int size;
		
		public Array(Type elemType, int size) {
			this.elemType = elemType;
			this.size = size;
		}

		@Override
		public int size() {
			return elemType.size() * size;
		}

		@Override
		public String toString() {
			return "Array";
		}
	}
}
