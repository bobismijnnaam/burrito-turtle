package comp;

import static comp.Reach.*;
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
import lang.BurritoParser.AssStatContext;
import lang.BurritoParser.BlockContext;
import lang.BurritoParser.CharacterExprContext;
import lang.BurritoParser.DecExprContext;
import lang.BurritoParser.DeclContext;
import lang.BurritoParser.DeferExprContext;
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
import lang.BurritoParser.IncExprContext;
import lang.BurritoParser.LitExprContext;
import lang.BurritoParser.LiteralExprContext;
import lang.BurritoParser.LockStatContext;
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
import lang.BurritoParser.PowExprContext;
import lang.BurritoParser.ProgramContext;
import lang.BurritoParser.ReturnStatContext;
import lang.BurritoParser.StartStatContext;
import lang.BurritoParser.StatContext;
import lang.BurritoParser.SwitchStatContext;
import lang.BurritoParser.TrueExprContext;
import lang.BurritoParser.TypeAssignStatContext;
import lang.BurritoParser.TypeStatContext;
import lang.BurritoParser.UnlockStatContext;
import lang.BurritoParser.WhileStatContext;
import lang.BurritoParser.XorExprContext;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import sprockell.Addr;
import sprockell.Instr;
import sprockell.MemAddr;
import sprockell.Operator;
import sprockell.Program;
import sprockell.Reg;
import sprockell.Target;
import sprockell.Value;

import comp.Type.Array;
import comp.Type.ArrayLiteral;
import comp.Type.Bool;
import comp.Type.Char;
import comp.Type.Int;
import comp.Type.Pointer;
import comp.Type.StringLiteral;

public class Generator extends BurritoBaseVisitor<List<Instr>> {
	public static final String MAINMETHOD = "program";
	
	private Program prog;
	private Result checkResult;
	private Stack<Program> progStack = new Stack<>();
	private Map<String,Program> progMap = new HashMap<>();
	
	private String mainLabel;
	private String endLabel;
	
	private String printBoolLabel;
	private String printIntLabel;
	private String printCharLabel;
	private String printPointerLabel;
	private String idleFuncLabel;
	
	// stdio = Addr stdioAddr
	// stdioAddr = 0x1000000
	private final MemAddr stdio = new MemAddr(new Addr(0x1000000));
	
	private final int sprockellSegmentSize = 1;
	
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

	public Program generate(ParseTree tree, Result checkResult, int cores) {
		this.prog = new Program();
		this.checkResult = checkResult;
		tree.accept(this);
		return this.prog;
	}
	
	public Program generate(ParseTree tree, Result checkResult) {
		return generate(tree, checkResult, 1);
	}

