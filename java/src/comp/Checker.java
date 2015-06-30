package comp;

import java.util.ArrayList;
import java.util.List;

import lang.BurritoBaseListener;
import lang.BurritoParser.AndExprContext;
import lang.BurritoParser.ArgContext;
import lang.BurritoParser.ArrayExprContext;
import lang.BurritoParser.ArrayTargetContext;
import lang.BurritoParser.ArrayTypeContext;
import lang.BurritoParser.AssStatContext;
import lang.BurritoParser.BlockContext;
import lang.BurritoParser.BoolTypeContext;
import lang.BurritoParser.CharTypeContext;
import lang.BurritoParser.CharacterExprContext;
import lang.BurritoParser.DecExprContext;
import lang.BurritoParser.DeclContext;
import lang.BurritoParser.DivAssStatContext;
import lang.BurritoParser.DivExprContext;
import lang.BurritoParser.EqExprContext;
import lang.BurritoParser.ExprContext;
import lang.BurritoParser.FalseExprContext;
import lang.BurritoParser.FuncContext;
import lang.BurritoParser.FuncExprContext;
import lang.BurritoParser.GtExprContext;
import lang.BurritoParser.GteExprContext;
import lang.BurritoParser.IdExprContext;
import lang.BurritoParser.IdTargetContext;
import lang.BurritoParser.IfStatContext;
import lang.BurritoParser.IncExprContext;
import lang.BurritoParser.IntTypeContext;
import lang.BurritoParser.LtExprContext;
import lang.BurritoParser.LteExprContext;
import lang.BurritoParser.MinAssStatContext;
import lang.BurritoParser.MinExprContext;
import lang.BurritoParser.ModExprContext;
import lang.BurritoParser.MulAssStatContext;
import lang.BurritoParser.MulExprContext;
import lang.BurritoParser.NegExprContext;
import lang.BurritoParser.NumExprContext;
import lang.BurritoParser.OrExprContext;
import lang.BurritoParser.ParExprContext;
import lang.BurritoParser.PlusAssStatContext;
import lang.BurritoParser.PlusExprContext;
import lang.BurritoParser.PowExprContext;
import lang.BurritoParser.SigContext;
import lang.BurritoParser.SwitchStatContext;
import lang.BurritoParser.TrueExprContext;
import lang.BurritoParser.TypeAssignStatContext;
import lang.BurritoParser.TypeStatContext;
import lang.BurritoParser.WhileStatContext;
import lang.BurritoParser.XorExprContext;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import sprockell.Program;
import comp.Type.Array;

public class Checker extends BurritoBaseListener {
	
	/** Result of the latest call of {@link #check}. */
	private Result result;
	/** Variable scope for the latest call of {@link #check}. */
	private Scope scope;
	/** List of errors collected in the latest call of {@link #check}. */
	private List<String> errors;
	
	public Result check(ParseTree tree) throws ParseException {
		return check(tree, new Scope());
	}
	
	public Result check(ParseTree tree, Scope scope) throws ParseException {
		this.scope = scope;
		this.result = new Result();
		this.errors = new ArrayList<>();
		
		new ParseTreeWalker().walk(this, tree);
		
		if (hasErrors()) {
			throw new ParseException(getErrors());
		}
		
		return this.result;
	}
	
	//*************************
	// START OF TYPE CHECKING
	
	// GLOBALS -----------------------
	
	/**
	 * The variable is already in the scope, but the expression wasn't checked.
	 * That's what happens here.
	 */
	@Override
	public void exitDecl(DeclContext ctx) {
		String id = ctx.ID().getText();
		Type type = result.getType(ctx.type());
		
		setType(ctx.ID(), type);		
		checkType(ctx.expr(), type);
		setOffset(ctx.ID(), scope.offset(id));
		setReach(ctx.ID(), scope.reach(id));
	}
	
	// FUNCTIONS ---------------------
	
	@Override
	public void enterSig(SigContext ctx) {
		if (!scope.startArgRecording()) {
			System.out.println("Something went wrong with the arguments");
		}
	}
	
	@Override
	public void exitSig(lang.BurritoParser.SigContext ctx) {
		// Get signature of function
		String funcID = ctx.ID().getText(); // Get function ID
		Type[] args = new Type[ctx.arg().size()]; // Aggregate argument types
		for (int i = 0; i < ctx.arg().size(); i++) {
			args[i] = getType(ctx.arg(i));
			
		}
		
		// TODO: Functin overload check sometimes null; checker breaks if not checked on null
		//Will always succeed, since if we're in this phase it means that the Function phase succeeded
		Function.Overload overload = null;
		if (scope.func(funcID) != null) {
			overload = scope.func(funcID).getOverload(args);
		}
		
		// Push a scope, since we're going into function scope
		scope.pushScope();
		// Commit the pushed arguments, so they will be registered as well and are assigned
		// a negative offset
		scope.finishArgRecording();

		setFunction(ctx.parent, overload);
	};
	
