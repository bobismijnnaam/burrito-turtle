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
	public void basicTests() {
		// int assignment
		String testProgram = "int i = 0;";
		ParseTree result = parse(testProgram);
		Checker checker = new Checker();
		
		try {
			Result checkResult = checker.check(result);
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(1)).getClass());
			assertEquals(new Type.Int().getClass(), checkResult.getType(result.getChild(0).getChild(3)).getClass());
		} catch (ParseException e) {
		}
		
		assertEquals(false, checker.hasErrors());
		
		// int assignment but not inferred has 1 error
		testProgram = "i = 0;";
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
		testProgram = "int i = false;";
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
		String testProgram = "bool[6] i; i[4] = true;";
		ParseTree result = parse(testProgram);
		Checker checker = new Checker();
		try {
			Result checkResult = checker.check(result);
			assertEquals(new Type.Array(new Type.Bool(), 6).getClass(), checkResult.getType(result.getChild(0).getChild(1)).getClass());
			assertEquals(new Type.Array(new Type.Bool(), 6).getClass(), checkResult.getType(result.getChild(1).getChild(0)).getClass());
			assertEquals(new Type.Bool().getClass(), checkResult.getType(result.getChild(1).getChild(2)).getClass());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertEquals(false, checker.hasErrors());
		
		// assign wrong type to elem
		testProgram = "int[6] i; i[4] = true;";
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
		testProgram = "i[1] = true;";
		result = parse(testProgram);
		checker = new Checker();
		try {
			Result checkResult = checker.check(result);
		} catch (ParseException e) {
			
		}
		
		assertEquals(true, checker.hasErrors());
		
		// array expr not a num initialized
		testProgram = "bool[5] i; i[false] = true;";
		result = parse(testProgram);
		checker = new Checker();
		try {
			Result checkResult = checker.check(result);
		} catch (ParseException e) {
		}
		
		assertEquals(true, checker.hasErrors());
		
		// array expr not a num initialized
		testProgram = "bool[7] x; x[5+false]|; int z = 0; int[5] y; int p = 0;";
		result = parse(testProgram);
		checker = new Checker();
		try {
			Result checkResult = checker.check(result);
			System.out.println("Bool[5] x: "+ checkResult.getOffset(result.getChild(0).getChild(1)));
			System.out.println("x: "+ checkResult.getOffset(result.getChild(1).getChild(0)));
			System.out.println("Int z: "+ checkResult.getOffset(result.getChild(2).getChild(1)));
			System.out.println("Int[5] y: "+ checkResult.getOffset(result.getChild(3).getChild(1)));
			System.out.println("Int p: "+ checkResult.getOffset(result.getChild(4).getChild(1)));
		} catch (ParseException e) {
		}
		
		for (String error : checker.getErrors()) {
			System.out.println(error);
		}
	}
	
	private ParseTree parse(String testProgram) {
		CharStream input = new ANTLRInputStream(testProgram);
		Lexer lexer = new BurritoLexer(input);
		TokenStream tokens = new CommonTokenStream(lexer);
		BurritoParser parser = new BurritoParser(tokens);
		return parser.program();
	}
}
