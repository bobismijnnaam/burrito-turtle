package tests;

import static org.junit.Assert.*;
import lang.BurritoLexer;
import lang.BurritoParser;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

import comp.Checker;
import comp.Collector;
import comp.ParseException;
import comp.Result;
import comp.Scope;
import comp.Type;
 
public class CheckerTest {
	
	@Test
	public void arrayTypeTests() {
		String testProgram = "void program() int[6]_ a; int[7] b; int[5][2] c; int[3][2][5][6] d; int[2][3][5][7] e; <-; .";

		ParseTree result = parse(testProgram);
		Checker checker = new Checker();
		Collector collector = new Collector();
		
		try {
			Scope scope = collector.generate(result);
			checker.check(result, scope);
		} catch (ParseException e) {
			System.out.println(e);
		}
	}
	
	@Test
	public void functionTests() {
		String testProgram = "int add(int a, int b) <- a + b;. void program(bool x, bool y) int c = 3; int d = 4; <- ; . ";
		ParseTree result = parse(testProgram);
		Checker checker = new Checker();
		Collector collector = new Collector();
		
		try {
			Scope scope = collector.generate(result);
			checker.check(result, scope);
		} catch (ParseException e) {
		}

		testProgram = "int add(int a, int b) <- a + b; . void program() int a = add(2, 3); <-; .";
		result = parse(testProgram);
		checker = new Checker();
		collector = new Collector();
		
		try {
			Scope scope = collector.generate(result);
			checker.check(result, scope);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void basicTests() {
		// int assignment
		String testProgram = "void program() int i = 0; <-; .";
		ParseTree result = parse(testProgram);
		Collector collector = new Collector();
		Checker checker = new Checker();

		try {
			Scope scope = collector.generate(result);
			Result checkResult = checker.check(result, scope);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(1).getChild(1)).getClass());
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(1).getChild(3)).getClass());
		} catch (ParseException e) {
		}
		
		assertEquals(false, checker.hasErrors());
		
		// int assignment but not inferred has 1 error
		testProgram = "void program() i = 0; <-; .";
		result = parse(testProgram);
		checker = new Checker();
		collector = new Collector();
		
		try {
			Scope scope = collector.generate(result);
			Result checkResult = checker.check(result, scope);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(1)).getClass());
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(3)).getClass());
		} catch (ParseException e) {
		}
		
		assertEquals(true, checker.hasErrors());
		
		// try to assign bool to int
		testProgram = "void program() int i = false; <-; .";
		result = parse(testProgram);
		checker = new Checker();
		collector = new Collector();
		
		try {
			Scope scope = collector.generate(result);
			Result checkResult = checker.check(result, scope);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(1)).getClass());
			assertEquals(new Type.Bool().getClass(), checkResult.getType(result.getChild(0).getChild(3)).getClass());
		} catch (ParseException e) {
		}
		
		assertEquals(true, checker.hasErrors());
		
	}
	
	@Test
	public void arrayTests() { 
		// correct array useage, type int and assign correct type to elem
		String testProgram = "void program() bool[6] i; i[4] = true; <-; .";
		ParseTree result = parse(testProgram);
		Checker checker = new Checker();
		Collector collector = new Collector();
		try {
			Scope scope = collector.generate(result);
			Result checkResult = checker.check(result, scope);
			
			assertEquals(new Type.Array(new Type.Bool(), 6).getClass(), checkResult.getType(result.getChild(0).getChild(1).getChild(0)).getClass());
			assertEquals(new Type.Bool().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(0)).getClass());
			assertEquals(new Type.Bool().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(2)).getClass());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertEquals(false, checker.hasErrors());
		
		// assign wrong type to elem
		testProgram = "void program() int[6] i; i[4] = true; <-; .";
		result = parse(testProgram);
		checker = new Checker();
		collector = new Collector();
		try {
			Scope scope = collector.generate(result);
			Result checkResult = checker.check(result, scope);
			assertEquals(new Type.Array(null, 0).getClass(), checkResult.getType(result.getChild(0).getChild(1).getChild(0)).getClass());
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(0)).getClass());
			assertEquals(new Type.Bool().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(2)).getClass());
		} catch (ParseException e) {
			
		}
		
		// array not initialized
		testProgram = "void program() i[1] = true; <-; .";
		result = parse(testProgram);
		checker = new Checker();
		collector = new Collector();
		try {
			Scope scope = collector.generate(result);
			checker.check(result, scope);
		} catch (ParseException e) {
			
		}
		
		assertEquals(true, checker.hasErrors());
		
		// array expr not a num initialized
		testProgram = "int program() bool[5] i; i[false] = true; .";
		result = parse(testProgram);
		checker = new Checker();
		collector = new Collector();
		try {
			Scope scope = collector.generate(result);
			checker.check(result, scope);
		} catch (ParseException e) {
		}
		
		assertEquals(true, checker.hasErrors());
		
		// array expr not a num as index
		testProgram = "int program() bool[7] x; x[0] = true; x[false]|; int z = 0; int[5] y; int p = 0; .";
		//testProgram = "bool[7][8] x; x[0][0] = 0;";
		result = parse(testProgram);
		checker = new Checker();
		collector = new Collector();
		try {
			Scope scope = collector.generate(result);
			checker.check(result, scope);
		} catch (ParseException e) {
		}
		
		assertEquals(true, checker.hasErrors());
	}
	
	@Test public void multiDimArrayTest() {
		String testProgram = "void program() bool[3][2][6][2][3] i; int x = 0; <-; .";
		ParseTree result = parse(testProgram);
		Checker checker = new Checker();
		Collector collector = new Collector();

		try {
			Scope scope = collector.generate(result);
			Result checkResult = checker.check(result, scope);
			assertEquals(216, checkResult.getOffset(result.getChild(0).getChild(2).getChild(1)));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		for (String error : checker.getErrors()) {
			System.out.println(error);
		}
	}
	
	@Test
	public void incExprTest() {
		String testProgram = "void program() int x = 0; x+++++++++; <- ;.";
		ParseTree result = parse(testProgram);
		Checker checker = new Checker();
		Collector collector = new Collector();

		try {
			Scope scope = collector.generate(result);
			Result checkResult = checker.check(result, scope);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(0)).getClass());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertEquals(false, checker.hasErrors());
	}
	
	@Test
	public void decExprTest() {
		String testProgram = "void program() int x = 0; x----------; <-; .";
		ParseTree result = parse(testProgram);
		Checker checker = new Checker();
		Collector collector = new Collector();

		try {
			Scope scope = collector.generate(result);
			Result checkResult = checker.check(result, scope);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(0)).getClass());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertEquals(false, checker.hasErrors());
	}
	
	@Test
	public void switchTest() {
		String testProgram = "void program() int x = 0; <x> 1: x = 3; . <-;.";
		ParseTree result = parse(testProgram);
		Collector funCol = new Collector();
		Checker checker = new Checker();

		try {
			Scope scope = funCol.generate(result);
			 checker.check(result, scope);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertEquals(false, checker.hasErrors());
		
		// switch id not initialized
		testProgram = "void program() <x> 1: x = 2; . <-; .";
		result = parse(testProgram);
		checker = new Checker();
		funCol = new Collector();
		try {
			Scope scope = funCol.generate(result);
			checker.check(result, scope);
		} catch (ParseException e) {
		}
		
		assertEquals(true, checker.hasErrors());
	}
	
	@Test
	public void imporTest() {
		String testProgram = "pls \"ruben\"; void program() <-; .";
		ParseTree result = parse(testProgram);
		Collector funCol = new Collector();
		Checker checker = new Checker();

		try {
			Scope scope = funCol.generate(result);
			checker.check(result, scope);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertEquals(false, checker.hasErrors());
	}
	
	@Test
	public void operatorAssignTests() {
		String testProgram = "void program() int x = 0; x += 3; <-; .";
		ParseTree result = parse(testProgram);
		Checker checker = new Checker();
		Collector collector = new Collector();

		try {
			Scope scope = collector.generate(result);
			Result checkResult = checker.check(result, scope);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(0)).getClass());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertEquals(false, checker.hasErrors());
		
		testProgram = "void program() int x = 0; x -= 3; <-; .";
		result = parse(testProgram);
		checker = new Checker();
		collector = new Collector();

		try {
			Scope scope = collector.generate(result);
			Result checkResult = checker.check(result, scope);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(0)).getClass());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertEquals(false, checker.hasErrors());
		
		testProgram = "void program() int x = 0; x /= 3; <-; .";
		result = parse(testProgram);
		checker = new Checker();
		collector = new Collector();

		try {
			Scope scope = collector.generate(result);
			Result checkResult = checker.check(result, scope);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(0)).getClass());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertEquals(false, checker.hasErrors());
		
		testProgram = "void program() int x = 0; x *= 3; <-; .";
		result = parse(testProgram);
		checker = new Checker();
		collector = new Collector();

		try {
			Scope scope = collector.generate(result);
			Result checkResult = checker.check(result, scope);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(0)).getClass());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertEquals(false, checker.hasErrors());
	}
	
	@Test
	public void charTest() {
		String testProgram = "void program() char a = 'a'; <-; .";
		ParseTree result = parse(testProgram);
		Checker checker = new Checker();
		Collector collector = new Collector();
		try {
			Scope scope = collector.generate(result);
			Result checkResult = checker.check(result, scope);
			assertEquals(new Type.Char().getClass(), checkResult.getType(result.getChild(0).getChild(1).getChild(1)).getClass());
			assertEquals(new Type.Char().getClass(), checkResult.getType(result.getChild(0).getChild(1).getChild(3)).getClass());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertEquals(false, checker.hasErrors());
	}
	
	private ParseTree parse(String testProgram) {
		CharStream input = new ANTLRInputStream(testProgram);
		Lexer lexer = new BurritoLexer(input);
		TokenStream tokens = new CommonTokenStream(lexer);
		BurritoParser parser = new BurritoParser(tokens);
		return parser.program();
	}
}
