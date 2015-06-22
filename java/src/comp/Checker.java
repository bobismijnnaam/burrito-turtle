package comp;

import java.util.ArrayList;
import java.util.List;

import lang.BurritoBaseListener;
import lang.BurritoParser.AssStatContext;
import lang.BurritoParser.BlockContext;
import lang.BurritoParser.BoolTypeContext;
import lang.BurritoParser.DivExprContext;
import lang.BurritoParser.EqExprContext;
import lang.BurritoParser.FalseExprContext;
import lang.BurritoParser.GtExprContext;
import lang.BurritoParser.GteExprContext;
import lang.BurritoParser.IdExprContext;
import lang.BurritoParser.IfStatContext;
import lang.BurritoParser.IntTypeContext;
import lang.BurritoParser.LtExprContext;
import lang.BurritoParser.LteExprContext;
import lang.BurritoParser.MinExprContext;
import lang.BurritoParser.ModExprContext;
import lang.BurritoParser.MulExprContext;
import lang.BurritoParser.NegExprContext;
import lang.BurritoParser.NumExprContext;
import lang.BurritoParser.ParExprContext;
import lang.BurritoParser.PlusExprContext;
import lang.BurritoParser.PowExprContext;
import lang.BurritoParser.TrueExprContext;
import lang.BurritoParser.TypeStatContext;
import lang.BurritoParser.WhileStatContext;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class Checker extends BurritoBaseListener {
	
	/** Result of the latest call of {@link #check}. */
	private Result result;
	/** Variable scope for the latest call of {@link #check}. */
	private Scope scope;
	/** List of errors collected in the latest call of {@link #check}. */
	private List<String> errors;
	
	public Result check(ParseTree tree) throws ParseException {
		this.scope = new Scope();
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
	
	// TYPES -------------------------
	
	@Override
	public void exitBoolType(BoolTypeContext ctx) {
		setType(ctx, Type.BOOL);
	}
	
	@Override
	public void exitIntType(IntTypeContext ctx) {
		setType(ctx, Type.INT);
	}
	
	// EXPR -----------------------------
	@Override
	public void exitFalseExpr(FalseExprContext ctx) {
		setType(ctx, Type.BOOL);
	}
	
	@Override
	public void exitTrueExpr(TrueExprContext ctx) {
		// TODO Auto-generated method stub
		setType(ctx, Type.BOOL);
	}
	
	@Override
	public void exitNumExpr(NumExprContext ctx) {
		setType(ctx, Type.INT);
	}
	
	@Override
	public void exitNegExpr(NegExprContext ctx) {
		checkType(ctx.expr(), Type.INT);
		setType(ctx, Type.INT);
	}
	
	@Override
	public void exitIdExpr(IdExprContext ctx) {
		String id = ctx.getText();
		Type type = this.scope.type(id);
		if (type == null) {
			addError(ctx, "Variable %s not declared", id);
		} else {
			setType(ctx, type);
			setOffset(ctx, this.scope.offset(id));
		}
	}
	
	@Override
	public void exitParExpr(ParExprContext ctx) {
		setType(ctx, getType(ctx.expr()));
	}
	
	// TODO , By compare check if it is a type that can be compared
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
	
	void checkTypeCompare(ParserRuleContext ctx) {
		Type type1 = getType(ctx.getChild(0));
		Type type2 = getType(ctx.getChild(2));
		if (type1 != type2) {
			addError(ctx, "Can't compare %s == %s types!", type1, type2);
		} else {
			setType(ctx, Type.BOOL);
		}
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
		Type type1 = getType(ctx.getChild(0));
		Type type2 = getType(ctx.getChild(2));
		String op = ctx.getChild(1).getText();
		if (type1 != Type.INT || type2 != Type.INT) {
			addError(ctx, "Can't %s %s %s!", type1, op, type2);
		} else {
			setType(ctx, Type.INT);
		}
	}
	
	// STATS ----------------------------
	@Override
	public void exitTypeStat(TypeStatContext ctx) {
		String id = ctx.target().getText();
		Type type = result.getType(ctx.type());
		
		scope.put(id, type);
		
		setType(ctx.target(), type);	
		checkType(ctx.expr(), type);
		setOffset(ctx.target(), scope.offset(id));
	}
	
	@Override
	public void exitWhileStat(WhileStatContext ctx) {
		checkType(ctx.expr(), Type.BOOL);
	}
	
	@Override
	public void exitIfStat(IfStatContext ctx) {
		checkType(ctx.expr(), Type.BOOL);
	}
	
	@Override
	public void exitAssStat(AssStatContext ctx) {
		String id = ctx.target().getText();
		Type type = this.scope.type(id);
		setOffset(ctx.target(), scope.offset(id));
		if (type != getType(ctx.expr())) {
			addError(ctx, "Can't assign %s = %s", type, getType(ctx.expr()));
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
	private void checkType(ParserRuleContext node, Type expected) {
		Type actual = getType(node);
		if (actual == null) {
			throw new IllegalArgumentException("Missing inferred type of "
					+ node.getText());
		}
		if (!actual.equals(expected)) {
			addError(node, "Expected type '%s' but found '%s'", expected,
					actual);
		}
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

	/** Convenience method to add a flow graph entry to the result. */
	private void setEntry(ParseTree node, ParserRuleContext entry) {
		if (entry == null) {
			throw new IllegalArgumentException("Null flow graph entry");
		}
		this.result.setEntry(node, entry);
	}

	/** Returns the flow graph entry of a given expression or statement. */
	private ParserRuleContext entry(ParseTree node) {
		return this.result.getEntry(node);
	}
}