	@Override
	public void exitArg(ArgContext ctx) {
		String argID = ctx.ID().getText();
		Type argType = getType(ctx.type());

		scope.recordArg(argID, argType);

		setType(ctx, argType);
	}
	
	@Override
	public void exitFunc(FuncContext ctx) {
		scope.popScope();
	}
	
	public void exitReturnStat(lang.BurritoParser.ReturnStatContext ctx) {
		// Find enclosing function
		// Should be attached to the func node
		Function.Overload func = null;
		ParseTree curr = ctx;
		while (func == null && curr != null) {
			func = getFunction(curr);
			curr = curr.getParent();
		}
		
		// If not found, report an error
		if (curr == null && func == null) {
			addError(ctx, "Return value found without enclosing function");
			return;
		}
		
		// Otherwise, attach it to the return node
		setFunction(ctx, func);
		
		checkType(ctx.expr(), func.func.returnType);
		// Set the current stack size so it can be unwound
		setStackSize(ctx, scope.getCurrentStackSize());
	};
	
	// TYPES -------------------------
	
	@Override
	public void exitBoolType(BoolTypeContext ctx) {
		setType(ctx, new Type.Bool());
	}
	
	@Override
	public void exitIntType(IntTypeContext ctx) {
		setType(ctx, new Type.Int());
	}
	
	@Override
	public void exitCharType(CharTypeContext ctx) {
		setType(ctx, new Type.Char());
	}
	
	@Override
	public void exitArrayType(ArrayTypeContext ctx) {
		Type.Array array = new Type.Array(getType(ctx.type()), new Integer(ctx.NUM().getText()));
		
		// TODO: Though the third one was the cleanest, but leaving the alternatives here for future reference
//		if (getType(ctx.type()).toString().equals("Array")) {
//		if (getType(ctx.type()).equals((new Type.Array(null, 0)))) {
		if (getType(ctx.type()) instanceof Type.Array) {
			Type.Array innerArray = (Array) getType(ctx.type());
			array.indexSize = new ArrayList<Integer>(innerArray.indexSize);
		}
		
		array.indexSize.add(new Integer(ctx.NUM().getText()));
		setType(ctx, array);
	}
	
	@Override
	public void exitIdTarget(IdTargetContext ctx) {
		String id = ctx.ID().getText();

		setType(ctx, scope.type(id));
		if (checkType(ctx, getType(ctx))) {
			setOffset(ctx, scope.offset(id));
			setReach(ctx, scope.reach(id));
		}
	}
	
	// TODO: Bounds checking/dimension checking here?
	@Override
	public void exitArrayTarget(ArrayTargetContext ctx) {
		String id = ctx.ID().getText();
		Type.Array array = (Array) this.scope.type(id);

		if (array == null) {
			addError(ctx, "Missing inferred type of " + ctx.ID().getText());
		} else {
			setType(ctx.ID(), array);
			setType(ctx, array.getBaseType());
			setOffset(ctx.ID(), this.scope.offset(id));
			setReach(ctx, this.scope.reach(id));
			
			for (ExprContext expr : ctx.expr()) 
				checkType(expr, new Type.Int());
		}
	}
	
	// EXPR -----------------------------
	@Override
	public void exitFalseExpr(FalseExprContext ctx) {
		setType(ctx, new Type.Bool());
	}
	
	@Override
	public void exitTrueExpr(TrueExprContext ctx) {
		setType(ctx, new Type.Bool());
	}
	
	@Override
	public void exitNumExpr(NumExprContext ctx) {
		setType(ctx, new Type.Int());
	}
	
	@Override
	public void exitCharacterExpr(CharacterExprContext ctx) {
		setType(ctx, new Type.Char());
	}
	
	@Override
	public void exitNegExpr(NegExprContext ctx) {
		checkType(ctx.expr(), new Type.Int());
		setType(ctx, new Type.Int());
	}
	
	@Override
	public void exitIdExpr(IdExprContext ctx) {
		String id = ctx.getText();
		Type type = this.scope.type(id);
		setType(ctx, type);
		checkType(ctx, type);
		setOffset(ctx, this.scope.offset(id));
		setReach(ctx, this.scope.reach(id));
	}
	
	@Override
	public void exitParExpr(ParExprContext ctx) {
		setType(ctx, getType(ctx.expr()));
	}
	
	@Override
	public void exitArrayExpr(ArrayExprContext ctx) {
		String id = ctx.ID().getText();
		Type.Array array = (Array) this.scope.type(id);
		if (array == null) {
			addError(ctx, "Missing inferred type of " + ctx.ID().getText());
		} else {
			setType(ctx.ID(), array);
			
			setType(ctx, array.getBaseType());
			setOffset(ctx.ID(), this.scope.offset(id));
			setReach(ctx.ID(), this.scope.reach(id));
			for (ExprContext expr : ctx.expr())
				checkType(expr, new Type.Int()); 
				// TODO: Check amount of dimensions as well?
		}
	}
	
