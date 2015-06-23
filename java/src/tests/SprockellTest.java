package tests;

import static org.junit.Assert.*;
import static sprockell.Operator.Which.*;
import static sprockell.Reg.Which.*;
import static sprockell.Sprockell.Op.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

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
import sprockell.Sprockell;
import sprockell.Target;
import sprockell.Value;
import comp.Checker;
import comp.Generator;
import comp.ParseException;
import comp.Result;

public class SprockellTest {
	
	@Test
	public void simplePipe() {
		String testProgram = "12345|; 0|; 987654321|; -98765432|; -12345999|; -0|; true|; false|;";
		String output = "123450987654321-98765432-123459990truefalse";
		
		String result = compileAndRun(testProgram);
		
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(output, result);
	}
	
	@Test
	public void simpleArithmatic() {
		String testProgram = ""
			+ "int x = 0;"
			+ "int y = 0;"
			+ "int z = 0;"
			+ "x|;"
			+ "y|;"
			+ "z|;"
			+ "x = 3; y = 4; z = x * y;"
			+ "x|;"
			+ "y|;"
			+ "z|;"
			+ "x = 23; y = 42; z = y / x;"
			+ "x|;"
			+ "y|;"
			+ "z|;"
			;
		String output = "000341223421";
		
		String result = compileAndRun(testProgram);
		
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(output, result);
	}
	
	@Test
	public void simpleWhile() {
		String testProgram = ""
			+ "int i = 0;"
			+ "i < 10 @"
			+ "	i|;"
			+ "	i = i + 1;"
			+ "."
			;
		String output = "0123456789";
		
		String result = compileAndRun(testProgram);
		
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(output, result);
	}
	
	@Test
	public void simpleIf() {
		String testProgram = ""
			+ "int a = 4;"
			+ "int b = 3;"
			+ "a > b ?"
			+ "	true|;"
			+ "	b < 1 ?"
			+ "		true|;"
			+ "	!"
			+ "		false|;"
			+ "	."
			+ "!"
			+ "	false|;"
			+ "."
			;
		String output = "truefalse";
		
		String result = compileAndRun(testProgram);
		
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(output, result);
	}
	
	@Test
	public void simpleDoublePipe() {
		String testProgram = ""
			+ "int a = 4;"
			+ "int b = 5;"
			+ "a|;"
			+ "a|\\;"
			+ "b|\\\\;"
			+ "b|\\\\\\;"
			+ "0|;"
			;
		String output = "44\n5\n\n5\n\n\n0";

		String result = compileAndRun(testProgram);
		
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(output, result.replaceAll("\r\n", "\n"));
	}
	
	@Test
	public void comments() {
		String[] testPrograms = {"-- A comment",
		"int a = 3; -- Another comment",
		"int a = 3;\n-- Yet another comment",
		"{A weird comment}int a{Everywhere} = 5;{We don't mind them}",
		"-- They can be combined as well\nint a {See?} = 5; -- No worries!"};
		
		for (String s : testPrograms) {
			String result = compileAndRun(s);
			assertNotNull("Test went wrong:\n" + s, result);
		}
	}
	
	public static String compileAndRun(String progStr) {
		Program prog = Sprockell.compile(progStr);
		
		if (prog == null) {
			System.out.println("There were errors");
			return null;
		}
		
		try {
			prog.writeToFile("test.hs");
			
			Runtime rt = Runtime.getRuntime();
			Process buildPr = rt.exec("ghc -i../sprockell/src -e main test.hs");
			buildPr.waitFor();
			
			InputStream is = buildPr.getInputStream();
			
			Scanner s = new Scanner(is).useDelimiter("\\A");
		    return s.hasNext() ? s.next() : "";
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("File not found");
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("IO Exception");
			return null;
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.out.println("We got interrupted");
			return null;
		} catch (NoSuchElementException e) {
			e.printStackTrace();
			System.out.println("Stream was empty");
			return null;
		}
	}
}
