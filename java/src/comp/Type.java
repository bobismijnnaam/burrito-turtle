package comp;

import java.util.ArrayList;
import java.util.List;

abstract public class Type {
	public static final int INT_SIZE = 1;
	
	/** returns the size (in bytes) of a value of this type. */
	abstract public int size();
	
	public boolean equals(Type other) {
		return this.getClass().equals(other.getClass());
	}
	
	public boolean equals(Object other) {
		if (other instanceof Type) {
			Type niceOther = (Type) other;
			return equals(niceOther);
		} else {
			return false;
		}
	}
	
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
	
	static public class Char extends Type {
		@Override
		public int size() {
			return INT_SIZE;
		}
		
		@Override
		public String toString() {
			return "char";
		}
	}
	
	static public class Array extends Type {
		public Type elemType;
		private int size;
		public List<Integer> indexSize;
		
		public Array(Type elemType, int size) {
			this.elemType = elemType;
			this.size = size;
			this.indexSize = new ArrayList<Integer>();
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
