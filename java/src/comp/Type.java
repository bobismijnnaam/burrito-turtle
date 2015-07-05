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
	
	/**
	 * Gosh
	 *
	 */
	static public class Array extends Type {
		public Type elemType;
		public int size;
		
		public Array(Type elemType, int size) {
			this.elemType = elemType;
			this.size = size;
		}
		
		public Type getBaseType() {
			if (elemType instanceof Array) {
				return ((Array) elemType).getBaseType();
			} else {
				return elemType;
			}
		}
		
		public void insertLayer(Array arr) {
			if (elemType instanceof Array) {
				((Array) elemType).insertLayer(arr);
			} else {
				arr.elemType = elemType;
				elemType = arr;
			}
		}
		
		@Override
		public int size() {
			return size * elemType.size();
		}

		@Override
		public boolean assignable() {
			return false;
		}
		
		@Override
		public String toString() {
			if (size >= 0) {
				return "array with " + size + " of " + elemType;
			} else {
				return "incomplete array of " + elemType;
			}
		}
		
		@Override
		public boolean equals(Object other) {
			if (other instanceof Array) {
				return elemType.equals(((Array) other).elemType) && size == ((Array) other).size;
			} 
			
			return false;
		}
		
		public boolean isIncomplete() {
			if (elemType instanceof Array) {
				return size < 0 || ((Array) elemType).isIncomplete();
			} else {
				return size < 0;
			}
		}
	}
	
	static public class StringLiteral extends Type {
		public String content;

		@Override
		public int size() {
			// TODO Auto-generated method stub
			return -1;
		}

		@Override
		public boolean assignable() {
			// TODO Auto-generated method stub
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
		
		public int getDepth() {
			if (pointsTo instanceof Pointer) {
				return 1 + ((Pointer) pointsTo).getDepth();
			} else {
				return 1;
			}
		}
		
		public void setBaseType(Type type) {
			if (pointsTo instanceof Pointer) {
				((Pointer) pointsTo).setBaseType(type);
			} else {
				pointsTo = type;
			}
		}
		
		public boolean equals(Object other) {
			if (other instanceof Pointer) {
				return ((Pointer) other).pointsTo.equals(pointsTo);
			}
			
			return false;
		}
	}
}
