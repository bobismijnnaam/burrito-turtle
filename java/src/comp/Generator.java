package comp;

import static sprockell.Operator.Which.*;

import static sprockell.Program.*;
import static sprockell.Reg.Which.*;
import static sprockell.Sprockell.Op.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import lang.BurritoBaseVisitor;
import lang.BurritoParser.AndExprContext;
import lang.BurritoParser.ArrayExprContext;
import lang.BurritoParser.ArrayTargetContext;
import lang.BurritoParser.AssStatContext;
import lang.BurritoParser.BlockContext;
import lang.BurritoParser.CharacterExprContext;
import lang.BurritoParser.DecExprContext;
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
import lang.BurritoParser.PlusAssStatContext;
import lang.BurritoParser.PlusExprContext;
import lang.BurritoParser.PowExprContext;
import lang.BurritoParser.ProgramContext;
import lang.BurritoParser.ReturnStatContext;
import lang.BurritoParser.StatContext;
import lang.BurritoParser.TrueExprContext;
import lang.BurritoParser.TypeAssignStatContext;
import lang.BurritoParser.TypeStatContext;
import lang.BurritoParser.WhileStatContext;
import lang.BurritoParser.XorExprContext;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import sprockell.Instr;
import sprockell.MemAddr;
import sprockell.Operator;
import sprockell.Program;
import sprockell.Reg;
import sprockell.Target;
import sprockell.Value;
import comp.Type.Array;

import static comp.Reach.*;


public class Generator extends BurritoBaseVisitor<List<Instr>> {
	public final String MAINMETHOD = "program";
	
	private Program prog;
	private Result checkResult;
	private Stack<Program> progStack = new Stack<>();
	private Map<String,Program> progMap = new HashMap<>();
	
	private String mainLabel;
	private String endLabel;
	
	private String printBoolLabel;
	private String printIntLabel;
	private String printCharLabel;
	
	/**
	 * Puts a new Program instance into prog, and pushes the old one on the stack.
	 * At the end the visitor will stitch them all together.
	 * @param funcName The function you're entering so we can save it in a map
	 */
	private void enterFunc(String funcName) {
		progStack.push(prog);
		prog = new Program();
		progMap.put(funcName, prog);
	}
	
	private void leaveFunc() {
		prog = progStack.pop();
	}

	public Program generate(ParseTree tree, Result checkResult) {
		this.prog = new Program();
		this.checkResult = checkResult;
		tree.accept(this);
		return this.prog;
	}

	// TODO: Initialize static variables
	@Override
	public List<Instr> visitProgram(ProgramContext ctx) {
		endLabel = Program.mkLbl(ctx, "done");
		Reg zero = new Reg(Zero);
		
		// Generate a program for each function
		for (FuncContext asc : ctx.func()) {
			visit(asc);
		}
		
		// Start making the final program here
		// First construct the "fake" stack for program()
		prog.emit(Const, new Value(endLabel), new Reg(RegE));
		prog.emit(Push, new Reg(RegE)); // Return address
		// No parameters, so nothing to push here. Maybe a TODO?
		// We push a zero here to let the ARP point to the FIRST byte after the last parameter
		// (or the return value of there were no parameters)
		prog.emit(Push, zero);
		prog.emit(Compute, new Operator(Add), new Reg(SP), zero, new Reg(RegA)); // Set the initial ARP
		prog.emit(Jump, new Target(mainLabel));
		
		for (Entry<String, Program> entry : progMap.entrySet()) {
			prog.mergeWithFunction(entry.getValue());
		}
		
		prog.emit(endLabel, Nop);
		prog.emit(Nop); // Extra nops to flush stdio
		prog.emit(Nop);
		prog.emit(Nop);
		prog.emit(Nop);
		prog.emit(Nop);
		prog.emit(EndProg);
		
		return null;
	}
	
