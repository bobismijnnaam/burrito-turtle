package comp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import comp.Type.Pointer;

public class Scope {
	class Arg {
		public Type type;
		public String id;
	}
	
	/** Current size of this scope (in bytes). 
	 * Used to calculate offsets of newly declared variables. */
	private int size;
	private int globalSize;
	/** Map from declared variables to their types. */
	private Map<String, Type> types = new HashMap<String, Type>();
	/** Map from declared variables to their offset within the allocation
	 * record of this scope. */
	private Map<String, Integer> offsets = new HashMap<String, Integer>();
	private Map<String, Function> functions = new HashMap<>();
	private Map<String, Reach> reaches = new HashMap<>();
	
	private List<Arg> argList = new ArrayList<>();
	
	private Stack<Integer> sizeStack = new Stack<Integer>();
	private Stack<Map<String, Type>> typeStack = new Stack<Map<String, Type>>();
	private Stack<Map<String, Integer>> offsetStack = new Stack<Map<String, Integer>>();
	private Stack<Map<String, Reach>> reachesStack = new Stack<Map<String, Reach>>();
	
	private boolean recordingArgs;
	
	/**
	 * Pops the previous scope state off the stack, restoring previous sizes, type mappings, etc.
	 */
	public void popScope() {
		size = sizeStack.pop();
		types = typeStack.pop();
		offsets = offsetStack.pop();
		reaches = reachesStack.pop();
	}
	
	/**
	 * Pushes the current scope state on the stack
	 */
	public void pushScope() {
		sizeStack.push(size);
		typeStack.push(new HashMap<String, Type>(types));
		offsetStack.push(new HashMap<String, Integer>(offsets));
		reachesStack.push(new HashMap<String, Reach>(reaches));
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
			this.reaches.put(id, Reach.Local);
		}
		return result;
	}
	
	public boolean putGlobal(String id, Type type) {
		boolean result = !this.types.containsKey(id);
		if (result) {
			this.types.put(id, type);
			this.offsets.put(id,  this.globalSize);
			this.globalSize += type.size();
			this.reaches.put(id, Reach.Global);
		}
		return result;
	}
	
	public boolean put(String id, Type type, int size) {
		boolean result = !this.types.containsKey(id);
		if (result) {
			this.types.put(id, type);
			this.offsets.put(id, this.size);
			this.size += size;
			this.reaches.put(id, Reach.Local);
		}
		return result;
	}
	
	public String putFunc(String id, String label, Type returnType, Type... args) {
		if (!functions.containsKey(id)) functions.put(id, new Function(id, returnType));
		Function func = functions.get(id);
		if (!func.returnType.equals(returnType)) return "A function can only have one return type"
				+ " across overloads";

		return func.registerOverload(args, label);
	}
	
	public boolean startArgRecording() {
		if (!recordingArgs && argList.size() == 0) {
			recordingArgs = true;
			return true;
		} else if (recordingArgs) {
			System.out.println("Argument capture was not stopped with finishArgs() before");
			return false;
		} else if (argList.size() > 0) {
			System.out.println("There were already arguments in the list before starting");
			return false;
		}
		
		return false;
	}
	
	public boolean recordArg(String id, Type type) {
		if (!recordingArgs) {
			System.out.println("Argument recording hasn't been started");
			return false;
		}
		
		Arg arg = new Arg();
		arg.id = id;
		arg.type = type;
		argList.add(arg);
		
		return true;
	}
	
	public boolean finishArgRecording() {
		if (!recordingArgs) {
			System.out.println("Argument recording was not started");
			return false;
		}
		
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
		recordingArgs = false;
		
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
	
	public Reach reach(String id) {
		return this.reaches.get(id);
	}
}
