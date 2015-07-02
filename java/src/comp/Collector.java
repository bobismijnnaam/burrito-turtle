package comp;

import java.util.ArrayList;
import java.util.List;

import lang.BurritoBaseVisitor;
import lang.BurritoParser.ArgContext;
import lang.BurritoParser.ArrayTypeContext;
import lang.BurritoParser.BoolTypeContext;
import lang.BurritoParser.DeclContext;
import lang.BurritoParser.FuncContext;
import lang.BurritoParser.IntTypeContext;
import lang.BurritoParser.LockTypeContext;
import lang.BurritoParser.PlainArgContext;
import lang.BurritoParser.ProgramContext;
import lang.BurritoParser.SigContext;
import lang.BurritoParser.VoidTypeContext;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import sprockell.Program;
import comp.Type.Array;

/**
 * Does an initial scan of the tree to collect functions and global variable declarations
 * Initialization order of global variables is equal to the order of declaration in the tree
 */
public class Collector extends BurritoBaseVisitor<Integer> {
	// TODO: Return statement checking (can probably be done with that flow thingy)
	// Or maybe another visitor, or maybe this one. Each branch should just eventually reach a return statement
	// That actually sounds completely doable.
	private Scope scope;
	private ParseTreeProperty<Type> types = new ParseTreeProperty<>();
	private List<String> errors = new ArrayList<>();
	
	private int treeCode;
	
	public Scope generate(ParseTree tree) {
		this.scope = new Scope();
		treeCode = tree.hashCode();
		tree.accept(this);
		return scope;
	}
	
	public Scope generator(ParseTree tree, Scope scope) {
		this.scope = scope;
		tree.accept(this);
		return scope;
	}
	
	@Override
	public Integer visitProgram(ProgramContext ctx) {
		for (FuncContext ftx : ctx.func()) {
			visit(ftx);
		}
		
		for (DeclContext dtx : ctx.decl()) {
			visit(dtx);
		}
		
		return 0;
	}
	
	@Override
	public Integer visitDecl(DeclContext ctx) {
		visit(ctx.type());
		
		String id = ctx.ID().getText();
		Type type = getType(ctx.type());
		
        scope.putGlobal(id, type);

		return 0;
	}
	
	@Override
	public Integer visitFunc(FuncContext ctx) {
		visit(ctx.sig());
		
		return 0;
	}
	
	@Override
	public Integer visitSig(SigContext ctx) {
		visit(ctx.type());
		for (ArgContext atx : ctx.arg()) {
			visit(atx);
		}
		
		String funcName = ctx.ID().getText();
		String funcLabel = Program.mkLbl(ctx, "function_" + funcName + "_" + treeCode);
		Type returnType = getType(ctx.type());
		Type[] argTypes = new Type[ctx.arg().size()];
		for (int i = 0; i < ctx.arg().size(); i++) {
			ArgContext atx = ctx.arg(i);
			argTypes[i] = getType(atx);
		}
		
		String result = scope.putFunc(funcName, funcLabel, returnType, ctx.NOT() != null, argTypes);
		if (result != null) addError(ctx, result);
		
		if (ctx.NOT() != null && argTypes.length != 0) {
			addError(ctx, "Can't define function with arguments parallel: " + ctx.getText());
		}
		
		return 0;
	}
	
	@Override 
	public Integer visitPlainArg(PlainArgContext ctx) {
		visit(ctx.type());
		setType(ctx, getType(ctx.type()));

		return 0;
	}
	
	// TYPES -------------------------
	
	@Override
	public Integer visitBoolType(BoolTypeContext ctx) {
		setType(ctx, new Type.Bool());
		return 0;
	}
	
	@Override
	public Integer visitIntType(IntTypeContext ctx) {
		setType(ctx, new Type.Int());
		return 0;
	}
	
	@Override
	public Integer visitVoidType(VoidTypeContext ctx) {
		setType(ctx, new Type.Void());
		return 0;
	}
	
	@Override
	public Integer visitLockType(LockTypeContext ctx) {
		setType(ctx, new Type.Lock());
		return 0;
	}
	
	@Override
	public Integer visitArrayType(ArrayTypeContext ctx) {
		visit(ctx.type());
		
		Type.Array array = new Type.Array(getType(ctx.type()), new Integer(ctx.NUM().getText()));
		if (getType(ctx.type()).toString().equals("Array")) {
			Type.Array innerArray = (Array) getType(ctx.type());
			array.indexSize = new ArrayList<Integer>(innerArray.indexSize);
		}
		
		array.indexSize.add(new Integer(ctx.NUM().getText()));

		setType(ctx, array);

		return 0;
	}

	private void setType(ParseTree node, Type type) {
		types.put(node, type);
	}

	private Type getType(ParseTree node) {
		return types.get(node);
	}

	/**
	 * Utility functions below here
	 */

	/** Indicates if any errors were encountered in this tree listener. */
	public boolean hasErrors() {
		return !getErrors().isEmpty();
	}

	/** Returns the list of errors collected in this tree listener. */
	public List<String> getErrors() {
		return this.errors;
	}
	
	/** Checks the inferred type of a given parse tree,
	 * and adds an error if it does not correspond to the expected type.
	 */
	private boolean checkType(ParserRuleContext node, Type expected) {
		Type actual = getType(node);
		if (actual == null) {
			addError(node, "Missing inferred type of " + node.getText());
			return false;
		}
		if (!actual.equals(expected)) {
			addError(node, "Expected type '%s' but found '%s'", expected,
					actual);
			return false;
		}
		
		return true;
	}

	/** Records an error at a given parse tree node.
	 * @param ctx the parse tree node at which the error occurred
	 * @param message the error message
	 * @param args arguments for the message, see {@link String#format}
	 */
	private void addError(ParserRuleContext node, String message,
			Object... args) {
		addError(node.getStart(), message, args);
	}

	/** Records an error at a given token.
	 * @param token the token at which the error occurred
	 * @param message the error message
	 * @param args arguments for the message, see {@link String#format}
	 */
	private void addError(Token token, String message, Object... args) {
		int line = token.getLine();
		int column = token.getCharPositionInLine();
		message = String.format(message, args);
		message = String.format("Line %d:%d - %s", line, column, message);
		this.errors .add(message);
	}
}
