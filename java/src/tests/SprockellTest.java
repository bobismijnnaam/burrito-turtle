package tests;

import static org.junit.Assert.*;
import static sprockell.Operator.Which.*;
import static sprockell.Reg.Which.*;
import static sprockell.Sprockell.Op.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
	
	private final static String BASE_DIR = "src/tests/testfiles";
	private final static String EXT = ".symbol";
	
	@Test
	public void simplePipe() {
		String output = "123450987654321-98765432-123459990truefalse";
		String result = compileAndRunFile("SimplePipe");
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(output, result);
	}
	
	@Test
	public void simpleArithmatic() {
		String output = "000341223421falsefalsefalsetruefalsetrue";
		String result = compileAndRunFile("SimpleArithmatic");
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(output, result);
	}
	
	@Test
	public void simpleWhile() {
		String output = "0123456789";
		String result = compileAndRunFile("SimpleWhile");
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(output, result);
	}
	
	@Test
	public void simpleIf() {
		String output = "truefalse";
		String result = compileAndRunFile("SimpleIf");
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(output, result);
	}
	
	@Test
	public void simpleOutBackslash() {
		String output = "44\n5\n\n5\n\n\n0";
		String result = compileAndRunFile("SimpleOutBackSlash");
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(output, result.replaceAll("\r\n", "\n"));
	}
	
	@Test
	public void simpleArray() {
		String result = compileAndRunFile("SimpleArray");
		String output = "0123456789101112131415161718192021222324252627282930313233343536373839404142434445464748495051525354555657585960616263646566676869707172737475767778798081828384858687888990919293949596979899";
		//String output = "3";
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(output, result.replaceAll("\r\n", "\n"));
	}
	
	@Test
	public void multiDimArray() {
		String result = compileAndRunFile("MultiDimArray");
		
		String output = "";
		int[][][][] x = new int[2][2][2][2];
		for (int a = 0; a < 2; a++) {
			for (int b = 0; b < 2; b++) {
				for (int c = 0; c < 2; c++) {
					for (int d = 0; d < 2; d++) {
						x[a][b][c][d] = a + b + c + d;
						output += x[a][b][c][d];
					}
				}
			}
		}
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(output, result.replaceAll("\r\n", "\n"));
	}
	
	@Test
	public void comments() {
		String result = compileAndRunFile("Comments");
		assertNotNull("Compiling or executing went wrong", result);
	}
	
	public static String compileAndRun(String progStr) {
		return compileAndRun(new ANTLRInputStream(progStr));
	}
	
	public static String compileAndRunFile(String filename) { 
		try {
			return compileAndRun(new ANTLRInputStream(new FileReader(new File(BASE_DIR, filename + EXT))));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String compileAndRun(ANTLRInputStream input) {
		Program prog = Sprockell.compile(input);
		
		System.out.println(prog.prettyString(0, true));
		
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
