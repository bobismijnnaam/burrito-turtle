package comp;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

public class Result {
	/** Mapping from statements and expressions to the atomic
	 * subtree that is their entry in the control flow graph. */
	private final ParseTreeProperty<ParserRuleContext> entries = new ParseTreeProperty<>();
	/** Mapping from expressions to types. */
	private final ParseTreeProperty<Type> types = new ParseTreeProperty<>();
	/** Mapping from variables to coordinates. */
	private final ParseTreeProperty<Integer> offsets = new ParseTreeProperty<>();
	private final ParseTreeProperty<Function.Overload> functions = new ParseTreeProperty<>();
	private final ParseTreeProperty<Integer> stackSizes = new ParseTreeProperty<>();
	private final ParseTreeProperty<Reach> reaches = new ParseTreeProperty<>();
	
	private int globalSize;
	private int sprockells;

	/** Adds an association from parse tree node to the flow graph entry. */
	public void setEntry(ParseTree node, ParserRuleContext entry) {
		this.entries.put(node, entry);
	}

	/** Returns the flow graph entry associated 
	 * with a given parse tree node. */
	public ParserRuleContext getEntry(ParseTree node) {
		return this.entries.get(node);
	}

	/** Adds an association from a parse tree node containing a 
	 * variable reference to the offset
	 * of that variable. */
	public void setOffset(ParseTree node, int offset) {
		this.offsets.put(node, offset);
	}

	/** Returns the declaration offset of the variable 
	 * accessed in a given parse tree node. */
	public int getOffset(ParseTree node) {
		return this.offsets.get(node);
	}

	/** Adds an association from a parse tree expression, type,
	 * or assignment target node to the corresponding (inferred) type. */
	public void setType(ParseTree node, Type type) {
		this.types.put(node, type);
	}

	/** Returns the type associated with a given parse tree node. */
	public Type getType(ParseTree node) {
		return this.types.get(node);
	}

	public void setFunction(ParseTree node, Function.Overload function) {
		this.functions.put(node, function);
	}
	
	public Function.Overload getFunction(ParseTree node) {
		return this.functions.get(node);
	}

	public void setStackSize(ParseTree node, int currentStackSize) {
		this.stackSizes.put(node, currentStackSize);
	}
	
	public int getStackSize(ParseTree node) {
		return this.stackSizes.get(node);
	}
	
	public void setReach(ParseTree node, Reach reach) {
		this.reaches.put(node,  reach);
	}
	
	public Reach getReach(ParseTree node) {
		return this.reaches.get(node);
	}
	
	/**
	 * @return The address where the sprockell segmenst start. It starts after the global values, and after the important value block
	 * (Which, as you can see, is the size of 2 words - an allocator lock and an IO lock)
	 */
	public int getSprockellSegment() {
		return globalSize + 2;
	}
	
	public int getGlobalSize() {
		return this.globalSize;
	}
	
	public void setGlobalSize(int globalSize) {
		this.globalSize = globalSize;
	}
	
	public void setSprockells(int sprockells) {
		this.sprockells = sprockells;
	}
	
	public int minimumSprockells() {
		return sprockells;
	}
}
