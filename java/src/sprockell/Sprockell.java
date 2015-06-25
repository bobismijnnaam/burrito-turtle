package sprockell;

import static sprockell.Operand.Type.*;
import lang.BurritoLexer;
import lang.BurritoParser;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import sprockell.Operand.Type;

import comp.Checker;
import comp.ErrorListener;
import comp.Generator;
import comp.ParseException;
import comp.Result;

public class Sprockell {
	
	public enum Op {
		/** Operator, Reg, Reg => Reg */
		Compute(2, OPERATOR, REG, REG, REG),
		/** Value => Reg */
		Const(1, VALUE, REG),
		/** Reg, Target */
		Branch(1, REG, TARGET),
		/** -> Target */
		Jump(0, TARGET),
		/** MemAddr => Reg */
		Load(1, MEMADDR, REG),
		/** Reg => MemAddr */
		Store(1, REG, MEMADDR),
		/** Reg */
		Push(1, REG),
		/** => Reg */
		Pop(0, REG),
		/** MemAddr */
		Read(1, MEMADDR),
		/** => Reg */
		Receive(0, REG),
		/** Reg => MemAddr */
		Write(1, REG, MEMADDR),
		/** MemAddr */
		TestAndSet(1, MEMADDR),
		/** End of program */
		EndProg(0),
		/** Nop */
		Nop(0),
		/** String*/
		Debug(1, STRING);
		
		final public int arguments;
		final public Type[] sig;

		private Op(int arguments, Operand.Type... sig) {
			this.arguments = arguments;
			this.sig = sig;
		}
	}
	
	public static Program scaryCompile(String progStr) {
		CharStream input = new ANTLRInputStream(progStr);
		
		ErrorListener listener = new ErrorListener();

		Lexer lexer = new BurritoLexer(input);
		lexer.removeErrorListeners();
		lexer.addErrorListener(listener);

		TokenStream tokens = new CommonTokenStream(lexer);
		BurritoParser parser = new BurritoParser(tokens);
		ParseTree result = parser.program();
		parser.removeErrorListeners();
		parser.addErrorListener(listener);
		
		Checker checker = new Checker();
		Generator generator = new Generator();

		try {
			Result checkResult = checker.check(result);
			Program prog = generator.generate(result, checkResult);

			return prog;
		} catch (ParseException p) {
			p.printStackTrace();
		}
		
		return null;
	}
	
	public static Program compile(String input) {
		return compile(new ANTLRInputStream(input));
	}
	
	public static Program compile(ANTLRInputStream input) {
		ErrorListener listener = new ErrorListener();

		Lexer lexer = new BurritoLexer(input);
		lexer.removeErrorListeners();
		lexer.addErrorListener(listener);

		TokenStream tokens = new CommonTokenStream(lexer);
		BurritoParser parser = new BurritoParser(tokens);
		ParseTree result = parser.program();
		parser.removeErrorListeners();
		parser.addErrorListener(listener);
		
		Checker checker = new Checker();
		Generator generator = new Generator();

		try {
			Result checkResult = checker.check(result);
			Program prog = generator.generate(result, checkResult);

			// If errors, an exception will be thrown
			listener.throwException();

			if (!prog.isWellFormed()) {
				System.out.println("Program is not well formed");
				return null;
			}
			
			if (!prog.fixLabels()) {
				System.out.println("There were missing labels");
				return null;
			}
			
			return prog;
		} catch (ParseException e) {
			e.printStackTrace();
			System.out.println("Something went wrong");
			return null;
		}
	}
}