	@Override
	public List<Instr> visitFunc(FuncContext ctx) {
		Function.Overload func = checkResult.getFunction(ctx); 

		enterFunc(func.func.id + func.label);
		
		if (func.func.id.equals(MAINMETHOD) && func.args.length == 0) {
			mainLabel = func.label;
		}

		prog.emit(func.label, Nop);
		
		for (StatContext stat : ctx.stat()) {
			visit(stat);
		}
		
		leaveFunc();
		
		return null;
	}
	
	@Override
	public List<Instr> visitPlusAssStat(PlusAssStatContext ctx) {
		return emitOpAss(ctx, Add);
	}
	
	@Override
	public List<Instr> visitMinAssStat(MinAssStatContext ctx) {
		return emitOpAss(ctx, Sub);
	}
	
	@Override
	public List<Instr> visitDivAssStat(DivAssStatContext ctx) {
		return emitOpAss(ctx, Div);
	}
	
	@Override
	public List<Instr> visitMulAssStat(MulAssStatContext ctx) {
		return emitOpAss(ctx, Mul);
	}
	
	private List<Instr> emitOpAss(ParserRuleContext ctx, Operator.Which op) {
		visit(ctx.getChild(0));
		prog.emit(Push, new Reg(RegE));
		visit(ctx.getChild(3));
		prog.emit(Pop, new Reg(RegD));

		// an offset now resides in RegD
		Reach reach = checkResult.getReach(ctx);
		if (reach == Local) {
			prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegD), new Reg(RegB));
			// mem addr => RegB get target value
			prog.emit(Load, new MemAddr(RegB), new Reg(RegC));
			prog.emit(Compute, new Operator(op), new Reg(RegC), new Reg(RegE), new Reg(RegE));
			prog.emit(Store, new Reg(RegE), new MemAddr(RegB));
		} else if (reach ==  Global) {
			prog.emit(Read, new MemAddr(RegD));
			prog.emit(Receive, new Reg(RegB));
			prog.emit(Compute, new Operator(op), new Reg(RegB), new Reg(RegE), new Reg(RegE));
			prog.emit(Write, new Reg(RegE), new MemAddr(RegD));
		}
		
		return null;
	}
	
	// TODO: Replace with storeAI (store reg => reg, offset)
	@Override
	public List<Instr> visitTypeAssignStat(TypeAssignStatContext ctx) {
		visit(ctx.expr());
		
		Reach reach = checkResult.getReach(ctx.ID());
		
		if (reach == Global) {
			System.out.println("[Generator] THIS IS WRONG");
		} else if (reach == Local) {
			Type type = checkResult.getType(ctx.ID());
			for (int i = 0; i < type.size(); i++) {
				prog.emit(Push, new Reg(Zero));
			}
			prog.emit(Const, new Value(checkResult.getOffset(ctx.ID())), new Reg(RegB));
			prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegB), new Reg(RegB));
			prog.emit(Store, new Reg(RegE), new MemAddr(RegB));
		}
		
		return null;
	}
	
	// TODO: Replace with storeAI (store reg => reg, offset)
	@Override
	public List<Instr> visitAssStat(AssStatContext ctx) {
		visit(ctx.target());
		prog.emit(Push, new Reg(RegE));
		visit(ctx.expr());
		prog.emit(Pop, new Reg(RegD));
		// D = offset, E = Waarde
		
		Reach reach = checkResult.getReach(ctx.target());
		if (reach == Local) {
			// Waarde mem[arp + offset]
			prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegD), new Reg(RegB));
			prog.emit(Store, new Reg(RegE), new MemAddr(RegB));
		} else if (reach == Global) {
			prog.emit(Write, new Reg(RegE), new MemAddr(RegD));
		} else {
			System.out.println(ctx.target().getText() + " has no reach set!");
		}
		
		return null;
	}
	
	@Override
	public List<Instr> visitTypeStat(TypeStatContext ctx) {
		visit(ctx.type());
		
		if (checkResult.getReach(ctx.ID()) != Local) System.out.println("[Generator] This can;t be global, has to be local");

		Type type = checkResult.getType(ctx.ID());
		for (int i = 0; i < type.size(); i++) {
			prog.emit(Push, new Reg(Zero));
		}
		
		return null;
	}
	
	// TODO: Do reach magic here
	@Override
	public List<Instr> visitArrayTarget(ArrayTargetContext ctx) {
		for (ExprContext expr : ctx.expr()) {
			visit(expr);
			prog.emit(Push, new Reg(RegE));
		}
		
		Array array = (Array) checkResult.getType(ctx.ID());
		prog.emit(Const, new Value(0), new Reg(RegC));
		
		int size = 1;
		for (int i = array.indexSize.size() - 1; i >= 0; i--) {
			prog.emit(Pop, new Reg(RegE));
			prog.emit(Const, new Value(size), new Reg(RegD));
			prog.emit(Compute, new Operator(Mul), new Reg(RegE), new Reg(RegD), new Reg(RegE));
			prog.emit(Compute, new Operator(Add), new Reg(RegE), new Reg(RegC), new Reg(RegC));
			size *= array.indexSize.get(i);
		}
		
		// In RegC staat nu offset relatief in array
		
		// Get offset from ctx.ID() in RegB
		prog.emit(Const, new Value(checkResult.getOffset(ctx.ID())), new Reg(RegB));
		// RegB + RegE -> RegE
		
		prog.emit(Compute, new Operator(Add), new Reg(RegB), new Reg(RegC), new Reg(RegE));
		return null;
	}
	//prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegE), new Reg(RegE));
	
	@Override
	public List<Instr> visitIdTarget(IdTargetContext ctx) {
		// Get offset from ctx.ID() in RegE
		prog.emit(Const, new Value(checkResult.getOffset(ctx)), new Reg(RegE));
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
		visit(ctx.getChild(2));
		prog.emit(Pop, new Reg(RegD));
		prog.emit(Compute, new Operator(op), new Reg(RegD), new Reg(RegE), new Reg(RegE));
		
		return null;
	}
	
	// TODO: Something with stack length here
	// The idea is that we switch from the premise of "after expr RegE will contain the result"
	// to "after expr the stack will point to the integer after the last integer of the result"
	// The type (and thus the size) is known at compile time so this should be possible
	// Also once you do that using objects becomes far more likely
	// And pass-by-value arrays ^^
	@Override
	public List<Instr> visitArrayExpr(ArrayExprContext ctx) {
		for (ExprContext expr : ctx.expr()) {
			visit(expr);
			prog.emit(Push, new Reg(RegE));
		}
		
		Array array = (Array) checkResult.getType(ctx.ID());
		prog.emit(Const, new Value(0), new Reg(RegC));
		
		int size = 1;
		for (int i = array.indexSize.size() - 1; i >= 0; i--) {
			prog.emit(Pop, new Reg(RegE));
			prog.emit(Const, new Value(size), new Reg(RegD));
			prog.emit(Compute, new Operator(Mul), new Reg(RegE), new Reg(RegD), new Reg(RegE));
			prog.emit(Compute, new Operator(Add), new Reg(RegE), new Reg(RegC), new Reg(RegC));
			size *= array.indexSize.get(i);
		}
		
		// Waarde mem[arp - offset]
		prog.emit(Const, new Value(checkResult.getOffset(ctx.ID())), new Reg(RegB));
		prog.emit(Compute, new Operator(Add), new Reg(RegB), new Reg(RegC), new Reg(RegC));
		
		Reach reach = checkResult.getReach(ctx.ID());
		if (reach == Local) {
			prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegC), new Reg(RegB));
			prog.emit(Load, new MemAddr(RegB), new Reg(RegE));
		} else if (reach == Global) {
			prog.emit(Read, new MemAddr(RegC));
			prog.emit(Receive, new Reg(RegE));
		}

		return null;
	}
	
	@Override
	public List<Instr> visitIncExpr(IncExprContext ctx) {
		visit(ctx.target());
		
		Reach reach = checkResult.getReach(ctx);
		
		if (reach == Local) {
			prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegE), new Reg(RegB));
			prog.emit(Load, new MemAddr(RegB), new Reg(RegC));
		} else if (reach == Global) {
			prog.emit(Read, new MemAddr(RegE));
			prog.emit(Receive, new Reg(RegC));
		}

		prog.emit(Const, new Value(ctx.PLUS().size()), new Reg(RegD));
		prog.emit(Compute, new Operator(Add), new Reg(RegC), new Reg(RegD), new Reg(RegD));

		if (reach == Local) {
			prog.emit(Store, new Reg(RegD), new MemAddr(RegB));
		} else if (reach == Global) {
			prog.emit(Write, new Reg(RegD), new MemAddr(RegE));
		}

		return null;
	}
	
	@Override
	public List<Instr> visitDecExpr(DecExprContext ctx) {
		visit(ctx.target());
		
		Reach reach = checkResult.getReach(ctx);
		
		if (reach == Local) {
			prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegE), new Reg(RegB));
			prog.emit(Load, new MemAddr(RegB), new Reg(RegC));
		} else if (reach == Global) {
			prog.emit(Read, new MemAddr(RegE));
			prog.emit(Receive, new Reg(RegC));
		}

		prog.emit(Const, new Value(ctx.MIN().size()), new Reg(RegD));
		prog.emit(Compute, new Operator(Sub), new Reg(RegC), new Reg(RegD), new Reg(RegD));

		if (reach == Local) {
			prog.emit(Store, new Reg(RegD), new MemAddr(RegB));
		} else if (reach == Global) {
			prog.emit(Write, new Reg(RegD), new MemAddr(RegE));
		}

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
	public List<Instr> visitAndExpr(AndExprContext ctx) {
		return emitArOp(ctx, And);
	}
	
	@Override
	public List<Instr> visitOrExpr(OrExprContext ctx) {
		return emitArOp(ctx, Or);
	}
	
	@Override
	public List<Instr> visitXorExpr(XorExprContext ctx) {
		return emitArOp(ctx, Xor);
	}
	
	@Override
	public List<Instr> visitParExpr(ParExprContext ctx) {
		return visit(ctx.expr());
	}
	
	@Override
	public List<Instr> visitIdExpr(IdExprContext ctx) {
//		System.out.println("Visiting " + ctx.getText());
//		if (checkResult.getReach(ctx) == Reach.Global) {
//			System.out.println(ctx.getText() + " is " + checkResult.getReach(ctx).toString());
//		}
//		if (checkResult.getReach(ctx) == Reach.Local) {
//			System.out.println(ctx.getText() + " is " + checkResult.getReach(ctx).toString());
//		}
		
		prog.emit(Const, new Value(checkResult.getOffset(ctx)), new Reg(RegB));
		
		Reach reach = checkResult.getReach(ctx);
		if (reach == Local) {
			prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegB), new Reg(RegB));
			prog.emit(Load, new MemAddr(RegB), new Reg(RegE));
		} else if (reach == Global) {
			prog.emit(Read, new MemAddr(RegB));
			prog.emit(Receive, new Reg(RegE));
		}
		
		return null;
	}
	
	@Override
	public List<Instr> visitNumExpr(NumExprContext ctx) {
		prog.emit(Const, new Value(new Integer(ctx.NUM().getText())), new Reg(RegE));
		return null;
	}
	
	@Override
	public List<Instr> visitCharacterExpr(CharacterExprContext ctx) {
		String c = ctx.CHARACTER().getText();
		char ch = c.charAt(1);
		int i = (int) ch;
		prog.emit(Const, new Value(i), new Reg(RegE));
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
	
	@Override
	public List<Instr> visitNegExpr(NegExprContext ctx) {
		visit(ctx.expr());
		prog.emit(Compute, new Operator(Sub), new Reg(Zero), new Reg(RegE), new Reg(RegE));
		return null;
	}
	
	@Override
	public List<Instr> visitFuncExpr(FuncExprContext ctx) {
		Function.Overload func = checkResult.getFunction(ctx.ID());
		
		Reg zero = new Reg(Zero);
		String returnLabel = Program.mkLbl(ctx, "return");
		
		// Push return value space on top of stack
		prog.emit(Push, zero);
		
		// Save registers
		prog.emit(Push, new Reg(RegA));
		prog.emit(Push, new Reg(RegB));
		prog.emit(Push, new Reg(RegC));
		prog.emit(Push, new Reg(RegD));
		prog.emit(Push, new Reg(RegE));
		
		// Push return addres on the stack
		prog.emit(Const, new Value(returnLabel), new Reg(RegE));
		prog.emit(Push, new Reg(RegE));
		
		// TODO: Make this size aware (e.g. with arrays, push more than one if needed.
		// Don't forget to also support this in the checker!
		for (ExprContext expr : ctx.expr()) {
			visit(expr);
			prog.emit(Push, new Reg(RegE));
		}
		
		// Push zero so the SP points to an EMPTY SLOT!
		// This is useful because now the ARP actually points to the place 
		// AFTER the last parameter (or the return value if there were no parameters)
		prog.emit(Push, zero);
		
		// Set new ARP
		prog.emit(Compute, new Operator(Add), new Reg(SP), new Reg(Zero), new Reg(RegA));
		
		// We push the extra zero because now the ARP points to the first EMPTY slot on the stack!
		// Before it was pointing to the last parameter integer
		// (The stack starts with SP at 128, fyi)
		// Now, if you add 0 to the arp (as you do with the offset of the first variable), you get the first byte AFTER the arp. Subtract one,
		// and you end up at the return address. Enz enz.
		// This also means that an extra pop will be needed when arriving back though.
		
		// Jump to new function
		prog.emit(Jump, new Target(func.label));
		
		// Control flow returns here
		// Restore registers
		prog.emit(returnLabel, Pop, new Reg(RegE));
		prog.emit(Pop, new Reg(RegD));
		prog.emit(Pop, new Reg(RegC));
		prog.emit(Pop, new Reg(RegB));
		prog.emit(Pop, new Reg(RegA));
		
		// Put return value into register E, ofcourse
		prog.emit(Pop, new Reg(RegE));
//		prog.emit(Const, new Value(99), new Reg(RegE));
		
		return null;
	}
	
	// TODO: Make this size-aware as well
	@Override
	public List<Instr> visitReturnStat(ReturnStatContext ctx) {
		if (ctx.expr() != null) {
			visit(ctx.expr()); 
		}
		
		Function.Overload func = checkResult.getFunction(ctx);

		// Pop all current stack variables off the stack
		int currStackSize = checkResult.getStackSize(ctx); 
		
		// Unwind the stack
		if (currStackSize > 0) {
			prog.emit(Const, new Value(currStackSize), new Reg(RegD));
			prog.emit(Compute, new Operator(Add), new Reg(SP), new Reg(RegD), new Reg(SP));
		} 
		
		// ARP is now at the integer AFTER the last parameter, so we 
		// pop one more to let the SP point to the last parameter
		// (or if there were no parameters, the return value)
		prog.emit(Pop, new Reg(Zero));
		
		// Unpop all the parameters
		int total = 0;
		for (Type arg : func.args) {
			for (int i = 0; i < arg.size(); i++) {
				total += 1; 
			}
		}
		if (total > 0) {
			prog.emit(Const, new Value(total), new Reg(RegD));
			prog.emit(Compute, new Operator(Add), new Reg(SP), new Reg(RegD), new Reg(SP));
		}
		
		// Store the return value in the return value field if it's not main
		// TODO: Do something with return value in case of main
		if (ctx.expr() != null && func.func.id != MAINMETHOD) {
			prog.emit(Const, new Value(6), new Reg(RegD)); // To skip over the return address field and the 5 registers
			prog.emit(Compute,  new Operator(Add), new Reg(SP), new Reg(RegD), new Reg(RegD));
			prog.emit(Store, new Reg(RegE), new MemAddr(RegD));
		}
		
		// BEAM ME UP SCOTTY
		prog.emit(Pop, new Reg(RegE));
		prog.emit(Jump, new Target(RegE));
		
		return null;
	}
	
	@Override
	public List<Instr> visitOutStat(OutStatContext ctx) {
		if (ctx.expr() != null) {
			visit(ctx.expr());
		
			// Different routine depending on type
			if (checkResult.getType(ctx.expr()).equals(new Type.Bool())) {
				if (printBoolLabel == null) {
					printBoolLabel = "pipeOp_bool";
					
					enterFunc(printBoolLabel);
					
					Reg workReg = new Reg(RegD);
					String equals = Program.mkLbl(ctx, "equals");
					String end = Program.mkLbl(ctx, "end");
					
					prog.emit(printBoolLabel, Compute,  new Operator(Equal), new Reg(Zero), new Reg(RegE), workReg);
					prog.emit(Branch, workReg, new Target(equals));
					
					for (char c : "true".toCharArray()) {
						prog.emit(Const, new Value(c), workReg);
						prog.emit(Write, workReg, new MemAddr("stdio"));
					}
					prog.emit(Jump, new Target(end));
					
					prog.emit(equals, Nop);
					
					for (char c : "false".toCharArray()) {
						prog.emit(Const, new Value(c), workReg);
						prog.emit(Write, workReg, new MemAddr("stdio"));
					}
					
					prog.emit(end, Pop, new Reg(RegE));
					prog.emit(Jump, new Target(RegE));
					
					leaveFunc();
				}
				
				String returnLabel = Program.mkLbl(ctx, "boolreturn");

				prog.emit(Const, new Value(returnLabel), new Reg(RegD));
				prog.emit(Push, new Reg(RegD));
				prog.emit(Jump, new Target(printBoolLabel));
				prog.emit(returnLabel, Nop);
			} else if (checkResult.getType(ctx.expr()).equals(new Type.Int())) {
				if (printIntLabel == null) {
					printIntLabel = "pipeOp_int";
					
					enterFunc(printIntLabel);
					
					Operator mod = new Operator(Mod);
					Operator div = new Operator(Div);
					Operator mul = new Operator(Mul);
					Operator add = new Operator(Add);
					
					Reg numReg = new Reg(RegE);
					Reg countReg = new Reg(RegB);
					Reg orderReg = new Reg(RegD);
					Reg workReg = new Reg(RegC);
					
					String begin = Program.mkLbl(ctx, "begin");
					String unroll = Program.mkLbl(ctx, "unroll");
					String beginUnroll = Program.mkLbl(ctx, "beginUnroll");
					String done = Program.mkLbl(ctx, "done"); 
					String again = Program.mkLbl(ctx, "again"); 
					
					// Starting values
					prog.emit(printIntLabel, Const, new Value(10), orderReg);
					prog.emit(Const, new Value(0), countReg);
					
					// TODO: When ComputeI is actually used, replace stack usage here
					
					// Print minus if it's negative
					prog.emit(Compute, new Operator(LtE), new Reg(Zero), numReg, workReg);
					prog.emit(Branch, workReg, new Target(begin));
					prog.emit(Const, new Value("-".charAt(0)), workReg);
					prog.emit(Write, workReg, new MemAddr("stdio"));
					prog.emit(Compute, new Operator(Sub), new Reg(Zero), numReg, numReg);
					
					// Take modulo of current value and save it on the stack
					prog.emit(begin, Compute, mod, numReg, orderReg, workReg);
					prog.emit(Push, workReg);
					
					// Divide current modulo by 10 to extract the current digit
					// 12345 mod 10 => 5, 10 / 10 => 1, 5 div 1 = 5 (the digit)
					// 12345 mod 100 => 45, 100 / 10 => 10, 45 div 10 => 4 (the digit)
					prog.emit(Const, new Value(10), workReg);
					prog.emit(Compute, div, orderReg, workReg, orderReg);
					
					// Retrieve the modulo'd current number
					prog.emit(Pop, workReg);
					
					// Retrieve the current digit and save it on the stack for printing
					prog.emit(Compute, div, workReg, orderReg, workReg);
					prog.emit(Push, workReg);
					
					// Restore the modulo and increment the digit count 
					prog.emit(Const, new Value(10), workReg);
					prog.emit(Compute, mul, workReg, orderReg, orderReg);
					prog.emit(Const, new Value(1), workReg);
					prog.emit(Compute, add, workReg, countReg, countReg);
					
					// If current order is more than the number (or order == number, i.e. order
					// == 10 and number == 10), we're finished
					// Otherwise multiply the current order by 10 to go to the next digit
					// And then jump to the beginning
					prog.emit(Compute, new Operator(Equal), orderReg, numReg, workReg);
					prog.emit(Branch, workReg, new Target(again));
					prog.emit(Compute, new Operator(GtE), orderReg, numReg, workReg);
					prog.emit(Branch, workReg, new Target(unroll));
					prog.emit(again, Const, new Value(10), workReg);
					prog.emit(Compute, mul, workReg, orderReg, orderReg);
					prog.emit(Jump, new Target(begin));
					
					// Initialize registers for printing
					Reg asciiReg = orderReg;
					Reg oneReg = numReg;
					prog.emit(unroll, Const, new Value(48), asciiReg);
					prog.emit(Const, new Value(1), oneReg);
					
					// Get a digit
					prog.emit(beginUnroll, Pop, workReg);
					
					// Convert it to ascii and decrement the digit count
					prog.emit(Compute, new Operator(Sub), countReg, oneReg, countReg);
					prog.emit(Compute, new Operator(Add), workReg, asciiReg, workReg);
					
					// Output it
					prog.emit(Write, workReg, new MemAddr("stdio"));
					
					// If all digits have been written to stdio, we're done
					prog.emit(Compute, new Operator(Equal), new Reg(Zero), countReg, workReg);
					prog.emit(Branch, workReg, new Target(done));
					
					// Otherwise, print another digit
					prog.emit(Jump, new Target(beginUnroll));
					
					// Emit done label here
					prog.emit(done, Pop, new Reg(RegE));
					prog.emit(Jump, new Target(RegE));
					
					leaveFunc();
				}
				
				String returnLabel = Program.mkLbl(ctx, "intreturn");

				prog.emit(Const, new Value(returnLabel), new Reg(RegD));
				prog.emit(Push, new Reg(RegD));
				prog.emit(Jump, new Target(printIntLabel));
				prog.emit(returnLabel, Nop);
			} else if (checkResult.getType(ctx.expr()).equals(new Type.Char())) { 
				// print a single char
				if (printCharLabel == null) {
					printCharLabel = "pipeOp_char";
					
					enterFunc(printCharLabel);
					// Output it
					prog.emit(printCharLabel, Write, new Reg(RegE), new MemAddr("stdio"));
					
					prog.emit(Pop, new Reg(RegE));
					prog.emit(Jump, new Target(RegE));
					
					leaveFunc();
				}
				
				String returnLabel = Program.mkLbl(ctx, "charreturn");

				prog.emit(Const, new Value(returnLabel), new Reg(RegD));
				prog.emit(Push, new Reg(RegD));	
				prog.emit(Jump, new Target(printCharLabel));
				prog.emit(returnLabel, Nop);
			} else {
				System.out.println("Unsupported type given to stdout " + ctx.getText());
				return null;
			}
		}
		
		Reg workReg = new Reg(RegE);
		// Haskell seems to have no trouble with the \r\n/\n business.
		if (ctx.newlines().getChildCount() > 0) prog.emit(Const, new Value((int) "\n".toCharArray()[0]), workReg);
		for (int i = 0; i < ctx.newlines().getChildCount(); i++) {
			prog.emit(Write, workReg, new MemAddr("stdio"));
		}
		
		return null;
	}
}