	// TODO: Make a consumer/producer program
	// TODO: Make a banking system
	@Override
	public List<Instr> visitProgram(ProgramContext ctx) {
		endLabel = Program.mkLbl(ctx, "done");
		Reg zero = new Reg(Zero);

		includeIdleFunc();
		
		prog.emit(Compute, new Operator(NEq), new Reg(SPID), new Reg(Zero), new Reg(RegE));
		prog.emit(Branch, new Reg(RegE), new Target(idleFuncLabel));
		
		// Only the manager thread gets to initialize the shared memory
		for (DeclContext dtx : ctx.decl()) {
			visit(dtx);
		}
		
		// Generate a program for each function (has to happen at some point)
		for (FuncContext asc : ctx.func()) {
			visit(asc);
		}

		// Start making the final program here
		// First construct the "fake" stack for program()
		prog.emit(Const, new Value(endLabel), new Reg(RegE));
		prog.emit(Push, new Reg(RegE)); // Return address
		// We push a zero here to let the ARP point to the FIRST byte after the last parameter
		// (or the return value of there were no parameters)
		prog.emit(Push, zero);
		prog.emit(Compute, new Operator(Add), new Reg(SP), zero, new Reg(RegA)); // Set the initial ARP
		prog.emit(Jump, new Target(mainLabel));
		
		// Finishing up the program happens after this line
		
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
	public List<Instr> visitDecl(DeclContext ctx) {
		if (checkResult.getReach(ctx.ID()) != Global) {
			System.out.println("[Generator] Global variable reach should be");
			return null;
		}
		
		Type type = checkResult.getType(ctx.type());
		
		// Initialize id part of lock to -1
		if (type instanceof Type.Lock) {
			prog.emit(Const, new Value(checkResult.getOffset(ctx.ID())), new Reg(RegE));
			prog.emit(Const, new Value(-1), new Reg(RegD));
			prog.emit(Compute, new Operator(Sub), new Reg(RegE), new Reg(RegD), new Reg(RegE));
			prog.emit(Write, new Reg(RegD), new MemAddr(RegE));
		}
		
		if (ctx.expr() != null) {
			Type right = checkResult.getType(ctx.expr());
			if (right instanceof StringLiteral) {
				StringLiteral sl = (StringLiteral) right;
				prog.emit(Const, new Value(checkResult.getOffset(ctx.ID())), new Reg(RegD));
				prog.emit(Const, new Value(1), new Reg(RegC));
				
				for (char c : sl.content.toCharArray()) {
					prog.emit(Const, new Value(c), new Reg(RegE));
					prog.emit(Write, new Reg(RegE), new MemAddr(RegD));
					prog.emit(Compute, new Operator(Add), new Reg(RegD), new Reg(RegC), new Reg(RegD));
				}
				prog.emit(Write, new Reg(Zero), new MemAddr(RegD));
				
			} else if (right instanceof ArrayLiteral) {
				ArrayLiteral al = (ArrayLiteral) right;
				
				prog.emit(Const, new Value(checkResult.getOffset(ctx.ID())), new Reg(RegD));
				prog.emit(Const, new Value(1), new Reg(RegC));
				
				for (int i = 0; i < al.arrSize; i++) {
					prog.emit(Const, new Value(al.contents[i]), new Reg(RegE));
					prog.emit(Write, new Reg(RegE), new MemAddr(RegD));
					prog.emit(Compute, new Operator(Add), new Reg(RegD), new Reg(RegC), new Reg(RegD));
				}
				
				prog.emit(Push, new Reg(Zero));
			} else {
				visit(ctx.expr());
				// Value is now in E
				// TODO: What if type is object or array?
				
				// Get offset
				prog.emit(Const, new Value(checkResult.getOffset(ctx.ID())), new Reg(RegD));
				// Write E to offset
				prog.emit(Write, new Reg(RegE), new MemAddr(RegD));
			}
		}
		
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
		return emitOpAss(ctx.expr(0), ctx.expr(1), Add);
	}
	
	@Override
	public List<Instr> visitMinAssStat(MinAssStatContext ctx) {
		return emitOpAss(ctx.expr(0), ctx.expr(1), Sub);
	}
	
	@Override
	public List<Instr> visitDivAssStat(DivAssStatContext ctx) { 
		return emitOpAss(ctx.expr(0), ctx.expr(1), Div);
	}
	
	@Override
	public List<Instr> visitMulAssStat(MulAssStatContext ctx) {
		return emitOpAss(ctx.expr(0), ctx.expr(1), Mul);
	}
	
	private List<Instr> emitOpAss(ExprContext target, ExprContext param, Operator.Which op) {
		visit(target); // E contains the initial value, D contains the address with bit
		prog.emit(Push, new Reg(RegE)); // Push the resulting value
		prog.emit(Push, new Reg(RegD)); // Push the address with address space bit
		visit(param); // E now contains the usable value
		
		Type left = checkResult.getType(target);
		Type right = checkResult.getType(param);
		
		if (left instanceof Pointer) {
			Pointer ptr = (Pointer) left; 
			prog.emit(Compute, new Operator(Add), new Reg(RegE), new Reg(Zero), new Reg(RegC));
			prog.emit(Const, new Value(ptr.pointsTo.size()), new Reg(RegB));
			prog.emit(Compute, new Operator(Mul), new Reg(RegB), new Reg(RegC), new Reg(RegC));
			prog.emit(Pop, new Reg(RegD));
			prog.emit(Pop, new Reg(RegE));
			prog.emit(Push, new Reg(RegD));

			if (op == Equal) {
				// Sure, initialize your pointer with an integer
			} else if (op == Add) {
				adjustPtrInEToC(target);
			} else if (op == Sub) {
				prog.emit(Compute, new Operator(Sub), new Reg(Zero), new Reg(RegC), new Reg(RegC));
				adjustPtrInEToC(target);
			}

			prog.emit(Pop, new Reg(RegD));
		} else {
			prog.emit(Pop, new Reg(RegD)); // Restore the target address
			prog.emit(Pop, new Reg(RegC)); // Put the previous value in C
			prog.emit(Compute, new Operator(op), new Reg(RegC), new Reg(RegE), new Reg(RegE)); // Put the new value in E
		}
		
		// Save E at the address pointed to by D
		saveEinDerefD((ParserRuleContext) target.parent);

		return null;
	}
	
	@Override
	public List<Instr> visitTypeAssignStat(TypeAssignStatContext ctx) {
		visit(ctx.expr());
		
		Reach reach = checkResult.getReach(ctx.ID());
		
		if (reach == Global) {
			System.out.println("[Generator] THIS IS WRONG");
		} else if (reach == Local) {
			Type type = checkResult.getType(ctx.ID());
			
			Type right = checkResult.getType(ctx.expr());
			if (right instanceof StringLiteral) {
				StringLiteral sl = (StringLiteral) right;
				
				char[] chars = sl.content.toCharArray();
				prog.emit(Const, new Value(chars[0]), new Reg(RegE));
				prog.emit(Store, new Reg(RegE), new MemAddr(SP));
				
				for (int i = 1; i < chars.length; i++) {
					prog.emit(Const, new Value(chars[i]), new Reg(RegE));
					prog.emit(Push, new Reg(RegE));
				}

				prog.emit(Push, new Reg(Zero));
				prog.emit(Push, new Reg(Zero));
			} else if (right instanceof ArrayLiteral) {
				ArrayLiteral al = (ArrayLiteral) right;
				
				if (al.elemType instanceof Int) {
					prog.emit(Const, new Value(al.contents[0]), new Reg(RegE));
					prog.emit(Store, new Reg(RegE), new MemAddr(SP));
					
					for (int i = 1; i < al.arrSize; i++) {
						prog.emit(Const, new Value(al.contents[i]), new Reg(RegE));
						prog.emit(Push, new Reg(RegE));
					}
					
					prog.emit(Push, new Reg(Zero));
				} else if (al.elemType instanceof Char) {
					// TODO
				} else if (al.elemType instanceof Bool) {
					// TODO
				}
				
			} else {
				for (int i = 0; i < type.size(); i++) {
					prog.emit(Push, new Reg(Zero));
				}

				prog.emit(Const, new Value(checkResult.getOffset(ctx.ID())), new Reg(RegB));
				prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegB), new Reg(RegB));
				prog.emit(Store, new Reg(RegE), new MemAddr(RegB));
			}
			
		}
		
