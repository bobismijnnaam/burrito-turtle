package comp;

import java.util.ArrayList;
import java.util.List;

abstract public class Type {
	public static final int INT_SIZE = 1;
	
	/** returns the size (in words) of a value of this type. */
	abstract public int size();
	/** returns whether or not you can assign something to a variable of this type */
	abstract public boolean assignable();
	
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
	
	static public class Void extends Type {
		@Override
		public int size() {
			return INT_SIZE;
		}
		
		@Override
		public boolean assignable() {
			return false;
		}
		
		@Override
		public String toString() {
			return "void";
		}
	}
	
	static public class Bool extends Type {
		@Override
		public int size() {
			return INT_SIZE;
		}
		
		@Override
		public boolean assignable() {
			return true;
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
		public boolean assignable() {
			return true;
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
		public boolean assignable() {
			return true;
		}

		@Override
		public String toString() {
			return "char";
		}
	}
	
	static public class Lock extends Type {
		@Override
		public int size() {
			return INT_SIZE * 2;
		}
		
		@Override
		public boolean assignable() {
			return false;
		}
		
		public String toString() {
			return "lock";
		}
	}
	
	static public class Array extends Type {
		public Type elemType;
		private int size;
		public List<Integer> indexSize;
		private boolean outer = false;
		
		public Array(Type elemType, int size) {
			this.elemType = elemType;
			this.size = size;
			this.indexSize = new ArrayList<Integer>();
		}
		
		@Override
		public boolean assignable() {
			return false;
		}
		
		public Type getBaseType() {
			if (elemType instanceof Array) {
				return ((Array) elemType).getBaseType();
			} else {
				return elemType;
			}
		}

		@Override
		public int size() {
			if (outer)
				return elemType.size() * size + 1;
			else
				return elemType.size() * size;
		}
		
		/**
		 * @return The amount of elements in this array
		 */
		public int length() {
			return size;
		}

		@Override
		public String toString() {
			return "array of " + elemType.toString();
		}
		
		public void setOuter() {
			outer = true;
		}
		
		public boolean isOuter() {
			return outer;
		}
	}
	
	static public class AnyArray extends Type {
		public Type elemType;
		
		AnyArray(Type elemType) {
			this.elemType = elemType;
		}
		
		public Type getBaseType() {
			if (elemType instanceof Array) {
				return ((Array) elemType).getBaseType();
			} else {
				return elemType;
			}
		}
		
		@Override
		public int size() {
			return 2;
		}

		@Override
		public boolean assignable() {
			return false;
		}
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
		
		@Override
		public boolean assignable() {
			return true;
		}
	}
}
