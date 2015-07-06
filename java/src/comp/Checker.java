package comp;

import static sprockell.Operator.Which.*;

import java.util.ArrayList;
import java.util.List;

import lang.BurritoBaseListener;
import lang.BurritoParser.AndExprContext;
import lang.BurritoParser.ArgContext;
import lang.BurritoParser.ArrayExprContext;
import lang.BurritoParser.ArrayTypeContext;
import lang.BurritoParser.AssStatContext;
import lang.BurritoParser.BlockContext;
import lang.BurritoParser.BoolTypeContext;
import lang.BurritoParser.CharTypeContext;
import lang.BurritoParser.CharacterExprContext;
import lang.BurritoParser.DecExprContext;
import lang.BurritoParser.DeclContext;
import lang.BurritoParser.DerefExprContext;
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
import lang.BurritoParser.IfStatContext;
import lang.BurritoParser.ImpContext;
import lang.BurritoParser.IncExprContext;
import lang.BurritoParser.IntTypeContext;
import lang.BurritoParser.LitExprContext;
import lang.BurritoParser.LiteralExprContext;
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
import lang.BurritoParser.NeqExprContext;
import lang.BurritoParser.NotExprContext;
import lang.BurritoParser.NumExprContext;
import lang.BurritoParser.OrExprContext;
import lang.BurritoParser.OutStatContext;
import lang.BurritoParser.ParExprContext;
import lang.BurritoParser.PlusAssStatContext;
import lang.BurritoParser.PlusExprContext;
import lang.BurritoParser.PointerTypeContext;
import lang.BurritoParser.PowExprContext;
import lang.BurritoParser.SeqExprContext;
import lang.BurritoParser.SigContext;
import lang.BurritoParser.StartStatContext;
import lang.BurritoParser.StringExprContext;
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

import sprockell.Operator;