		return null;
	}
	
	/**
	 * Saves value in register E in address pointed to by D. D SHOULD contain the address bit!
	 * When in doubt, this method uses ALL REGISTERS! After use, D still contains the address bit.
	 * @param ctx A seed for the mkLbl function
	 */
	public void saveEinDerefD(ParserRuleContext ctx) {
		String globalLabel = Program.mkLbl(ctx, "globalAssignment");
		String endLabel = Program.mkLbl(ctx, "endAssignment");
		
		prog.emit(Const, new Value(31), new Reg(RegC));
		prog.emit(Compute, new Operator(RShift), new Reg(RegD), new Reg(RegC), new Reg(RegC)); // C now contains -1 if it's global, 0 if it's local
		prog.emit(Compute, new Operator(Mul), new Reg(RegC), new Reg(RegC), new Reg(RegC)); // C now contains 1 if it's global, 0 if it's local
		prog.emit(Branch, new Reg(RegC), new Target(globalLabel));

		// It should be saved on stack
		prog.emit(Store, new Reg(RegE), new MemAddr(RegD));
		prog.emit(Jump, new Target(endLabel));
		
		// It should be saved on the heap
		prog.emit(globalLabel, Const, new Value(-2147483648), new Reg(RegB));
		prog.emit(Compute, new Operator(Xor), new Reg(RegB), new Reg(RegD), new Reg(RegD)); // RegD now contains the address without the address space bit
		prog.emit(Write, new Reg(RegE), new MemAddr(RegD));
		prog.emit(Compute, new Operator(Xor), new Reg(RegB), new Reg(RegD), new Reg(RegD)); // RegD now contains the address without the address space bit
		prog.emit(endLabel, Nop);
	}
	
	@Override
	public List<Instr> visitAssStat(AssStatContext ctx) {
		visit(ctx.expr(0));
		prog.emit(Push, new Reg(RegD));
		visit(ctx.expr(1));
		prog.emit(Pop, new Reg(RegD));
		
		saveEinDerefD(ctx);
		
		return null;
	}
	
	@Override
	public List<Instr> visitTypeStat(TypeStatContext ctx) {
		visit(ctx.type());
		
		if (checkResult.getReach(ctx.ID()) != Local) System.out.println("[Generator] This can't be global, has to be local");

		Type type = checkResult.getType(ctx.ID());
		for (int i = 0; i < type.size(); i++) {
			prog.emit(Push, new Reg(Zero));
		}
		
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
	public List<Instr> visitSwitchStat(SwitchStatContext ctx) {
		String defaultLabel = mkLbl(ctx, "default");
		String endLabel = mkLbl(ctx, "end");
		
		int curCase = 1;
		int block = 0;
		
		String endCaseLabel = mkLbl(ctx, "case" + curCase + "end");
		
		// put value to switch on in E
		visit(ctx.expr());
		prog.emit(Push, new Reg(RegE));
		prog.emit(Pop, new Reg(RegB));
		
		// for each case
		for (LitExprContext lit : ctx.litExpr()) {
			visit(lit);
			prog.emit(Compute, new Operator(NEq), new Reg(RegB), new Reg(RegE), new Reg(RegC));

			prog.emit(Branch, new Reg(RegC), new Target(endCaseLabel));
			visit(ctx.block(block));
			block++;
			curCase++;
			prog.emit(Jump, new Target(endLabel));
			prog.emit(endCaseLabel, Nop);
			endCaseLabel = mkLbl(ctx, "case" + curCase + "end");
		}
		
		// default case
		if (ctx.DEFAULT() != null) {
			prog.emit(defaultLabel, Nop);
			if (!ctx.block().isEmpty()) {
				visit(ctx.block(ctx.block().size() - 1));
			} // else { // TODO: What is this?
			  // visit(ctx.block(0));
			// }
			prog.emit(Jump, new Target(endLabel));
		}
		
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
	public List<Instr> visitLockStat(LockStatContext ctx) {
		String loopLabel = Program.mkLbl(ctx, "looplabel");
		
		Reg offsetReg = new Reg(RegE);
		Reg testReg = new Reg(RegD);
		Reg zero = new Reg(Zero);
		
		prog.emit(Const, new Value(checkResult.getOffset(ctx)), offsetReg);
		prog.emit(loopLabel, TestAndSet, new MemAddr(RegE));
		prog.emit(Receive, testReg);
		prog.emit(Compute, new Operator(Equal), testReg, zero, testReg);
		prog.emit(Branch, testReg, new Target(loopLabel));
		prog.emit(Const, new Value(checkResult.getOffset(ctx) + 1), offsetReg);
		prog.emit(Write, new Reg(SPID), new MemAddr(RegE));
		
		return null;
	}
	
	@Override
	public List<Instr> visitUnlockStat(UnlockStatContext ctx) {
		Reg offsetReg = new Reg(RegE);
		Reg idReg = new Reg(RegD);
		Reg workReg = new Reg(RegC);

		String nothingWrongLabel = Program.mkLbl(ctx, "noworries");
		
		prog.emit(Const, new Value(checkResult.getOffset(ctx) + 1), idReg);
		prog.emit(Const, new Value(checkResult.getOffset(ctx)), offsetReg);

		prog.emit(Read, new MemAddr(RegD));
		prog.emit(Receive, workReg);
		prog.emit(Compute, new Operator(Equal), workReg, new Reg(SPID), workReg);
		prog.emit(Branch, workReg, new Target(nothingWrongLabel));

		// TODO: Test if spid recoginition works
		
		// Write ~! to stdout to indicate wrong lock opening
		prog.emit(Const, new Value(126), workReg);
		prog.emit(Write, workReg, stdio);
		prog.emit(Const, new Value(33), workReg);
		prog.emit(Write, workReg, stdio);
		
		prog.emit(nothingWrongLabel, Const, new Value(-1), workReg);
		prog.emit(Write, workReg, new MemAddr(RegD));
		prog.emit(Write, new Reg(Zero), new MemAddr(RegE));
		
		return null;
	}
	
	@Override
	public List<Instr> visitNotExpr(NotExprContext ctx) {
		visit(ctx.expr());
		prog.emit(Compute, new Operator(Equal), new Reg(RegE), new Reg(Zero), new Reg(RegE));
		return null;
	}
	
	// TODO: Handle pointers here
	private List<Instr> emitArOp(ParserRuleContext ctx, Operator.Which op) {
		visit(ctx.getChild(0));
		prog.emit(Push, new Reg(RegE));
		visit(ctx.getChild(2));
		prog.emit(Pop, new Reg(RegD));
		prog.emit(Compute, new Operator(op), new Reg(RegD), new Reg(RegE), new Reg(RegE));
		
		return null;
	}
	
//	@Override
//	public List<Instr> visitArrayExpr(ArrayExprContext ctx) {
//		for (ExprContext expr : ctx.expr()) {
//			visit(expr);
//			prog.emit(Push, new Reg(RegE));
//		}
//		
//		Type type = checkResult.getType(ctx.ID()); 
//		
//		if (type instanceof Array) {
//			Array array = (Array) type;
//
//			prog.emit(Const, new Value(0), new Reg(RegC));
//			
//			int size = 1;
//			for (int i = array.indexSize.size() - 1; i >= 0; i--) {
//				prog.emit(Pop, new Reg(RegE));
//				prog.emit(Const, new Value(size), new Reg(RegD));
//				prog.emit(Compute, new Operator(Mul), new Reg(RegE), new Reg(RegD), new Reg(RegE));
//				prog.emit(Compute, new Operator(Add), new Reg(RegE), new Reg(RegC), new Reg(RegC));
//				size *= array.indexSize.get(i);
//			}
//			
//			// Waarde mem[arp - offset]
//			// The +1 is to account for the length integer that resides at the arrays' address
//			System.out.println(((Array) checkResult.getType(ctx.ID())).isOuter());
//			prog.emit(Const, new Value(checkResult.getOffset(ctx.ID()) + 1), new Reg(RegB));
//			prog.emit(Compute, new Operator(Add), new Reg(RegB), new Reg(RegC), new Reg(RegC));
//			
//			Reach reach = checkResult.getReach(ctx.ID());
//			if (reach == Local) {
//				prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegC), new Reg(RegB));
//				prog.emit(Load, new MemAddr(RegB), new Reg(RegE));
//			} else if (reach == Global) {
//				prog.emit(Read, new MemAddr(RegC));
//				prog.emit(Receive, new Reg(RegE));
//			}
//		} else if (type instanceof AnyArray) {
//			AnyArray array = (AnyArray) type;
//			
//			// The first offset
//			prog.emit(Const, new Value(array.getBaseType().size()), new Reg(RegC));
//			prog.emit(Pop, new Reg(RegB));
//			prog.emit(Compute, new Operator(Mul), new Reg(RegB), new Reg(RegC), new Reg(RegC));
//			
//			// C now holds the offset within the array
//			
//			if (array.elemType instanceof Array) {
//				// TODO: Implement this
//				System.out.println("Variable nested arrays not yet implemented");
//			} 
//
//			// We have the correct offset!
//
//			prog.emit(Const, new Value(checkResult.getOffset(ctx.ID())), new Reg(RegE));
//			prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegE), new Reg(RegE));
//			prog.emit(Load, new MemAddr(RegE), new Reg(RegD));
//			prog.emit(Const, new Value(1), new Reg(RegB));
//			prog.emit(Compute, new Operator(Sub), new Reg(RegE), new Reg(RegB), new Reg(RegE));
//			prog.emit(Load, new MemAddr(RegE), new Reg(RegE));
//			prog.emit(Compute, new Operator(Add), new Reg(RegE), new Reg(RegC), new Reg(RegE));
//			
//			// RegD contains if the array is global or not
//			// RegE contains the offset of the element
//			String stackLabel = Program.mkLbl(ctx, "stackLabel");
//			String doneLabel = Program.mkLbl(ctx, "finishedLabel");
//			
//			prog.emit(Compute, new Operator(Equal), new Reg(RegD), new Reg(Zero), new Reg(RegD));
//			prog.emit(Branch, new Reg(RegD), new Target(stackLabel));
//			prog.emit(Read, new MemAddr(RegE));
//			prog.emit(Receive, new Reg(RegE));
//			prog.emit(Jump, new Target(doneLabel));
//			prog.emit(stackLabel, Load, new MemAddr(RegE), new Reg(RegE)); // Was the ARP added?
//			prog.emit(doneLabel, Nop);
//			
//			// E contains the offset of the first array element. D contains whether or not it's a global array
//		}
//		
//		return null;
//	}
	
	/**
	 * Moves a pointer (with address bit set) in an array-like fashion: stack pointers with + towards 0,
	 * and shared pointers with + towards infinity, and the other way around with minus.
	 * When in doubt, this sequence needs ALL REGISTERS (except A)
	 * @param ctx A seed for mkLbl
	 * @param dist The distance the ptr has to move
	 */
	public void adjustPtrInE(ParserRuleContext ctx, int dist) {
		prog.emit(Const, new Value(dist), new Reg(RegC));
		adjustPtrInEToC(ctx);
	}
	
	/**
	 * Moves a pointer (with address bit set) in an array-like fashion: stack pointers with + towards 0,
	 * and shared pointers with + towards infinity, and the other way around with minus. The distance
	 * moved is taken from C.
	 * When in doubt, this sequence needs ALL REGISTERS (except A)
	 * @param ctx A seed for mkLbl
	 */
	public void adjustPtrInEToC(ParserRuleContext ctx) {
		String globalLabel = Program.mkLbl(ctx, "globalIncLabel");
		String endLabel = Program.mkLbl(ctx, "decideEndLabel");
		
		prog.emit(Const, new Value(31), new Reg(RegD));

		prog.emit(Compute, new Operator(RShift), new Reg(RegE), new Reg(RegD), new Reg(RegD));
		prog.emit(Compute, new Operator(Mul), new Reg(RegD), new Reg(RegD), new Reg(RegD));
		prog.emit(Branch, new Reg(RegD), new Target(globalLabel));
		
		// Stack pointer
		prog.emit(Compute, new Operator(Sub), new Reg(RegE), new Reg(RegC), new Reg(RegE)); // E now contains the incremented value
		prog.emit(Jump, new Target(endLabel));
		
		// Global pointer
		prog.emit(globalLabel, Const, new Value(-2147483648), new Reg(RegD));
		prog.emit(Compute, new Operator(Xor), new Reg(RegD), new Reg(RegE), new Reg(RegE));
		prog.emit(Compute, new Operator(Add), new Reg(RegC), new Reg(RegE), new Reg(RegE));
		prog.emit(Compute, new Operator(Xor), new Reg(RegD), new Reg(RegE), new Reg(RegE));

		prog.emit(endLabel, Nop);
	}
	
	@Override
	public List<Instr> visitIncExpr(IncExprContext ctx) {
		visit(ctx.expr()); // E now contains the value in E and the address in D
		
		Type type = checkResult.getType(ctx);
		
		if (type instanceof Pointer) {
			prog.emit(Push, new Reg(RegD));
			
			adjustPtrInE(ctx, ctx.PLUS().size());

			prog.emit(endLabel, Pop, new Reg(RegD));
		} else if (type instanceof Int || type instanceof Char) {
			prog.emit(Const, new Value(ctx.PLUS().size()), new Reg(RegC));
			prog.emit(Compute, new Operator(Add), new Reg(RegC), new Reg(RegE), new Reg(RegE)); // E now contains the incremented value
		}
		
		saveEinDerefD(ctx);
		
		return null;
	}
	
	@Override
	public List<Instr> visitDecExpr(DecExprContext ctx) {
		visit(ctx.expr()); // E now contains the value in E and the address in D
		
		Type type = checkResult.getType(ctx);
		
		if (type instanceof Pointer) {
			prog.emit(Push, new Reg(RegD));
			
			adjustPtrInE(ctx, -1 * ctx.MIN().size());

			prog.emit(endLabel, Pop, new Reg(RegD));
		} else if (type instanceof Int || type instanceof Char) {
			prog.emit(Const, new Value(-1 * ctx.MIN().size()), new Reg(RegC));
			prog.emit(Compute, new Operator(Add), new Reg(RegC), new Reg(RegE), new Reg(RegE)); // E now contains the incremented value
		}
		
		saveEinDerefD(ctx);

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
	public List<Instr> visitNeqExpr(NeqExprContext ctx) {
		return emitArOp(ctx, NEq);
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
//		prog.emit(Const, new Value(checkResult.getOffset(ctx)), new Reg(RegD));
//		
//		Reach reach = checkResult.getReach(ctx);
//		if (reach == Local) {
//			prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegD), new Reg(RegD));
//			prog.emit(Load, new MemAddr(RegD), new Reg(RegE));
//		} else if (reach == Global) {
//			prog.emit(Const, new Value(-2147483648), new Reg(RegC)); // Value of 32 bits: 1000...000
//			prog.emit(Compute, new Operator(Or), new Reg(RegC), new Reg(RegD), new Reg(RegD)); // set the address bit to 1 to indicate global variable
//			prog.emit(Read, new MemAddr(RegD));
//			prog.emit(Receive, new Reg(RegE));
//		} 
		
		int offset = checkResult.getOffset(ctx);
		Reach reach = checkResult.getReach(ctx);
		Type type = checkResult.getType(ctx);
		
		prog.emit(Const, new Value(offset), new Reg(RegD));
		
		if (type instanceof Array) {
			if (reach == Local) {
				prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegD), new Reg(RegD));
				prog.emit(Compute, new Operator(Add), new Reg(RegD), new Reg(Zero), new Reg(RegE));
			} else if (reach == Global) {
				prog.emit(Const, new Value(-2147483648), new Reg(RegC)); // Value of 32 bits: 1000...000
				prog.emit(Compute, new Operator(Or), new Reg(RegC), new Reg(RegD), new Reg(RegD)); // set the address bit to 1 to indicate global variable
				prog.emit(Compute, new Operator(Add), new Reg(RegD), new Reg(Zero), new Reg(RegE));
			} else {
				System.out.println("[Generator] Reach was not a defined value!");
			}			
		} else {
			if (reach == Local) {
				prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegD), new Reg(RegD));
				prog.emit(Load, new MemAddr(RegD), new Reg(RegE));
			} else if (reach == Global) {
				prog.emit(Read, new MemAddr(RegD));
				prog.emit(Receive, new Reg(RegE));
				prog.emit(Const, new Value(-2147483648), new Reg(RegC)); // Value of 32 bits: 1000...000
				prog.emit(Compute, new Operator(Or), new Reg(RegC), new Reg(RegD), new Reg(RegD)); // set the address bit to 1 to indicate global variable
			} else {
				System.out.println("[Generator] Reach was not a defined value!");
			}
		}
		
		return null;
	}
	
	@Override
	public List<Instr> visitNumExpr(NumExprContext ctx) {
		prog.emit(Const, new Value(new Integer(ctx.NUM().getText())), new Reg(RegE));
		prog.emit(Const, new Value(-1), new Reg(RegD)); // To properly guard against when RegD is used inappropriately
		return null;
	}
	
	@Override
	public List<Instr> visitCharacterExpr(CharacterExprContext ctx) {
		String c = ctx.CHARACTER().getText();
		String cStrip = c.replaceAll("'", "");
		
		if (cStrip.equals("\\0")) {
			prog.emit(Const, new Value(0), new Reg(RegE));
		} else if (cStrip.equals("\\n")) {
			prog.emit(Const, new Value("\n".charAt(0)), new Reg(RegE));
		} else if (cStrip.length() == 1){
			char ch = c.charAt(1);
			int i = (int) ch;
			prog.emit(Const, new Value(i), new Reg(RegE));
		} 

		prog.emit(Const, new Value(-1), new Reg(RegD));
		
		return null;
	}
	
	@Override
	public List<Instr> visitTrueExpr(TrueExprContext ctx) {
		prog.emit(Const, new Value(1), new Reg(RegE));
		prog.emit(Const, new Value(-1), new Reg(RegD));
		return null;
	}
	
	@Override
	public List<Instr> visitFalseExpr(FalseExprContext ctx) {
		prog.emit(Const, new Value(0), new Reg(RegE));
		prog.emit(Const, new Value(-1), new Reg(RegD));
		return null;
	}
	
	@Override
	public List<Instr> visitNegExpr(NegExprContext ctx) {
		visit(ctx.expr());
		prog.emit(Compute, new Operator(Sub), new Reg(Zero), new Reg(RegE), new Reg(RegE));
		prog.emit(Const, new Value(-1), new Reg(RegD));
		return null;
	}
	
	@Override
	public List<Instr> visitLiteralExpr(LiteralExprContext ctx) {
		visit(ctx.litExpr());
		prog.emit(Const, new Value(-1), new Reg(RegD)); // TODO: This may need to be changed when dealing what statically allocated arrays
		return null;
	}
	
	@Override
	public List<Instr> visitDeferExpr(DeferExprContext ctx) {
		int offset = checkResult.getOffset(ctx.ID());
		Reach reach = checkResult.getReach(ctx.ID());
		
//		loadIdRegDE(offset, reach);
//		prog.emit(Compute, new Operator(Add), new Reg(RegD), new Reg(Zero), new Reg(RegE));

		prog.emit(Const, new Value(offset), new Reg(RegE));
		prog.emit(Const, new Value(-1), new Reg(RegD)); // The pointer does not actually have an address
		
		if (reach == Local) {
			prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegE), new Reg(RegE));
		} else if (reach == Global) {
			prog.emit(Const, new Value(-2147483648), new Reg(RegC));
			prog.emit(Compute, new Operator(Xor), new Reg(RegC), new Reg(RegE), new Reg(RegE));
		}
		
		return null;
	}
	
	/**
	 * Dereferences an address (including address space bit) and puts the result in E. When it doubt, this needs ALL registers!
	 * @param ctx A seed for the mkLbl function.
	 */
	public void derefEinE(ParserRuleContext ctx) {
		String globalLabel = Program.mkLbl(ctx, "globalAssignment");
		String endLabel = Program.mkLbl(ctx, "endAssignment");
		
		prog.emit(Const, new Value(31), new Reg(RegC));
		prog.emit(Compute, new Operator(RShift), new Reg(RegE), new Reg(RegC), new Reg(RegC)); // C now contains -1 if it's global, 0 if it's local
		prog.emit(Compute, new Operator(Mul), new Reg(RegC), new Reg(RegC), new Reg(RegC)); // C now contains 1 if it's global, 0 if it's local
		prog.emit(Branch, new Reg(RegC), new Target(globalLabel));
		
		// It should be saved on stack
		prog.emit(Load, new MemAddr(RegE), new Reg(RegE));
		prog.emit(Jump, new Target(endLabel));
		
		// It should be saved on the heap
		prog.emit(globalLabel, Const, new Value(-2147483648), new Reg(RegB));
		prog.emit(Compute, new Operator(Xor), new Reg(RegB), new Reg(RegE), new Reg(RegE)); // RegE now contains the address without the address space bit
		prog.emit(Read, new MemAddr(RegE));
		prog.emit(Receive, new Reg(RegE));
		prog.emit(endLabel, Nop);
	}
	
	@Override
	public List<Instr> visitDerefExpr(DerefExprContext ctx) {
		visit(ctx.expr());
		prog.emit(Push, new Reg(RegE));
		derefEinE(ctx);
		prog.emit(Pop, new Reg(RegD)); // Save the address that's gonna be in E in D, ofcourse

		return null;
	}
	
	@Override
	public List<Instr> visitArrayExpr(ArrayExprContext ctx) {
		Type left = checkResult.getType(ctx.expr(0));
		Type right = checkResult.getType(ctx.expr(1));
		
		if (left instanceof Pointer && right instanceof Int) {
			visit(ctx.expr(0));
			prog.emit(Push, new Reg(RegE));
			visit(ctx.expr(1));

			Pointer ptr = (Pointer) left;
			
			prog.emit(Compute, new Operator(Add), new Reg(Zero), new Reg(RegE), new Reg(RegC));
			prog.emit(Const, new Value(ptr.pointsTo.size()), new Reg(RegB));
			prog.emit(Compute, new Operator(Mul), new Reg(RegB), new Reg(RegC), new Reg(RegC));

			prog.emit(Pop, new Reg(RegE));

			adjustPtrInEToC(ctx);
			
			prog.emit(Compute, new Operator(Add), new Reg(Zero), new Reg(RegE), new Reg(RegD));
			
			if (checkResult.getAssignable(ctx)) {
				derefEinE(ctx);
			}			
		} else if (left instanceof Array && right instanceof Int) {
			visit(ctx.expr(0));
			prog.emit(Push, new Reg(RegE));
			visit(ctx.expr(1));

			Array arr = (Array) left;
			
			prog.emit(Compute, new Operator(Add), new Reg(Zero), new Reg(RegE), new Reg(RegC));
			prog.emit(Const, new Value(arr.elemType.size()), new Reg(RegB));
			prog.emit(Compute, new Operator(Mul), new Reg(RegB), new Reg(RegC), new Reg(RegC));

			prog.emit(Pop, new Reg(RegE));

			adjustPtrInEToC(ctx);
			
			prog.emit(Compute, new Operator(Add), new Reg(Zero), new Reg(RegE), new Reg(RegD));
			
			if (checkResult.getAssignable(ctx)) {
				derefEinE(ctx);
			}
		}
		
		
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
		
		// TODO: What if an expr is an obj or array?
		// Don't forget to also support this in the checker!
		for (ExprContext expr : ctx.expr()) {
			Type type = checkResult.getType(expr);
			
//			if (type instanceof Array) {
//				Reach reach = checkResult.getReach(expr);
//				
//				if (reach == Global) {
//					prog.emit(Const, new Value(1), new Reg(RegE));
//					prog.emit(Push, new Reg(RegE));
//					prog.emit(Const, new Value(checkResult.getOffset(expr)+1), new Reg(RegE));
//					prog.emit(Push, new Reg(RegE));
//				} else if (reach == Local) {
//					prog.emit(Push, new Reg(Zero));
//					prog.emit(Const, new Value(checkResult.getOffset(expr)), new Reg(RegE));
//					prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegE), new Reg(RegE));
//					prog.emit(Push, new Reg(RegE));
//				}
//			} else {
				visit(expr);
				prog.emit(Push, new Reg(RegE));
//			}
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
		
		return null;
	}
	
//	@Override
//	public List<Instr> visitLenExpr(LenExprContext ctx) {
//		Type type = checkResult.getType(ctx.ID());
//	
//		if (type instanceof AnyArray) {
//			String stackLabel = Program.mkLbl(ctx, "stackLabel");
//			String doneLabel = Program.mkLbl(ctx, "doneLabel");
//			
//			prog.emit(Const, new Value(1), new Reg(RegD)); // D contains 1
//			prog.emit(Const, new Value(checkResult.getOffset(ctx.ID())), new Reg(RegE));
//			prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegE), new Reg(RegE)); // E contains the anyarray address
//			
//			prog.emit(Load, new MemAddr(RegE), new Reg(RegC)); // C contains whether or not it is global
//			
//			prog.emit(Compute, new Operator(Sub), new Reg(RegE), new Reg(RegD), new Reg(RegE));
//			prog.emit(Load, new MemAddr(RegE), new Reg(RegE));
//		
//			prog.emit(Compute, new Operator(Equal), new Reg(RegC), new Reg(Zero), new Reg(RegC));
//			prog.emit(Branch, new Reg(RegC), new Target(stackLabel));
//			// It's a global variable!
//			prog.emit(Compute, new Operator(Sub), new Reg(RegE), new Reg(RegD), new Reg(RegE)); // minus 1 to get at the integer that stores the size
//			prog.emit(Read, new MemAddr(RegE));
//			prog.emit(Receive, new Reg(RegE));
//			prog.emit(Jump, new Target(doneLabel));
//			
//			// It's a stack variable!
//			prog.emit(stackLabel, Compute, new Operator(Add), new Reg(RegE), new Reg(RegD), new Reg(RegE)); // add 1 to get at the integer that stores the size (it's on the stack so we need to go towards 128)
//			prog.emit(Load, new MemAddr(RegE), new Reg(RegE));
//			prog.emit(doneLabel, Nop);
//		} else if (type instanceof Array) {
//			Reach reach = checkResult.getReach(ctx.ID());
//			
//			prog.emit(Const, new Value(checkResult.getOffset(ctx.ID())), new Reg(RegE));
//			if (reach == Local) {
//				prog.emit(Compute, new Operator(Sub), new Reg(RegA), new Reg(RegE), new Reg(RegE));
//				prog.emit(Load, new MemAddr(RegE), new Reg(RegE));
//			} else if (reach == Global) {
//				prog.emit(Read, new MemAddr(RegE));
//				prog.emit(Receive, new Reg(RegE));
//			}
//		}
//		
//		return null;
//	}
	
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
		// TODO: What if return type is array or obj?
		if (!(func.func.returnType instanceof Type.Void)) {
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
		if (ctx.NOT() != null) {
			String lockLabel = Program.mkLbl(ctx, "pipeopLockStdOut");
			// Lock out operator
			// stdout lock is in the important variable segment at the second position
			prog.emit(Const, new Value(checkResult.getGlobalSize() + 1), new Reg(RegE));
			prog.emit(lockLabel, TestAndSet, new MemAddr(RegE));
			prog.emit(Receive, new Reg(RegD));
			prog.emit(Compute, new Operator(Equal), new Reg(RegD), new Reg(Zero), new Reg(RegD));
			prog.emit(Branch, new Reg(RegD), new Target(lockLabel));
		}
		
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
						prog.emit(Write, workReg, stdio);
					}
					prog.emit(Jump, new Target(end));
					
					prog.emit(equals, Nop);
					
					for (char c : "false".toCharArray()) {
						prog.emit(Const, new Value(c), workReg);
						prog.emit(Write, workReg, stdio);
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
				includePrintInt(ctx);
				
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
					prog.emit(printCharLabel, Write, new Reg(RegE), stdio);
					
					prog.emit(Pop, new Reg(RegE));
					prog.emit(Jump, new Target(RegE));
					
					leaveFunc();
				}
				
				String returnLabel = Program.mkLbl(ctx, "charreturn");

				prog.emit(Const, new Value(returnLabel), new Reg(RegD));
				prog.emit(Push, new Reg(RegD));	
				prog.emit(Jump, new Target(printCharLabel));
				prog.emit(returnLabel, Nop);
			} else if (checkResult.getType(ctx.expr()) instanceof Type.Pointer) { 
				includePrintInt(ctx);

				Type type = checkResult.getType(ctx.expr());

				if (printPointerLabel == null) {
					printPointerLabel = "pipeOp_pointer";
					
					String globalLabel = Program.mkLbl(ctx, "pipeOp_global");
					String endLabel = Program.mkLbl(ctx, "pipeOp_end");
					
					enterFunc(printPointerLabel);
					
					prog.emit(printPointerLabel, Const, new Value(31), new Reg(RegD));
					prog.emit(Compute, new Operator(RShift), new Reg(RegE), new Reg(RegD), new Reg(RegD));
					prog.emit(Compute, new Operator(Mul), new Reg(RegD), new Reg(RegD), new Reg(RegD));
					prog.emit(Branch, new Reg(RegD), new Target(globalLabel));
					
					for (char c : "local".toCharArray()) {
						prog.emit(Const, new Value(c), new Reg(RegD));
						prog.emit(Write, new Reg(RegD), stdio);
					}
					
					prog.emit(Jump, new Target(endLabel));
					
					prog.emit(globalLabel, Nop);
					
					for (char c : "global".toCharArray()) {
						prog.emit(Const, new Value(c), new Reg(RegD));
						prog.emit(Write, new Reg(RegD), stdio);
					}

					prog.emit(Const, new Value(-2147483648), new Reg(RegD));
					prog.emit(Compute, new Operator(Xor), new Reg(RegE), new Reg(RegD), new Reg(RegE));
					
					String loopLabel = Program.mkLbl(ctx, "loopLabel");
					prog.emit(endLabel, Pop, new Reg(RegB));
					prog.emit(Const, new Value("_".toCharArray()[0]), new Reg(RegC));
					prog.emit(loopLabel, Const, new Value(1), new Reg(RegD));
					prog.emit(Write, new Reg(RegC), stdio);
					prog.emit(Compute, new Operator(Sub), new Reg(RegB), new Reg(RegD), new Reg(RegB));
					prog.emit(Compute, new Operator(NEq), new Reg(RegB), new Reg(Zero), new Reg(RegD));
					prog.emit(Branch, new Reg(RegD), new Target(loopLabel));
					
					prog.emit(Jump, new Target(printIntLabel));
					
					leaveFunc();
				}
				
				String returnLabel = Program.mkLbl(ctx, "printPointerReturn");
				prog.emit(Const, new Value(returnLabel), new Reg(RegC));
				prog.emit(Push, new Reg(RegC));	
				prog.emit(Const, new Value(((Pointer) type).getDepth()), new Reg(RegC));
				prog.emit(Push, new Reg(RegC));
				prog.emit(Jump, new Target(printPointerLabel));
				prog.emit(returnLabel, Nop);
			}else{
				System.out.println("Unsupported type given to stdout " + ctx.getText());
				return null;
			}
		}
		
		Reg workReg = new Reg(RegE);
		if (ctx.newlines().getChildCount() > 0) prog.emit(Const, new Value((int) "\n".toCharArray()[0]), workReg);
		for (int i = 0; i < ctx.newlines().getChildCount(); i++) {
			prog.emit(Write, workReg, stdio);
		}
		
		if (ctx.NOT() != null) {
			prog.emit(Const, new Value(checkResult.getGlobalSize() + 1), new Reg(RegE));
			prog.emit(Write, new Reg(Zero), new MemAddr(RegE));
		}
		
		return null;
	}
	
	public void includePrintInt(ParserRuleContext ctx) {
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
			
			// Print minus if it's negative
			prog.emit(Compute, new Operator(LtE), new Reg(Zero), numReg, workReg);
			prog.emit(Branch, workReg, new Target(begin));
			prog.emit(Const, new Value("-".charAt(0)), workReg);
			prog.emit(Write, workReg, stdio);
			prog.emit(Compute, new Operator(Sub), new Reg(Zero), numReg, numReg);
			
			// Take modulo of current value and save it on the stack
			prog.emit(begin, Compute, mod, numReg, orderReg, workReg);
			prog.emit(Push, workReg);
//			prog.emit(Const, new Value(100), workReg);
//			prog.emit(Write, workReg, stdio);
			
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
//					prog.emit(Write, workReg, new MemAddr("stdio"));
			prog.emit(Write, workReg, stdio);
			
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
	}
	
	@Override
	public List<Instr> visitStartStat(StartStatContext ctx) {
		Function.Overload func = checkResult.getFunction(ctx);
		
		prog.emit(Const, new Value(checkResult.getSprockellSegment()), new Reg(RegE));
		prog.emit(Const, new Value(func.sprockell * sprockellSegmentSize), new Reg(RegD));
		prog.emit(Compute, new Operator(Add), new Reg(RegE), new Reg(RegD), new Reg(RegE));
		prog.emit(Const, new Value(func.label), new Reg(RegD));
		prog.emit(Write, new Reg(RegD), new MemAddr(RegE));

		return null;
	}

	private void includeIdleFunc() {
		if (idleFuncLabel == null) {
			idleFuncLabel = "idleFunc";
			
			enterFunc(idleFuncLabel);
			
			prog.emit(idleFuncLabel, Const, new Value(checkResult.getSprockellSegment()), new Reg(RegE));
			prog.emit(Const, new Value(sprockellSegmentSize), new Reg(RegD));
			prog.emit(Compute, new Operator(Mul), new Reg(RegD), new Reg(SPID), new Reg(RegD));
			prog.emit(Compute, new Operator(Add), new Reg(RegD), new Reg(RegE), new Reg(RegE));
			prog.emit(Read, new MemAddr(RegE));
			prog.emit(Receive, new Reg(RegD));
			prog.emit(Compute, new Operator(Equal), new Reg(RegD), new Reg(Zero), new Reg(RegE));
			prog.emit(Branch, new Reg(RegE), new Target(idleFuncLabel));
			
			// Start making the final thread stack etc. here
			// First construct the "fake" stack for program()
			prog.emit(Push, new Reg(Zero)); // Return address
			prog.emit(Push, new Reg(Zero)); // RegA
			prog.emit(Push, new Reg(Zero)); // RegB
			prog.emit(Push, new Reg(Zero)); // RegC
			prog.emit(Push, new Reg(Zero)); // RegD
			prog.emit(Push, new Reg(Zero)); // RegE
			prog.emit(Const, new Value(endLabel), new Reg(RegE));
			prog.emit(Push, new Reg(RegE)); // Return address
			// We push a zero here to let the ARP point to the FIRST byte after the last parameter
			// (or the return value of there were no parameters)
			prog.emit(Push, new Reg(Zero));
			prog.emit(Compute, new Operator(Add), new Reg(SP), new Reg(Zero), new Reg(RegA)); // Set the initial ARP
			
			prog.emit(Jump, new Target(RegD));
			
			leaveFunc();
		}
	}
}