	@Override
	public void exitIncExpr(IncExprContext ctx) {
		String id = ctx.target().getText();

		Type type = this.scope.type(id);
		setType(ctx, type);
		// TODO: Once we support more types than just ints (such as char? Doesn't matter
		// since it works out but still), we should look at this
		checkType(ctx, new Type.Int());
		setOffset(ctx, this.scope.offset(id));
		setReach(ctx, this.scope.reach(id));
	}
	
	@Override
	public void exitDecExpr(DecExprContext ctx) {
		String id = ctx.target().getText();

		Type type = this.scope.type(id);
		setType(ctx, type);
		// TODO: Here as well - there might be some other increment than just floats
		checkType(ctx, new Type.Int());
		setOffset(ctx, this.scope.offset(id));
		setReach(ctx, this.scope.reach(id));
	}
	
	// TODO At compare check if it is a type that can be compared
	@Override
	public void exitGtExpr(GtExprContext ctx) {
		checkTypeCompare(ctx);
	}
	
	@Override
	public void exitLtExpr(LtExprContext ctx) {
		checkTypeCompare(ctx);
	}
	
	@Override
	public void exitEqExpr(EqExprContext ctx) {
		checkTypeCompare(ctx);
	}
	
	@Override
	public void exitLteExpr(LteExprContext ctx) {
		checkTypeCompare(ctx);
	}
	
	@Override
	public void exitGteExpr(GteExprContext ctx) {
		checkTypeCompare(ctx); 
	}
	
	@Override
	public void exitAndExpr(AndExprContext ctx) {
		checkTypeCompare(ctx);
	}
	
	@Override
	public void exitOrExpr(OrExprContext ctx) {
		checkTypeCompare(ctx);
	}
	
	@Override
	public void exitXorExpr(XorExprContext ctx) {
		checkTypeCompare(ctx);
	}
	
	void checkTypeCompare(ParserRuleContext ctx) {
		Type type1 = getType(ctx.getChild(0));
		Type type2 = getType(ctx.getChild(2));
		checkType((ParserRuleContext) ctx.getChild(0), type2);
		checkType((ParserRuleContext) ctx.getChild(2), type1);
		setType(ctx, new Type.Bool());
	}
	
	@Override
	public void exitModExpr(ModExprContext ctx) {
		checkTypeOp(ctx);
	}
	
	@Override
	public void exitDivExpr(DivExprContext ctx) {
		checkTypeOp(ctx);
	}
	
	@Override
	public void exitMulExpr(MulExprContext ctx) {
		checkTypeOp(ctx);
	}
	
	@Override
	public void exitPlusExpr(PlusExprContext ctx) {
		checkTypeOp(ctx);
	}
	
	@Override
	public void exitMinExpr(MinExprContext ctx) {
		checkTypeOp(ctx);
	}
	
	@Override
	public void exitPowExpr(PowExprContext ctx) {
		checkTypeOp(ctx);
	}
	
	void checkTypeOp(ParserRuleContext ctx) {
		checkType((ParserRuleContext) ctx.getChild(0), new Type.Int());
		checkType((ParserRuleContext) ctx.getChild(2), new Type.Int());
		setType(ctx, new Type.Int());
	}
	
	@Override
	public void exitFuncExpr(FuncExprContext ctx) {
		String funcID = ctx.ID().getText();
		
		// Check if function exists
		if (!scope.containsFunc(funcID)) {
			addError(ctx, "Function \"" + funcID + "\" not declared");
			return;
		}
		
		Function func = scope.func(funcID);
		
		// Build argument list of function call
		Type[] callArgs = new Type[ctx.expr().size()];
		for (int i = 0; i < ctx.expr().size(); i++) {
			callArgs[i] = getType(ctx.expr(i));
		}
		Function.Overload overload = func.getOverload(callArgs); 
		if (overload == null) {
			addError(ctx, "No appropriate overload found for function \"" + func.id + "\". Available overloads are: " + func);
		}
		
		// Set return type of function as expression type
		setType(ctx, scope.func(funcID).returnType);
		// Attach the function to the ID for the generator
		setFunction(ctx.ID(), overload);
	}
	
	// STATS ----------------------------
	@Override
	public void exitTypeAssignStat(TypeAssignStatContext ctx) {
		String id = ctx.ID().getText();
		Type type = result.getType(ctx.type());
		
		scope.put(id, type);

		setType(ctx.ID(), type);	
		checkType(ctx.expr(), type);
		setOffset(ctx.ID(), scope.offset(id));
		setReach(ctx.ID(), scope.reach(id));
	}
	
