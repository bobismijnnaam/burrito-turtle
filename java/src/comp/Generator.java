package comp;

import static sprockell.Operator.Which.*;
import static sprockell.Program.*;
import static sprockell.Reg.Which.*;
import static sprockell.Sprockell.Op.*;

import java.util.List;

import lang.BurritoBaseVisitor;
import lang.BurritoParser.AssStatContext;
import lang.BurritoParser.BlockContext;
import lang.BurritoParser.DivExprContext;
import lang.BurritoParser.EqExprContext;
import lang.BurritoParser.FalseExprContext;
import lang.BurritoParser.GtExprContext;
import lang.BurritoParser.GteExprContext;
import lang.BurritoParser.IdExprContext;
import lang.BurritoParser.IfStatContext;
import lang.BurritoParser.LtExprContext;
import lang.BurritoParser.LteExprContext;
import lang.BurritoParser.MinExprContext;
import lang.BurritoParser.ModExprContext;
import lang.BurritoParser.MulExprContext;
import lang.BurritoParser.NotExprContext;
import lang.BurritoParser.NumExprContext;
import lang.BurritoParser.ParExprContext;
import lang.BurritoParser.PlusExprContext;
import lang.BurritoParser.PowExprContext;
import lang.BurritoParser.ProgramContext;
import lang.BurritoParser.StatContext;
import lang.BurritoParser.TrueExprContext;
import lang.BurritoParser.TypeStatContext;
import lang.BurritoParser.WhileStatContext;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import sprockell.Instr;
import sprockell.MemAddr;
import sprockell.Operator;
import sprockell.Program;
import sprockell.Reg;
import sprockell.Target;
import sprockell.Value;

public class Generator extends BurritoBaseVisitor<List<Instr>> {
	private Program prog;
	private Result checkResult;
	private ParseTreeProperty<Reg> regs;
	private ParseTreeProperty<String> labels;

	public Program generate(ParseTree tree, Result checkResult) {
		this.prog = new Program();
		this.checkResult = checkResult;
		this.regs = new ParseTreeProperty<>();
		this.labels = new ParseTreeProperty<>();
		tree.accept(this);
		return this.prog;
	}

	@Override
	public List<Instr> visitProgram(ProgramContext ctx) {
		for (StatContext asc : ctx.stat()) {
			visit(asc);
		}
		
		prog.emit(EndProg);
		
		return null;
	}
	
	@Override
	public List<Instr> visitTypeStat(TypeStatContext ctx) {
		visit(ctx.expr());
		// TODO: Replace with storeAI (store reg => reg, offset)
		prog.emit(Const, new Value(checkResult.getOffset(ctx.target())), new Reg(RegB));
		prog.emit(Compute, new Operator(Add), new Reg(RegA), new Reg(RegB), new Reg(RegB));
		prog.emit(Store, new Reg(RegE), new MemAddr(RegB));
		
		return null;
	}
	
	@Override
	public List<Instr> visitAssStat(AssStatContext ctx) {
		visit(ctx.expr());
		// TODO: Replace with storeAI (store reg => reg, offset)
		prog.emit(Const, new Value(checkResult.getOffset(ctx.target())), new Reg(RegB));
		prog.emit(Compute, new Operator(Add), new Reg(RegA), new Reg(RegB), new Reg(RegB));
		prog.emit(Store, new Reg(RegE), new MemAddr(RegB));
		
		return null;
	}
	
	@Override
	public List<Instr> visitIfStat(IfStatContext ctx) {
		visit(ctx.expr());
		String trueLabel = mkLbl(ctx, "true");
		String endLabel = mkLbl(ctx, "end");
		
		prog.emit(Branch, new Reg(RegE), new Target(trueLabel));
		
		if (ctx.ELSE() != null) {
			visit(ctx.block(1));
		}
		prog.emit(Jump, new Target(endLabel));
		
		// True label
		prog.emit(trueLabel, Nop);
		visit(ctx.block(0));
		
		// End label
		prog.emit(endLabel, Nop);
		
		return null;
	}
	
