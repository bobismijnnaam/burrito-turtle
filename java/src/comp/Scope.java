package comp;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

public class Scope {
	/** Current size of this scope (in bytes). 
	 * Used to calculate offsets of newly declared variables. */
	private int size;
	/** Map from declared variables to their types. */
	private Map<String, Type> types = new HashMap<String, Type>();
	/** Map from declared variables to their offset within the allocation
	 * record of this scope. */
	private Map<String, Integer> offsets = new HashMap<String, Integer>();
	
	private Stack<Integer> sizeStack = new Stack<Integer>();
	private Stack<Map<String, Type>> typeStack = new Stack<Map<String, Type>>();
	private Stack<Map<String, Integer>> offsetStack = new Stack<Map<String, Integer>>();
	
	/**
	 * Pops the previous scope state off the stack, restoring previous sizes, type mappings, etc.
	 */
	public void popScope() {
		size = sizeStack.pop();
		types = typeStack.pop();
		offsets = offsetStack.pop();
	}
	
	/**
	 * Pushes the current scope state on the stack
	 */
	public void pushScope() {
		sizeStack.push(size);
		typeStack.push(new HashMap<String, Type>(types));
		offsetStack.push(new HashMap<String, Integer>(offsets));
	}

	/**
	 * Tests if a given identifier is declared in this scope.
	 */
	public boolean contains(String id) {
		return this.types.containsKey(id);
	}

	/**
	 * Declares an identifier with a given type, if the identifier
	 * is not yet in this scope.
	 * @return <code>true</code> if the identifier was added;
	 * <code>false</code> if it was already declared.
	 */
	public boolean put(String id, Type type) {
		boolean result = !this.types.containsKey(id);
		if (result) {
			this.types.put(id, type);
			this.offsets.put(id, this.size);
			this.size += type.size();
		}
		return result;
	}
	
	public boolean put(String id, Type type, int size) {
		boolean result = !this.types.containsKey(id);
		if (result) {
			this.types.put(id, type);
			this.offsets.put(id, this.size);
			this.size += type.size() * size;
		}
		return result;
	}

	/**
	 * Returns the type of a given (presumably declared) identifier.
	 */
	public Type type(String id) {
		return this.types.get(id);
	}

	/** 
	 * Returns the offset of a given (presumably declared) identifier. 
	 * with respect to the beginning of this scope's activation record.
	 * Offsets are assigned in order of declaration. 
	 */
	public Integer offset(String id) {
		return this.offsets.get(id);
	}
}