	@Override
	public void exitTypeStat(TypeStatContext ctx) {
		String id = ctx.ID().getText();
		Type type = result.getType(ctx.type());
		
		// TODO: Is this correct? Just want to make sure - Bob
//		if (ctx.getChildCount() > 0) {
//		if (type instanceof Type.Array) {
			scope.put(id, type);
//		} else {
//			scope.put(id, type);
//		}
		
		setType(ctx.ID(), type);		
		setOffset(ctx.ID(), scope.offset(id));
		setReach(ctx.ID(), scope.reach(id));
	}

	
	@Override
	public void exitWhileStat(WhileStatContext ctx) {
		checkType(ctx.expr(), new Type.Bool());
	}
	
	@Override
	public void exitIfStat(IfStatContext ctx) {
		checkType(ctx.expr(), new Type.Bool());
	}
	
	// += -= /= *= stats
	@Override
	public void exitPlusAssStat(PlusAssStatContext ctx) {
		checkAssignment(ctx);
	}
	
	@Override
	public void exitMinAssStat(MinAssStatContext ctx) {
		checkAssignment(ctx);
	}
	
	@Override
	public void exitDivAssStat(DivAssStatContext ctx) {
		checkAssignment(ctx);
	}
	
	@Override
	public void exitMulAssStat(MulAssStatContext ctx) {
		checkAssignment(ctx);
	}
	
	private void checkAssignment(ParseTree ctx) {
		String id = ctx.getChild(0).getText();
		
//		Type type = this.scope.type(id);
		// TODO: This seemed cleaner, but leaving it here for future reference
		Type type = getType(ctx.getChild(0));
		if (type instanceof Type.Array) {
			type = ((Array) type).getBaseType();
		}
//		if (id.contains("[")) {
//			id = id.split("\\[")[0];
//			Type.Array array = (Array) this.scope.type(id);
//			if (array != null) {
//				type = array.elemType;
//				while (type.toString().equals("Array")) {
//					type = ((Type.Array) type).elemType;
//				}
//			}
//		}
		
		if (type != null) {
			if (checkType((ParserRuleContext) ctx.getChild(3), type)) {
				setOffset((ParserRuleContext) ctx.getChild(0), scope.offset(id));
				setReach(ctx, scope.reach(id));
			}
		} else {
			addError((ParserRuleContext) ctx.getChild(0), "Missing inferred type of " + ctx.getChild(0).getText());
		}
	}
	
	@Override
	public void exitAssStat(AssStatContext ctx) {
		// Not sure if these comments are ready to be deleted :p
//		String id = ctx.target().;
		
//		Type type = this.scope.type(id);
//		if (id.contains("[")) {
//			id = id.split("\\[")[0];
//			Type.Array array = (Array) this.scope.type(id);
//			if (array != null) {
//				type = array.elemType;
//				while (type.toString().equals("Array")) {
//					type = ((Type.Array) type).elemType;
//				}
//			}
//		}
				
		Type type = getType(ctx.target());
//		String id = null;
		
		if (type != null) {
			if (checkType(ctx.expr(), type)) {
//				setOffset(ctx.target(), scope.offset(id));
//				setReach(ctx.target(), scope.reach(id));
				// ctx.target sets the offset and the reach.
				// Leaving the comments here for future reference
			}
		} else {
			addError(ctx.target(), "Missing inferred type of " + ctx.target().getText());
		}
	}
	
	// SWITCH
	@Override
	public void exitSwitchStat(SwitchStatContext ctx) {
		
	}
	
	// BLOCK THINGY -------------------------------
	@Override
	public void enterBlock(BlockContext ctx) {
		scope.pushScope();
	}
	
	@Override
	public void exitBlock(BlockContext ctx) {
		scope.popScope();
	}
	
	// END OF TYPE CHECKING
	//**************************
	
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
		this.errors.add(message);
	}

	/** Convenience method to add an offset to the result. */
	private void setOffset(ParseTree node, Integer offset) {
		this.result.setOffset(node, offset);
	}

	/** Convenience method to add a type to the result. */
	private void setType(ParseTree node, Type type) {
		this.result.setType(node, type);
	}

	/** Returns the type of a given expression or type node. */
	private Type getType(ParseTree node) {
		return this.result.getType(node);
	}
	
	private void setFunction(ParseTree node, Function.Overload function) {
		this.result.setFunction(node, function);
	}
	
	private Function.Overload getFunction(ParseTree node) {
		return this.result.getFunction(node);
	}

	private void setStackSize(ParseTree node, int currentStackSize) {
		this.result.setStackSize(node, currentStackSize);
	}

	private void setReach(ParseTree node, Reach reach) {
		this.result.setReach(node, reach);
	}
	
	private Reach getReach(ParseTree node) {
		return this.result.getReach(node);
	}
}
