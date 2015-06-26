package comp;

import java.util.ArrayList;
import java.util.List;

import lang.BurritoBaseVisitor;
import lang.BurritoParser.ArgContext;
import lang.BurritoParser.ArrayTypeContext;
import lang.BurritoParser.BoolTypeContext;
import lang.BurritoParser.FuncContext;
import lang.BurritoParser.IntTypeContext;
import lang.BurritoParser.ProgramContext;
import lang.BurritoParser.SigContext;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import sprockell.Program;

import comp.Type.Array;

public class FunctionCollector extends BurritoBaseVisitor<Integer> {
	// TODO: Return statement checking (can probably be done with that flow thingy)
	// Or maybe another visitor, or maybe this one. Each branch should just eventually reach a return statement
	// That actually sounds completely doable.
	// TODO: Make sure when an array is returned it is properly saved on the stack and stuff
	// TODO: void return type
	// TODO: When comparing two overloads if they have the same arguments, don't forget to check
	// if two array arguments are the same size as well. This means, after doing equals(), you have
	// to upcast them to array and check their sizes!
	// TODO: Make functions type aware (when doing a function with return type int[2], make it leave 
	// two integers on the stack, and act accordingly after the expression has evaluated (popping them off
	// the stack if not needed anymore, etc.)
	private Scope scope;
	private ParseTreeProperty<Type> types = new ParseTreeProperty<>();
	private List<String> errors = new ArrayList<>();
	
	public Scope generate(ParseTree tree) {
		this.scope = new Scope();
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
		String funcLabel = Program.mkLbl(ctx, "function_" + funcName);
		Type returnType = getType(ctx.type());
		Type[] argTypes = new Type[ctx.arg().size()];
		for (int i = 0; i < ctx.arg().size(); i++) {
			ArgContext atx = ctx.arg(i);
			argTypes[i] = getType(atx);
		}
		
		String result = scope.putFunc(funcName, funcLabel, returnType, argTypes);
		if (result != null) addError(ctx, result);
		
		return 0;
	}
	
	@Override
	public Integer visitArg(ArgContext ctx) {
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
