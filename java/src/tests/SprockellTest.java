package tests;

import static org.junit.Assert.*;
import static sprockell.Operator.Which.*;
import static sprockell.Reg.Which.*;
import static sprockell.Sprockell.Op.*;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import lang.BurritoLexer;
import lang.BurritoParser;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

import sprockell.MemAddr;
import sprockell.Operator;
import sprockell.Program;
import sprockell.Reg;
import sprockell.Target;
import sprockell.Value;
import comp.Checker;
import comp.Generator;
import comp.ParseException;
import comp.Result;

public class SprockellTest {
	
	@Test
	public void sprockellTest() {
		Program prog = new Program();
		prog.emit(Const, new Value(2), new Reg(RegA));
		prog.emit(Const, new Value(3), new Reg(RegB));
		prog.emit("Ruben's Dan examen!!", Compute, new Operator(Add), new Reg(RegA), new Reg(RegB), new Reg(RegC));
		prog.emit(Jump, new Target("Ruben's Dan examen!!"));
		prog.emit(Write, new Reg(RegC), new MemAddr("stdio"));
		prog.emit(EndProg);
		
		assertTrue(prog.isWellFormed());
		
		System.out.println("Before subsitution: ");
		System.out.println(prog.prettyString(0, true));
		System.out.println("After subsitution");
		prog.fixLabels();
		System.out.println(prog.prettyString(0, false));
	}
	
	@Test
	public void generatorTest() {
		String testProgram = "int a = 1; int b = 2; int c = 1; c = a + b; c < 3 ? c = 3; ! c = c * 2; .";

		CharStream input = new ANTLRInputStream(testProgram);
		
//		ErrorListener listener = new ErrorListener();
		Lexer lexer = new BurritoLexer(input);
//		lexer.removeErrorListeners();
//		lexer.addErrorListener(listener);
		TokenStream tokens = new CommonTokenStream(lexer);
		BurritoParser parser = new BurritoParser(tokens);
//		parser.removeErrorListeners();
//		parser.addErrorListener(listener);
		ParseTree result = parser.program();
//		listener.throwException();
		
		Checker checker = new Checker();
		Generator generator = new Generator();
		try {
			Result checkResult = checker.check(result);
			Program prog = generator.generate(result, checkResult);
			System.out.println("Compiled program: \n");
			System.out.println(prog.prettyString(0, true));
			System.out.println("\nLabels fixed: \n");
			prog.fixLabels();
			System.out.println(prog.prettyString(0, true));
			prog.writeToFile("simpleTest.hs");
		} catch (ParseException e) {
			e.printStackTrace();
			System.out.println("Something went wrong");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
