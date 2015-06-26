package tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.junit.Test;

import sprockell.Program;
import sprockell.Sprockell;

public class SprockellTest { 
	
	private final static String BASE_DIR = "src/tests/testfiles";
	private final static String EXT = ".symbol";
	
	@Test
	public void simpleGlobals() {
		String result = compileAndRunFile("SimpleGlobals");
		System.out.println(result);
	}

	@Test
	public void fib() {
		String result = compileAndRunFile("Fib");
		String output = "0\n1\n3\n8\n21\n55\n";
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(output, result.replaceAll("\r\n", "\n"));
	}
	
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
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(output, result.replaceAll("\r\n", "\n"));
	}
	
	@Test
	public void simpleOverloading() {
		String result = compileAndRunFile("SimpleOverloading");
		String output = "3\n1\n25\n";
		assertNotNull("Compiling or executing went wrong", result);
		assertSanitized(output, result);
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
	
	@Test
	public void simpleIncExpr() {
		String result = compileAndRunFile("SimpleIncExpr");
		int z = 0;
		for (int i = 0; i < 11; i++) {
			z++;
		}
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(z, 11);
	}
	
	@Test
	public void simpleDecExpr() {
		String result = compileAndRunFile("SimpleDecExpr");
		int z = 30;
		for (int i = 0; i < 11; i++) {
			z--;
		}
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(z, 19);
	}
	
	@Test
	public void immediateOpAss() {
		String result = compileAndRunFile("ImmediateOpAss");
		String output = "";
		int i = 0;
		i += 4;
		output += i;
		i *= 10;
		output += i;
		i /= 10;
		output += i;
		i -= 4;
		output += i;
		assertEquals(result, output);
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
	
	public void assertSanitized(String expected, String actual) {
		assertEquals(expected.replaceAll("\r\n", "\n"), actual.replaceAll("\r\n", "\n"));
	}
} 
