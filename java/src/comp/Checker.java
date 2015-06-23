package comp;

import java.util.ArrayList;
import java.util.List;

import lang.BurritoBaseListener;
import lang.BurritoParser.ArrayTargetContext;
import lang.BurritoParser.ArrayTypeContext;
import lang.BurritoParser.AssStatContext;
import lang.BurritoParser.BlockContext;
import lang.BurritoParser.BoolTypeContext;
import lang.BurritoParser.DivExprContext;
import lang.BurritoParser.EqExprContext;
import lang.BurritoParser.FalseExprContext;
import lang.BurritoParser.GtExprContext;
import lang.BurritoParser.GteExprContext;
import lang.BurritoParser.IdExprContext;
import lang.BurritoParser.IdTargetContext;
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
import lang.BurritoParser.TargetContext;
import lang.BurritoParser.TrueExprContext;
import lang.BurritoParser.TypeAssignStatContext;
import lang.BurritoParser.TypeStatContext;
import lang.BurritoParser.WhileStatContext;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import comp.Type.Array;

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
		setType(ctx, new Type.Bool());
	}
	
	@Override
	public void exitIntType(IntTypeContext ctx) {
		setType(ctx, new Type.Int());
	}
	
	@Override
	public void exitArrayType(ArrayTypeContext ctx) {
		setType(ctx, new Type.Array(getType(ctx.type()), new Integer(ctx.NUM().getText())));
	}
	
	@Override
	public void exitIdTarget(IdTargetContext ctx) {
		setType(ctx, scope.type(ctx.ID().getText()));
	}
	
	@Override
	public void exitArrayTarget(ArrayTargetContext ctx) {
		setType(ctx, scope.type(ctx.ID().getText()));
		checkType(ctx.expr(), new Type.Int());
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
	
	// STATS ----------------------------
	@Override
	public void exitTypeAssignStat(TypeAssignStatContext ctx) {
		String id = ctx.ID().getText();
		Type type = result.getType(ctx.type());
		
		scope.put(id, type);
		
		setType(ctx.ID(), type);	
		checkType(ctx.expr(), type);
		setOffset(ctx.ID(), scope.offset(id));
	}
	
	@Override
	public void exitTypeStat(TypeStatContext ctx) {
		String id = ctx.ID().getText();
		Type type = result.getType(ctx.type());
		
		if (ctx.getChildCount() > 0) {
			scope.put(id, type, new Integer(ctx.type().getChild(2).getText()));
		} else {
			scope.put(id, type);
		}
		
		setType(ctx.ID(), type);		
		setOffset(ctx.ID(), scope.offset(id));
	}
	
	@Override
	public void exitWhileStat(WhileStatContext ctx) {
		checkType(ctx.expr(), new Type.Bool());
	}
	
	@Override
	public void exitIfStat(IfStatContext ctx) {
		checkType(ctx.expr(), new Type.Bool());
	}
	
	@Override
	public void exitAssStat(AssStatContext ctx) {
		String id = ctx.target().getText();
		
		Type type = this.scope.type(id);
		if (id.contains("[")) {
			id = id.split("\\[")[0];
			Type.Array array = (Array) this.scope.type(id);
			if (array != null)
				type = array.elemType;
		}
		
		if (type != null) {
			if (checkType(ctx.expr(), type)) {
				setOffset(ctx.target(), scope.offset(id));
			}
		} else {
			addError(ctx.target(), "Missing inferred type of " + ctx.target().getText());
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
}
