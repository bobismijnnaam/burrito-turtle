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
	
	static public class Array extends Type {
		public Type elemType;
		private int size;
		public List<Integer> indexSize;
		
		public Array(Type elemType, int size) {
			this.elemType = elemType;
			this.size = size;
			this.indexSize = new ArrayList<Integer>();
		}
		
		public Type getBaseType() {
			if (new Array(null, 0).equals(elemType)) {
				return ((Array) elemType).getBaseType();
			} else {
				return elemType;
			}
		}

		@Override
		public int size() {
			return elemType.size() * size;
		}

		@Override
		public String toString() {
			return "array of " + elemType.toString();
		}
	}
	
	public static void main(String[] args) {
		Type baseType = new Type.Int();
		Type index1 = new Type.Array(baseType, 2);
		Type index2 = new Type.Array(index1, 2);
		Type index3 = new Type.Array(index2, 2);
		System.out.println(index3);
		System.out.println(((Array) index3).getBaseType());
	}
	
	static public class Pointer extends Type {
		public Type pointsTo;
		
		public Pointer(Type pointsTo) {
			this.pointsTo = pointsTo;
		}
		
		@Override
		public String toString() {
			return "pointer to " + pointsTo.toString();
		}

		@Override
		public int size() {
			return INT_SIZE;
		}
	}
}
