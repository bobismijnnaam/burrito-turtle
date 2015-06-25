package comp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private Map<String, Function> functions = new HashMap<String, Function>();
	
	private List<String> argList = new ArrayList<String>();
	
	private Stack<Integer> sizeStack = new Stack<Integer>();
	private Stack<Map<String, Type>> typeStack = new Stack<Map<String, Type>>();
	private Stack<Map<String, Integer>> offsetStack = new Stack<Map<String, Integer>>();
	
	// TODO: Implement function pushing popping
	
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
	
	public boolean containsFunc(String id) {
		return this.functions.containsKey(id);
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
			this.size += size;
		}
		return result;
	}
	
	public boolean putFunc(String id, String label, Type returnType, Type... args) {
		Function func = new Function();
		func.id = id;
		func.label = label;
		func.args = args;
		func.returnType = returnType;
		
		functions.put(id, func);
		
		return true;
	}
	
	public boolean putArg(String id, Type type) {
		types.put(id, type);
		argList.add(id);
		
//		System.out.println("Registered " + id + " of type " + type);
		
		return true;
	}
	
	public boolean finishArgs() {
		int leftMargin = 0;
		for (String id : argList) {
			leftMargin -= type(id).size();
		}
		
		for (String id : argList) {
			offsets.put(id, leftMargin);
			leftMargin += type(id).size();
		}
		
		argList.clear();
		
		return true;
	}

	/**
	 * Returns the type of a given (presumably declared) identifier.
	 */
	public Type type(String id) {
		return this.types.get(id);
	}
	
	public Function func(String id) {
		return this.functions.get(id);
	}

	/** 
	 * Returns the offset of a given (presumably declared) identifier. 
	 * with respect to the beginning of this scope's activation record.
	 * Offsets are assigned in order of declaration. 
	 */
	public int offset(String id) {
		return this.offsets.get(id);
	}
	
	public int getCurrentStackSize() {
		return size;
	}
}
