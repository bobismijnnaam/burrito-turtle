package tests;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;

import lang.BurritoLexer;
import lang.BurritoParser;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

import sprockell.Program;
import comp.Checker;
import comp.Generator;
import comp.ParseException;
import comp.Result;
import comp.Type;
 
public class CheckerTest {
	
	@Test
	public void functionTests() {
		String testProgram = "int add(int a, int b) <- a + b;. int program(bool x, bool y) int c = 3; int d = 4; <- add(c, d); . ";
		ParseTree result = parse(testProgram);
		Checker checker = new Checker();
		
		try {
			Result checkResult = checker.check(result);
			//System.out.println(result.getChild(1).getChild(3).getChild(1).getText());
			//System.out.println(checkResult.getType(result.getChild(1).getChild(3).getChild(1)));
		} catch (ParseException e) {
		}

//		testProgram = "int add(int a, int b) <- a + b;. int program(bool x, bool y) <- add(x, y); .";
//		result = parse(testProgram);
//		checker = new Checker();
//		
//		try {
//			Result checkResult = checker.check(result);
//			System.out.println(result.getChild(1).getChild(1).getChild(2).getChild(1).getText());
//			System.out.println(checkResult.getType(result.getChild(1).getChild(1).getChild(2).getChild(1)));
//		} catch (ParseException e) {
//			e.printStackTrace();
//		}
//		
//		System.out.println("Done");
	}
	
	@Test
	public void basicTests() {
		// int assignment
		String testProgram = "int program() int i = 0; .";
		ParseTree result = parse(testProgram);
		Checker checker = new Checker();

		try {
			Result checkResult = checker.check(result);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(1).getChild(1)).getClass());
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(1).getChild(3)).getClass());
		} catch (ParseException e) {
		}
		
		assertEquals(false, checker.hasErrors());
		
		// int assignment but not inferred has 1 error
		testProgram = "int program() i = 0; .";
		result = parse(testProgram);
		checker = new Checker();
		
		try {
			Result checkResult = checker.check(result);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(1)).getClass());
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(3)).getClass());
		} catch (ParseException e) {
		}
		
		assertEquals(true, checker.hasErrors());
		
		// try to assign bool to int
		testProgram = "int program() int i = false; .";
		result = parse(testProgram);
		checker = new Checker();
		
		try {
			Result checkResult = checker.check(result);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(1)).getClass());
			assertEquals(new Type.Bool().getClass(), checkResult.getType(result.getChild(0).getChild(3)).getClass());
		} catch (ParseException e) {
		}
		
		assertEquals(true, checker.hasErrors());
		
	}
	
	@Test
	public void arrayTests() { 
		// correct array useage, type int and assign correct type to elem
		String testProgram = "int program() bool[6] i; i[4] = true; .";
		ParseTree result = parse(testProgram);
		Checker checker = new Checker();
		try {
			Result checkResult = checker.check(result);
			
			assertEquals(new Type.Array(new Type.Bool(), 6).getClass(), checkResult.getType(result.getChild(0).getChild(1).getChild(0)).getClass());
			assertEquals(new Type.Bool().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(0)).getClass());
			assertEquals(new Type.Bool().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(2)).getClass());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertEquals(false, checker.hasErrors());
		
		// assign wrong type to elem
		testProgram = "int program() int[6] i; i[4] = true; .";
		result = parse(testProgram);
		checker = new Checker();
		try {
			Result checkResult = checker.check(result);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(1)).getClass());
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(1).getChild(0)).getClass());
			assertEquals(new Type.Bool().getClass(), checkResult.getType(result.getChild(1).getChild(2)).getClass());
		} catch (ParseException e) {
			
		}
		
		assertEquals(true, checker.hasErrors());
		
		// array not initialized
		testProgram = "int program() i[1] = true; .";
		result = parse(testProgram);
		checker = new Checker();
		try {
			Result checkResult = checker.check(result);
		} catch (ParseException e) {
			
		}
		
		assertEquals(true, checker.hasErrors());
		
		// array expr not a num initialized
		testProgram = "int program() bool[5] i; i[false] = true; .";
		result = parse(testProgram);
		checker = new Checker();
		try {
			Result checkResult = checker.check(result);
		} catch (ParseException e) {
		}
		
		assertEquals(true, checker.hasErrors());
		
		// array expr not a num as index
		testProgram = "int program() bool[7] x; x[0] = true; x[false]|; int z = 0; int[5] y; int p = 0; .";
		//testProgram = "bool[7][8] x; x[0][0] = 0;";
		result = parse(testProgram);
		checker = new Checker();
		try {
			Result checkResult = checker.check(result);
		} catch (ParseException e) {
		}
		
		assertEquals(true, checker.hasErrors());
	}
	
	@Test public void multiDimArrayTest() {
		String testProgram = "int program() bool[3][2][6][2][3] i; int x = 0; .";
		ParseTree result = parse(testProgram);
		Checker checker = new Checker();

		try {
			Result checkResult = checker.check(result);
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
		String testProgram = "int program() int x = 0; x+++++++++; .";
		ParseTree result = parse(testProgram);
		Checker checker = new Checker();

		try {
			Result checkResult = checker.check(result);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(0)).getClass());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertEquals(false, checker.hasErrors());
	}
	
	@Test
	public void decExprTest() {
		String testProgram = "int program() int x = 0; x----------; .";
		ParseTree result = parse(testProgram);
		Checker checker = new Checker();

		try {
			Result checkResult = checker.check(result);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(0)).getClass());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertEquals(false, checker.hasErrors());
	}
	
	@Test
	public void operatorAssignTests() {
		String testProgram = "int program() int x = 0; x += 3; .";
		ParseTree result = parse(testProgram);
		Checker checker = new Checker();

		try {
			Result checkResult = checker.check(result);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(0)).getClass());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertEquals(false, checker.hasErrors());
		
		testProgram = "int program() int x = 0; x -= 3; .";
		result = parse(testProgram);
		checker = new Checker();

		try {
			Result checkResult = checker.check(result);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(0)).getClass());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertEquals(false, checker.hasErrors());
		
		testProgram = "int program() int x = 0; x /= 3; .";
		result = parse(testProgram);
		checker = new Checker();

		try {
			Result checkResult = checker.check(result);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(0)).getClass());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertEquals(false, checker.hasErrors());
		
		testProgram = "int program() int x = 0; x *= 3; .";
		result = parse(testProgram);
		checker = new Checker();

		try {
			Result checkResult = checker.check(result);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(2).getChild(0)).getClass());
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