	@Override
	public List<Instr> visitWhileStat(WhileStatContext ctx) {
		String beginLabel = mkLbl(ctx, "begin");
		String bodyLabel = mkLbl(ctx, "body");
		String endLabel = mkLbl(ctx, "end");
		
		prog.emit(beginLabel, Nop);
		visit(ctx.expr());
		prog.emit(Branch, new Reg(RegE), new Target(bodyLabel));
		prog.emit(Jump, new Target(endLabel));
		prog.emit(bodyLabel, Nop);
		visit(ctx.block());
		prog.emit(Jump, new Target(beginLabel));
		prog.emit(endLabel, Nop);
		
		return null;
	}
	
	@Override
	public List<Instr> visitBlock(BlockContext ctx) {
		for (StatContext asc : ctx.stat()) {
			visit(asc);
		}
		
		return null;
	}
	
	@Override
	public List<Instr> visitNotExpr(NotExprContext ctx) {
		visit(ctx.expr());
		prog.emit(Compute, new Operator(Equal), new Reg(RegE), new Reg(Zero), new Reg(RegE));
		return null;
	}
	
	private List<Instr> emitArOp(ParserRuleContext ctx, Operator.Which op) {
		visit(ctx.getChild(0));
		prog.emit(Push, new Reg(RegE));
		prog.emit(Pop, new Reg(RegD));
		visit(ctx.getChild(2));
		prog.emit(Compute, new Operator(op), new Reg(RegD), new Reg(RegE), new Reg(RegE));
		
		return null;
	}
	
	@Override
	public List<Instr> visitDivExpr(DivExprContext ctx) {
		return emitArOp(ctx, Div);
	}
	
	@Override
	public List<Instr> visitMulExpr(MulExprContext ctx) {
		return emitArOp(ctx, Mul);
	}
	
	@Override
	public List<Instr> visitPowExpr(PowExprContext ctx) {
		System.out.println("Todo: implement pow");
		return null;
	}
	
	@Override
	public List<Instr> visitModExpr(ModExprContext ctx) {
		return emitArOp(ctx, Mod);
	}
	
	@Override
	public List<Instr> visitPlusExpr(PlusExprContext ctx) {
		return emitArOp(ctx, Add);
	}
	
	@Override
	public List<Instr> visitMinExpr(MinExprContext ctx) {
		return emitArOp(ctx, Sub);
	}
	
	@Override
	public List<Instr> visitEqExpr(EqExprContext ctx) {
		return emitArOp(ctx, Equal);
	}
	
	@Override
	public List<Instr> visitLtExpr(LtExprContext ctx) {
		return emitArOp(ctx, Lt);
	}
	
	@Override
	public List<Instr> visitGtExpr(GtExprContext ctx) {
		return emitArOp(ctx, Gt);
	}
	
	@Override
	public List<Instr> visitLteExpr(LteExprContext ctx) {
		return emitArOp(ctx, LtE);
	}
	
	@Override
	public List<Instr> visitGteExpr(GteExprContext ctx) {
		return emitArOp(ctx, GtE);
	}
	
	@Override
	public List<Instr> visitParExpr(ParExprContext ctx) {
		return visit(ctx.expr());
	}
	
	@Override
	public List<Instr> visitIdExpr(IdExprContext ctx) {
		prog.emit(Const, new Value(checkResult.getOffset(ctx)), new Reg(RegB));
		prog.emit(Compute, new Operator(Add), new Reg(RegA), new Reg(RegB), new Reg(RegB));
		prog.emit(Load, new MemAddr(RegB), new Reg(RegE));
		
		return null;
	}
	
	@Override
	public List<Instr> visitNumExpr(NumExprContext ctx) {
		prog.emit(Const, new Value(new Integer(ctx.NUM().getText())), new Reg(RegE));
		return null;
	}
	
	@Override
	public List<Instr> visitTrueExpr(TrueExprContext ctx) {
		prog.emit(Const, new Value(1), new Reg(RegE));
		return null;
	}
	
	@Override
	public List<Instr> visitFalseExpr(FalseExprContext ctx) {
		prog.emit(Const, new Value(0), new Reg(RegE));
		return null;
	}
	

}
