package comp;

import static sprockell.Operator.Which.*;
import static sprockell.Program.*;
import static sprockell.Reg.Which.*;
import static sprockell.Sprockell.Op.*;

import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import lang.BurritoBaseVisitor;
import lang.BurritoParser.ArrayExprContext;
import lang.BurritoParser.ArrayTargetContext;
import lang.BurritoParser.AndExprContext;
import lang.BurritoParser.AssStatContext;
import lang.BurritoParser.BlockContext;
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
import org.antlr.v4.runtime.tree.TerminalNode;

import comp.Type.Array;
import sprockell.Instr;
import sprockell.MemAddr;
import sprockell.Operator;
import sprockell.Program;
import sprockell.Reg;
import sprockell.Target;
import sprockell.Value;

public class Generator extends BurritoBaseVisitor<List<Instr>> {
	public final String MAINMETHOD = "program";
	
	private Program prog;
	private Result checkResult;
	private Stack<Program> progStack = new Stack<>();
	private Map<String,Program> progMap = new HashMap<>();
	
	private String mainLabel;
	private String endLabel;
	
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

	@Override
	public List<Instr> visitProgram(ProgramContext ctx) {
		endLabel = Program.mkLbl(ctx, "done");
		Reg zero = new Reg(Zero);
		
		// Generate a program for each function
		for (FuncContext asc : ctx.func()) {
			visit(asc);
		}
		
		if (!progMap.containsKey(MAINMETHOD)) {
			System.out.println(MAINMETHOD + " NOT FOUND, undefined behaviour from here on");
			return null;
		}
		
		// Start making the final program here
		// First construct the "fake" stack for program()
		prog.emit(Push, zero); // Return value of program(); (it's an int)
		prog.emit(Push, zero); // RegA
		prog.emit(Push, zero); // RegB
		prog.emit(Push, zero); // RegC
		prog.emit(Push, zero); // RegD
		prog.emit(Push, zero); // RegE
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
		prog.emit(Nop);
		prog.emit(Nop);
		prog.emit(Nop);
		prog.emit(Nop);
		prog.emit(Nop);
		prog.emit(EndProg);
		
		return null;
	}
	