import comp.Type.Array;
import comp.Type.ArrayLiteral;
import comp.Type.Bool;
import comp.Type.Char;
import comp.Type.Int;
import comp.Type.Pointer;
import comp.Type.StringLiteral;
import comp.Type.Void;

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
	
	// IMPORTS ------------------------
	@Override
	public void exitImp(ImpContext ctx) {
		
	}
	
	// GLOBALS -----------------------
	
	/**
	 * The variable is already in the scope, but the expression wasn't checked.
	 * That's what happens here.
	 */
	@Override
	public void exitDecl(DeclContext ctx) {
		String id = ctx.ID().getText();
		Type type = scope.type(ctx.ID().getText());

		setOffset(ctx.ID(), scope.offset(id));
		setReach(ctx.ID(), scope.reach(id));
		
		// TODO: Sprint => Sprockell Interpreter
		
		if (ctx.expr() != null) {
			if (getType(ctx.expr()) instanceof StringLiteral) {}
			else if (getType(ctx.expr()) instanceof ArrayLiteral) {}
			else {
				if (!type.assignable()) {
					addError(ctx, "It is prohibited to assign a value to type " + type);
					return;
				}
				
				checkType(ctx.expr(), type);
			}
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
		setType(ctx, new Type.Void());
	}
	
	@Override
	public void exitLockType(LockTypeContext ctx) {
		setType(ctx, new Type.Lock());
	}
	
	@Override
	public void exitPointerType(PointerTypeContext ctx) {
		Type type = getType(ctx.type());
		setType(ctx, new Type.Pointer(type));
	}
	
	@Override
	public void exitArrayType(ArrayTypeContext ctx) {
		Type left = getType(ctx.type());
		int size = -1;
		if (ctx.NUM() != null) {
			size = new Integer(ctx.NUM().getText());
		}

		if (left instanceof Array) {
			Array right = new Array(new Void(), size); // Void will be filled in later
			((Array) left).insertLayer(right);
			setType(ctx, left);
		} else {
			Array arr = new Array(left, size);
			setType(ctx, arr);
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

		String c = ctx.CHARACTER().getText();
		String cStrip = c.replaceAll("'", "");
		
		if (cStrip.equals("\\0")) {
		} else if (cStrip.equals("\\n")) {
		} else if (cStrip.length() == 1){
		} else {
			addError(ctx, "Invalid character literal: " + ctx.getText());
		}
	}
	
	@Override
	public void exitStringExpr(StringExprContext ctx) {
		StringLiteral sl = new StringLiteral();
		sl.content = ctx.getText().substring(1, ctx.getText().length() - 1);
		setType(ctx, sl);
	}
	
	@Override
	public void exitNegExpr(NegExprContext ctx) {
		checkType(ctx.expr(), new Type.Int());
		setType(ctx, new Type.Int());
		setAssignable(ctx, false);
	}
	
	@Override
	public void exitLiteralExpr(LiteralExprContext ctx) {
		setType(ctx, getType(ctx.litExpr()));
		setAssignable(ctx, false);
	}
	
	@Override
	public void exitSeqExpr(SeqExprContext ctx) {
		Type first = getType(ctx.litExpr(0));
		
		if (!(first instanceof Bool || first instanceof Int || first instanceof Char)) {
			addError(ctx, "An array literal can only contain bools, ints, or chars");
			return;
		}
		
		boolean allTheSame = true;
		for (int i = 1; i < ctx.litExpr().size(); i++) {
			allTheSame = allTheSame && first.equals(getType(ctx.litExpr(i)));
		}
		
		if (!allTheSame) {
			addError(ctx, "In an array literal can only contain one type, in this case " + first);
			return;
		}

		ArrayLiteral thisType = new ArrayLiteral();
		thisType.elemType = first;
		thisType.arrSize = ctx.litExpr().size();
		thisType.contents = new int[ctx.litExpr().size()];
		
		if (thisType.elemType instanceof Int) {
			for (int i = 0; i < thisType.arrSize; i++) {
				String conts = ctx.litExpr(i).getText();
				thisType.contents[i] = new Integer(conts); 
			}
		} else if (thisType.elemType instanceof Bool) {
			for (int i = 0; i < thisType.arrSize; i++) {
				String conts = ctx.litExpr(i).getText();
				if (conts.toLowerCase().charAt(0) == 't') {
					thisType.contents[i] = 1;
				} else {
					thisType.contents[i] = 0;
				}
			}
		} else if (thisType.elemType instanceof Char) {
			for (int i = 0; i < thisType.arrSize; i++) {
				thisType.contents[i] = ctx.litExpr(i).getText().charAt(1);
			}			
		}
		
		setType(ctx, thisType);
	
		return;
	}
	
	@Override
	public void exitIdExpr(IdExprContext ctx) {
		String id = ctx.getText();
		Type type = this.scope.type(id);
		
		if (!scope.contains(id)) {
			addError(ctx, "Variable " + id + " not declared");
			setType(ctx, null);
			setOffset(ctx, -1);
			setReach(ctx, Reach.Local);
			setAssignable(ctx, false);
			return;
		}
		
		setType(ctx, type);
		setOffset(ctx, this.scope.offset(id)); 
		setReach(ctx, this.scope.reach(id)); 
		setAssignable(ctx, true);

//		if (checkType(ctx, type)) { // This looks like bollocks
//		}
	}
	
	@Override
	public void exitParExpr(ParExprContext ctx) {
		setType(ctx, getType(ctx.expr()));
		setAssignable(ctx, getAssignable(ctx.expr()));
	}
	
	@Override
	public void exitIncExpr(IncExprContext ctx) {
		Type type = getType(ctx.expr());
		if (!getAssignable(ctx.expr()) || !(type instanceof Type.Int || type instanceof Type.Char || type instanceof Type.Pointer)) {
			addError(ctx, "Can't increment target " + ctx.expr().getText());
			return;
		}
		setAssignable(ctx, false);
		
		setType(ctx, type);
	}
	
	@Override
	public void exitDecExpr(DecExprContext ctx) {
		Type type = getType(ctx.expr());
		if (!getAssignable(ctx.expr()) || !(type instanceof Type.Int || type instanceof Type.Char || type instanceof Type.Pointer)) {
			addError(ctx, "Can't decrement target " + ctx.expr().getText());
			return;
		}

		setAssignable(ctx, false);
		setType(ctx, type);
	}
	
	@Override
	public void exitGtExpr(GtExprContext ctx) {
		checkTypeCompare(ctx.expr(0), ctx.expr(1), false);
	}
	
	@Override
	public void exitLtExpr(LtExprContext ctx) {
		checkTypeCompare(ctx.expr(0), ctx.expr(1), false);
	}
	
	@Override
	public void exitEqExpr(EqExprContext ctx) {
		checkTypeCompare(ctx.expr(0), ctx.expr(1), false);
	}
	
	@Override
	public void exitNeqExpr(NeqExprContext ctx) {
		checkTypeCompare(ctx.expr(0), ctx.expr(1), false);
	}
	
	@Override
	public void exitLteExpr(LteExprContext ctx) {
		checkTypeCompare(ctx.expr(0), ctx.expr(1), false);
	}
	
	@Override
	public void exitGteExpr(GteExprContext ctx) {
		checkTypeCompare(ctx.expr(0), ctx.expr(1), true); 
	}
	
	@Override
	public void exitAndExpr(AndExprContext ctx) {
		checkTypeCompare(ctx.expr(0), ctx.expr(1), true);
	}
	
	@Override
	public void exitOrExpr(OrExprContext ctx) {
		checkTypeCompare(ctx.expr(0), ctx.expr(1), true);
	}
	
	@Override
	public void exitXorExpr(XorExprContext ctx) {
		checkTypeCompare(ctx.expr(0), ctx.expr(1), true);
	}
	
	// Proper addition/subtraction for pointers
	void checkTypeCompare(ExprContext leftExpr, ExprContext rightExpr, boolean boolOp) {
		Type left = getType(leftExpr);
		Type right = getType(rightExpr);
		ParserRuleContext ctx = (ParserRuleContext) leftExpr.parent;

		if (!left.equals(right)) {
			addError(ctx, "Left and right hand side types do not match:"
					+ left
					+ " =/= "
					+ right);
			return;
		}
		
		if (boolOp) {
			if (!(left instanceof Bool)) {
				addError(ctx, "Cannot do a boolean operation on type " + left);
			}
		} else {
			if (!(left instanceof Char || left instanceof Int || left instanceof Pointer)) {
				addError(ctx, "Cannot do compare operation on type " + left);
			}
		}

		setType(ctx, new Type.Bool());
		setAssignable(ctx, false);
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
		Type left = getType(ctx.expr(0));
		Type right = getType(ctx.expr(1));

		if (left instanceof Pointer && right instanceof Int) {
			setType(ctx, left);
			setAssignable(ctx, false);
		} else {
			checkTypeOp(ctx);
		}
	}
	
	@Override
	public void exitMinExpr(MinExprContext ctx) {
		Type left = getType(ctx.expr(0));
		Type right = getType(ctx.expr(1));
		
		if (left instanceof Pointer && right instanceof Int) {
			setType(ctx, left);
			setAssignable(ctx, false);
		} else {
			checkTypeOp(ctx);
		}
	}
	
	@Override
	public void exitPowExpr(PowExprContext ctx) {
		checkTypeOp(ctx);
	}
	
	@Override
	public void exitNotExpr(NotExprContext ctx) {
		checkType(ctx.expr(), new Type.Bool());
		setType(ctx, new Type.Bool());
		setAssignable(ctx, false);
	}
	
	void checkTypeOp(ParserRuleContext ctx) {
		Type left = getType(ctx.getChild(0));
		Type right = getType(ctx.getChild(2));
		
		if (!(left instanceof Int || left instanceof Char)
				|| !(right instanceof Int || right instanceof Char)){
			addError(ctx, "Left and right operand types are not equal: "
					+ left
					+ " =/= "
					+ right);
		} else {
			setType(ctx, left);
			setAssignable(ctx, false);
		}
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
			
			if (callArgs[i] instanceof Array) {
				callArgs[i] = new Pointer(((Array) callArgs[i]).elemType);
			}
		}

		Function.Overload overload = func.getOverload(callArgs); 
		if (overload == null) {
			addError(ctx, "No appropriate overload found for function \"" + func.id + "\". Available overloads are: " + func);
		}
		
		// Set return type of function as expression type
		setType(ctx, scope.func(funcID).returnType);
		// Attach the function to the ID for the generator
		setFunction(ctx.ID(), overload);
		setAssignable(ctx, false);
	}
	
	@Override
	public void exitArrayExpr(ArrayExprContext ctx) {
		Type left = getType(ctx.expr(0));
		Type right = getType(ctx.expr(1));
		
		if (left instanceof Pointer && right instanceof Int) {
			setType(ctx, ((Pointer) left).pointsTo);
			setAssignable(ctx, true);
		} else if (left instanceof Array && right instanceof Int) {
			Type inner = ((Array) left).elemType;
			setType(ctx, inner);

			if (inner instanceof Array) {
				setAssignable(ctx, false);
			} else {
				setAssignable(ctx, true);
			}
		} else {
			addError(ctx, "Cannot do array indexing on types " + left + " and " + right);
			return;
		}
	}
	
//	@Override
//	public void exitLenExpr(LenExprContext ctx) {
//		String id = ctx.ID().getText();
//		Type type = scope.type(id);
//
//		if (!((type instanceof Type.Array) || (type instanceof Type.AnyArray))) {
//			addError(ctx, "len can only be used with arrays");
//		}
//		
//		if (!scope.contains(id)) {
//			addError(ctx,  "Variable " + id + " not defined");
//			return;
//		}
//		
//		setType(ctx, new Type.Int());
//		setType(ctx.ID(), type);
//		setOffset(ctx.ID(), scope.offset(id));
//		setReach(ctx.ID(), scope.reach(id));
//	}
	
	@Override
	public void exitDerefExpr(DerefExprContext ctx) {
		Type left = getType(ctx.expr());
		if (!(left instanceof Type.Pointer)) {
			addError(ctx, "Cannot dereference expression " + ctx.getText());
			return;
		} else {
			setType(ctx, ((Type.Pointer) left).pointsTo);
			setAssignable(ctx, true);
		}
	}
	
	@Override
	public void exitDeferExpr(lang.BurritoParser.DeferExprContext ctx) {
		String id = ctx.ID().getText();
		Type type = scope.type(id);
		
		if (!scope.contains(id)) {
			addError(ctx, "Variable not declared: " + id);
			return;
		}
		
		setType(ctx.ID(), type);
		setReach(ctx.ID(), scope.reach(id));
		setOffset(ctx.ID(), scope.offset(id));

		setType(ctx, new Type.Pointer(type));
		setAssignable(ctx, false);
	};
	
	
	
	// STATS ----------------------------
	
	@Override
	public void exitOutStat(OutStatContext ctx) {
		if (ctx.expr() == null)
			return;
		
		Type type = getType(ctx.expr());
		
		if (!(type instanceof Type.Int
				|| type instanceof Type.Bool
				|| type instanceof Type.Char
				|| type instanceof Type.Pointer)) {
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
		String id = ctx.ID().getText();
		Type type = getType(ctx.type());
		Type exprType = getType(ctx.expr());
		
		if (type instanceof Array) {
			Array arr = (Array)	type;
			if (exprType instanceof StringLiteral & arr.size == -1 ) {
				if (ctx.expr() == null) {
					addError(ctx, "Incomplete chararray type is only to be used with string literal assignment");
					return;
				}
				
				if (exprType instanceof StringLiteral) {
					StringLiteral sl = (StringLiteral) exprType;
					((Array) type).size = sl.content.length() + 1; // + 1 is for null terminator
				} else {
					addError(ctx, "Incomplete chararray type is only to be used with string literal assignment");
					return;
				}
			} else if (arr.size == -1 && (arr.elemType instanceof Int || arr.elemType instanceof Bool || arr.elemType instanceof Char)) {
				if (exprType instanceof ArrayLiteral) {
					ArrayLiteral al = (ArrayLiteral) exprType;
					if (!arr.elemType.equals(al.elemType)) {
						addError(ctx, "It is prohibited to assign a literal of " + al.elemType + " to an " + arr);
					}
					arr.size = al.arrSize;
				} else {
					addError(ctx, "Incomplete array type is only to be used with literal assignment");
				}
			} else {
				if (arr.isIncomplete()) {
					addError(ctx, "Incomplete chararray type is only to be used with string literal assignment");
					return;
				} else if (!arr.assignable()) {
					addError(ctx, "It's prohibited to assign a value to something of type " + type);
					return;
				}
			}
		} else {
			if (!type.assignable()) {
				addError(ctx, "It's prohibited to assign a value to something of type " + type);
				return;
			}

			checkType(ctx.expr(), type);
		}
		
		scope.put(id, type);
		
		setType(ctx.ID(), type);	
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
		assChecker(ctx.expr(0), ctx.expr(1), Operator.Which.Add);
	}
	
	@Override
	public void exitMinAssStat(MinAssStatContext ctx) {
		assChecker(ctx.expr(0), ctx.expr(1), Operator.Which.Sub);
	}
	
	@Override
	public void exitDivAssStat(DivAssStatContext ctx) {
		assChecker(ctx.expr(0), ctx.expr(1), Operator.Which.Div);
	}
	
	@Override
	public void exitMulAssStat(MulAssStatContext ctx) {
		assChecker(ctx.expr(0), ctx.expr(1), Operator.Which.Mul);
	}
	
	@Override
	public void exitAssStat(AssStatContext ctx) {
		assChecker(ctx.expr(0), ctx.expr(1), Operator.Which.Equal);
	}
	
	public void assChecker(ExprContext target, ExprContext param, Operator.Which modus) {
		Type left = getType(target);
		Type right = getType(param);
		
		if (left == null || right == null) {
			addError(target, "Missing inferred type of left or right hand side");
			return;
		}
		
		if ((left instanceof Char || left instanceof Int || left instanceof Bool) && (right instanceof Char || right instanceof Int || right instanceof Bool)) {
			// Fine
		} else if (left instanceof Pointer && right instanceof Int) {
			if (modus == Equal || modus == Add || modus == Sub) {}
			else {
				addError(target, "Cannot perform operator "
						+ modus.toString().replaceAll("Equal", "Assignment")
						+ " on pointer " + target.getText());
			}
		} else if (left instanceof Pointer && right instanceof Pointer) {
			// Fine
		} else if (left instanceof Pointer && right instanceof Array && modus == Equal) {
			Pointer ptr = (Pointer) left;
			Array arr = (Array) right;
			if (ptr.pointsTo.equals(arr.elemType)) {
				// Fine
			} else {
				addError(target, "Cannot perform operator Assignment on types " + left + " and " + right);
			}
		} else {
			// Not fine!
			addError((ParserRuleContext) target.parent, "Cannot perform operator "
					+ modus.toString().replaceAll("Equal", "Assignment")
					+ " on types "
					+ left
					+ " and "
					+ right);
		}
		
		if (!getAssignable(target)) {
			addError(target, "Can't assign to expression " + target.getText());
			return;
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
		// :TODO clean switch stat

		Type type = getType(ctx.expr());

		if (type != null) {
			setType(ctx, type);
			
			for (LitExprContext lit : ctx.litExpr()) {
				checkType(lit, type);
			}
		} else {
			addError(ctx, "Missing inferred type of " + ctx.expr().getText());
		}
	}
	
	// BLOCK THINGY -------------------------------
	@Override
	public void enterBlock(BlockContext ctx) {
		scope.pushScope();
	}
	
	@Override
	public void exitBlock(BlockContext ctx) {
		scope.popScope();
		
		setToPop(ctx, scope.hasToPop());
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
		if (expected == null)
			return false;
		
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
	
	public Reach getReach(ParseTree node) {
		return this.result.getReach(node);
	}
	
	public void setAssignable(ParseTree node, boolean assign) {
		result.setAssignable(node, assign);
	}
	
	public boolean getAssignable(ParseTree node) {
		return result.getAssignable(node);
	}
	
	public void setToPop(ParseTree node, int amount) {
		result.setToPop(node, amount);
	}
}
