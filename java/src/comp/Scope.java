package comp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Scope {
	class Arg {
		public Type type;
		public String id;
	}
	
	/** Current size of this scope (in bytes). 
	 * Used to calculate offsets of newly declared variables. */
	private int size;
	/** Map from declared variables to their types. */
	private Map<String, Type> types = new HashMap<String, Type>();
	/** Map from declared variables to their offset within the allocation
	 * record of this scope. */
	private Map<String, Integer> offsets = new HashMap<String, Integer>();
	private Map<String, Function> functions = new HashMap<>();
	
	private List<Arg> argList = new ArrayList<>();
	
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
	
	// TODO: Finish this (check if signature already exists, if not add)
	public String putFunc(String id, String label, Type returnType, Type... args) {
		if (!functions.containsKey(id)) functions.put(id, new Function(id, returnType));
		Function func = functions.get(id);
		if (!func.returnType.equals(returnType)) return "A function can only have one return type"
				+ " across overloads";

		return func.registerOverload(args, label);
	}
	
	// TODO: Return true without any checks? Should be useful somehow
	public boolean putArg(String id, Type type) {
		Arg arg = new Arg();
		arg.id = id;
		arg.type = type;
		argList.add(arg);
		
		return true;
	}
	
	// TODO: Return true without any checks? Should be useful somehow
	public boolean finishArgs() {
		int leftMargin = 0;
		for (Arg arg : argList) {
			types.put(arg.id, arg.type);
			leftMargin -= type(arg.id).size();
		}
		
		for (Arg arg : argList) {
			offsets.put(arg.id, leftMargin);
			leftMargin += types.get(arg.id).size();
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