	@Override
	public List<Instr> visitFunc(FuncContext ctx) {
		Function func = checkResult.getFunction(ctx);
		
		enterFunc(func.id);

		if (func.id.equals(MAINMETHOD)) {
			mainLabel = func.label;
			if (func.args.length > 0) {
				System.out.println("Warning, giving " + MAINMETHOD + " more than one argument causes undefined behaviour!");
			}
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
		prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegD), new Reg(RegB));
		// mem addr => RegB get target value
		prog.emit(Load, new MemAddr(RegB), new Reg(RegC));
		prog.emit(Compute, new Operator(op), new Reg(RegC), new Reg(RegE), new Reg(RegE));
		prog.emit(Store, new Reg(RegE), new MemAddr(RegB));
		return null;
	}
	
	@Override
	public List<Instr> visitTypeAssignStat(TypeAssignStatContext ctx) {
		visit(ctx.expr());
		// TODO: Replace with storeAI (store reg => reg, offset)
		Type type = checkResult.getType(ctx.ID());
		for (int i = 0; i < type.size(); i++) {
			prog.emit(Push, new Reg(Zero));
		}
		prog.emit(Const, new Value(checkResult.getOffset(ctx.ID())), new Reg(RegB));
		prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegB), new Reg(RegB));
		prog.emit(Store, new Reg(RegE), new MemAddr(RegB));

		return null;
	}
	
	@Override
	public List<Instr> visitAssStat(AssStatContext ctx) {
		visit(ctx.target());
		prog.emit(Push, new Reg(RegE));
		visit(ctx.expr());
		prog.emit(Pop, new Reg(RegD));
		// D = offset, E = Waarde
		
		// TODO: Replace with storeAI (store reg => reg, offset)
		
		// Waarde mem[arp + offset]
		// Changed the thing here to SUB TODO
		prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegD), new Reg(RegB));
		prog.emit(Store, new Reg(RegE), new MemAddr(RegB));
		return null;
	}
	
	@Override
	public List<Instr> visitTypeStat(TypeStatContext ctx) {
		visit(ctx.type());
		
		Type type = checkResult.getType(ctx.ID());
		for (int i = 0; i < type.size(); i++) {
			prog.emit(Push, new Reg(Zero));
		}
		
		return null;
	}
	
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
		prog.emit(Const, new Value(checkResult.getOffset(ctx)), new Reg(RegB));
		// RegB + RegE -> RegE
		prog.emit(Compute, new Operator(Add), new Reg(RegB), new Reg(RegC), new Reg(RegE));
		//prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegE), new Reg(RegE));
		return null;
	}
	
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
		// sub?
		prog.emit(Compute, new Operator(Add), new Reg(RegB), new Reg(RegC), new Reg(RegC));
		prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegC), new Reg(RegB));
		prog.emit(Load, new MemAddr(RegB), new Reg(RegE));
		return null;
	}
	
	@Override
	public List<Instr> visitIncExpr(IncExprContext ctx) {
		visit(ctx.target());
		
		prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegE), new Reg(RegB));
		prog.emit(Load, new MemAddr(RegB), new Reg(RegC));
		prog.emit(Const, new Value(ctx.PLUS().size()), new Reg(RegD));
		prog.emit(Compute, new Operator(Add), new Reg(RegC), new Reg(RegD), new Reg(RegE));
		prog.emit(Store, new Reg(RegE), new MemAddr(RegB));
		return null;
	}
	
	@Override
	public List<Instr> visitDecExpr(DecExprContext ctx) {
		visit(ctx.target());
		
		prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegE), new Reg(RegB));
		prog.emit(Load, new MemAddr(RegB), new Reg(RegC));
		prog.emit(Const, new Value(ctx.MIN().size()), new Reg(RegD));
		prog.emit(Compute, new Operator(Sub), new Reg(RegC), new Reg(RegD), new Reg(RegE));
		prog.emit(Store, new Reg(RegE), new MemAddr(RegB));
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
		prog.emit(Const, new Value(checkResult.getOffset(ctx)), new Reg(RegB));
		prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegB), new Reg(RegB));
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
	
	@Override
	public List<Instr> visitNegExpr(NegExprContext ctx) {
		visit(ctx.expr());
		prog.emit(Compute, new Operator(Sub), new Reg(Zero), new Reg(RegE), new Reg(RegE));
		return null;
	}
	
	@Override
	public List<Instr> visitFuncExpr(FuncExprContext ctx) {
		Function func = checkResult.getFunction(ctx.ID());
		
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
	
	@Override
	public List<Instr> visitReturnStat(ReturnStatContext ctx) {
		if (ctx.expr() != null) {
			visit(ctx.expr());
			
			// Now move the value from regE to the stack.
		}
		
		Function func = checkResult.getFunction(ctx.parent);
		
		// Pop all current stack variables off the stack
		int currStackSize = checkResult.getStackSize(ctx); 
		
		// Unwind the stack
		// TODO: Optimize this to an instruction that just changes the stack pointer
		// Bonus: this does null every stack entry :-)
		for (int i = 0; i < currStackSize; i++) {
			prog.emit(Pop, new Reg(Zero));
		}
		
		// ARP is now at the integer AFTER the last parameter, so we 
		// pop one more to let the SP point to the last parameter
		// (or if there were no parameters, the return value)
		prog.emit(Pop, new Reg(Zero));
		
		// Unpop all the parameters
		for (Type arg : func.args) {
			for (int i = 0; i < arg.size(); i++) {
				prog.emit(Pop, new Reg(Zero));
			}
		}
		
		// Store the return value in the return value field
		if (ctx.expr() != null) {
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
				Reg workReg = new Reg(RegD);
				String equals = Program.mkLbl(ctx, "equals");
				String end = Program.mkLbl(ctx, "end");
				
				prog.emit(Compute,  new Operator(Equal), new Reg(Zero), new Reg(RegE), workReg);
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
				
				prog.emit(end, Nop);
			} else if (checkResult.getType(ctx.expr()).equals(new Type.Int())) {
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
				prog.emit(Const, new Value(10), orderReg);
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
				prog.emit(done, Nop);
			} else {
				System.out.println("Unsupported type given to stdout " + ctx.getText());
				return null;
			}
		}
		
		Reg workReg = new Reg(RegE);
		String nl = "\n"; // System.lineSeparator(); // Sprockell seems to handle \n just fine

		for (int i = 0; i < ctx.newlines().getChildCount(); i++) {
			for (char c : nl.toCharArray()) {
				prog.emit(Const, new Value(c), workReg);
				prog.emit(Write, workReg, new MemAddr("stdio"));
			}
		}
		
		return null;
	}
}
