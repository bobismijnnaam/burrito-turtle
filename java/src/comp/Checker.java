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
import lang.BurritoParser.LenExprContext;
import lang.BurritoParser.LockStatContext;
import lang.BurritoParser.LockTypeContext;
import lang.BurritoParser.LtExprContext;
import lang.BurritoParser.LteExprContext;
import lang.BurritoParser.MinAssStatContext;
import lang.BurritoParser.MinExprContext;
import lang.BurritoParser.ModExprContext;
import lang.BurritoParser.MulAssStatContext;
import lang.BurritoParser.MulExprContext;
import lang.BurritoParser.NegExprContext;
import lang.BurritoParser.NotExprContext;
import lang.BurritoParser.NumExprContext;
import lang.BurritoParser.OrExprContext;
import lang.BurritoParser.OutStatContext;
import lang.BurritoParser.ParExprContext;
import lang.BurritoParser.PlainArgContext;
import lang.BurritoParser.PlusAssStatContext;
import lang.BurritoParser.PlusExprContext;
import lang.BurritoParser.PowExprContext;
import lang.BurritoParser.SigContext;
import lang.BurritoParser.StartStatContext;
import lang.BurritoParser.SwitchStatContext;
import lang.BurritoParser.TrueExprContext;
import lang.BurritoParser.TypeAssignStatContext;
import lang.BurritoParser.TypeStatContext;
import lang.BurritoParser.UnlockStatContext;
import lang.BurritoParser.VoidTypeContext;
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
		
		result.setGlobalSize(scope.getGlobalSize());
		result.setSprockells(scope.getSprockells());
		
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

		if (type instanceof Type.Array) {
			((Type.Array) type).setOuter();
		}
		
		setOffset(ctx.ID(), scope.offset(id));
		setReach(ctx.ID(), scope.reach(id));
		
		if (ctx.expr() != null) {
			if (!type.assignable()) {
				addError(ctx, "It is prohibited to assign a value to type " + type);
			}
			
			setType(ctx.ID(), type);		
			checkType(ctx.expr(), type);
		}	
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
		
		// Will always succeed, since if we're in this phase it means that the Function phase succeeded
		Function.Overload overload = null;
		if (scope.func(funcID) != null) {
			overload = scope.func(funcID).getOverload(args);
		} else {
			addError(ctx, "Sanity check failed: function " + funcID + " not declared");
		}
		
		if (funcID.equals(Generator.MAINMETHOD)) {
			if (!(overload.func.returnType instanceof Type.Void)) {
				addError(ctx, "program() method should have return type void, not type " + overload.func.returnType);
			}
		}
		
		// Push a scope, since we're going into function scope
		scope.pushScope();
		// Commit the pushed arguments, so they will be registered as well and are assigned
		// a negative offset
		scope.finishArgRecording();

		setFunction(ctx.parent, overload);
	};
	
	@Override
	public void exitPlainArg(PlainArgContext ctx) {
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
		
		if (ctx.expr() == null) {
			if (!(func.func.returnType instanceof Type.Void)) {
				addError(ctx, "Return statement should contain a value since return type of this function is non-void");
			}
		} else {
			checkType(ctx.expr(), func.func.returnType);
		}
		
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
	public void exitVoidType(VoidTypeContext ctx) {
		setType(ctx, new Type.Char());
	}
	
	@Override
	public void exitLockType(LockTypeContext ctx) {
		setType(ctx, new Type.Lock());
	}
	
	@Override
	public void exitArrayType(ArrayTypeContext ctx) {
		Type.Array array = new Type.Array(getType(ctx.type()), new Integer(ctx.NUM().getText()));
		
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
		}
	}
	
	@Override
	public void exitIncExpr(IncExprContext ctx) {
		String id = ctx.target().getText();

		Type type = this.scope.type(id);
		setType(ctx, type);
		
		if (!(type instanceof Type.Int || type instanceof Type.Char)) {
			addError(ctx, "Type " + type + " is not incrementable");
		}
		
		setOffset(ctx, this.scope.offset(id));
		setReach(ctx, this.scope.reach(id));
	}
	
	@Override
	public void exitDecExpr(DecExprContext ctx) {
		String id = ctx.target().getText();

		Type type = this.scope.type(id);
		setType(ctx, type);
		checkType(ctx, new Type.Int());
		setOffset(ctx, this.scope.offset(id));
		setReach(ctx, this.scope.reach(id));
	}
	
	@Override
	public void exitGtExpr(GtExprContext ctx) {
		checkTypeCompare(ctx, new Type.Int());
	}
	
	@Override
	public void exitLtExpr(LtExprContext ctx) {
		checkTypeCompare(ctx, new Type.Int());
	}
	
	@Override
	public void exitEqExpr(EqExprContext ctx) {
		checkTypeCompare(ctx, new Type.Int());
	}
	
	@Override
	public void exitLteExpr(LteExprContext ctx) {
		checkTypeCompare(ctx, new Type.Int());
	}
	
	@Override
	public void exitGteExpr(GteExprContext ctx) {
		checkTypeCompare(ctx, new Type.Int()); 
	}
	
	@Override
	public void exitAndExpr(AndExprContext ctx) {
		checkTypeCompare(ctx, new Type.Bool());
	}
	
	@Override
	public void exitOrExpr(OrExprContext ctx) {
		checkTypeCompare(ctx, new Type.Bool());
	}
	
	@Override
	public void exitXorExpr(XorExprContext ctx) {
		checkTypeCompare(ctx, new Type.Bool());
	}
	
	void checkTypeCompare(ParserRuleContext ctx, Type desired) {
		Type type1 = getType(ctx.getChild(0));
		Type type2 = getType(ctx.getChild(2));
		checkType((ParserRuleContext) ctx.getChild(0), type2);
		checkType((ParserRuleContext) ctx.getChild(2), type1);
		if (!(type1.equals(desired) && type2.equals(desired))) {
			addError(ctx, "Left or right hand side does not match. Found: " + type1 + " and " + type2 + ", expected " + desired);
		}
		
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
	
	@Override
	public void exitNotExpr(NotExprContext ctx) {
		checkType(ctx.expr(), new Type.Bool());
		setType(ctx, new Type.Bool());
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
	
	@Override
	public void exitLenExpr(LenExprContext ctx) {
		String id = ctx.ID().getText();
		Type type = scope.type(id);
		if (!(type instanceof Type.Array)) {
			addError(ctx, "len can only be used with arrays");
		}
		setType(ctx, new Type.Int());
		setOffset(ctx.ID(), scope.offset(id));
		setReach(ctx.ID(), scope.reach(id));
	}
	
	// STATS ----------------------------
	
	@Override
	public void exitOutStat(OutStatContext ctx) {
		if (ctx.expr() == null)
			return;
		
		Type type = getType(ctx.expr());
		
		if (!(type instanceof Type.Int || type instanceof Type.Bool || type instanceof Type.Char)) {
			addError(ctx, "Pipe operator does not support type " + type);
		}
	}
	
	@Override
	public void exitStartStat(StartStatContext ctx) {
		String func = ctx.ID().getText();
		if (!scope.containsFunc(func)) {
			addError(ctx, "Function " + func + " not declared");
			return;
		}
		
		setFunction(ctx, scope.func(func).getOverload(new Type[0]));
	}
	
	@Override
	public void exitTypeAssignStat(TypeAssignStatContext ctx) {
		// TODO: Handle arrays
		String id = ctx.ID().getText();
		Type type = result.getType(ctx.type());
		
		if (!type.assignable()) {
			addError(ctx, "It's prohibited to assign a value to something of type " + type);
		}
		
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
		
		if (type instanceof Type.Lock) {
			addError(ctx, "Declaring a lock in local scope is prohibited");
		}
		
		if (type instanceof Type.Array) {
			((Type.Array) type).setOuter();
		}
		
		scope.put(id, type);
		
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
		
		Type type = getType(ctx.getChild(0));
		if (type instanceof Type.Array) {
			type = ((Array) type).getBaseType();
		}
		
		if (type != null) {
			if (!type.assignable()) {
				addError((ParserRuleContext) ctx, "It is prohibited to assign a value to type " + type);
			}
			
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
		Type type = getType(ctx.target());
		
		if (type != null) {
			checkType(ctx.expr(), type);
		} else {
			addError(ctx.target(), "Missing inferred type of " + ctx.target().getText());
		}
	}
	
	@Override
	public void exitLockStat(LockStatContext ctx) {
		setType(ctx, scope.type(ctx.ID().getText()));
		checkType(ctx, new Type.Lock());
		
		setOffset(ctx, scope.offset(ctx.ID().getText()));
	}
	
	@Override
	public void exitUnlockStat(UnlockStatContext ctx) {
		setType(ctx, scope.type(ctx.ID().getText()));
		checkType(ctx, new Type.Lock());

		setOffset(ctx, scope.offset(ctx.ID().getText()));
		
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
