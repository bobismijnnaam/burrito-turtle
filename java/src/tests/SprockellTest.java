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
	public void complexArrays() {
		String result = compileAndRunFile("ComplexArrays", 1);
		String output = "Hello country!\n"
				+ "Hello world!\n"
				+ "axbycz\n"
				+ "555111666333777555\n"
				+ "truetruefalsefalsefalsetruetruefalse\n";
		
		assertNotNull("Compiling or executing went wrong", result);
		assertSanitized(output, result);
	}
	
	@Test
	public void concurrentPipe() {
		String result = compileAndRunFile("ConcurrentPipe", 4);
		assertNotNull("Compiling or executing went wrong", result);
		
		result = result.replaceAll("\r\n", "\n");
		int count1 = (result.length() - result.replaceAll("11111\n", "").length()) / 6;
		int count2 = (result.length() - result.replaceAll("22222\n", "").length()) / 6;
		int count3 = (result.length() - result.replaceAll("33333\n", "").length()) / 6;
		
		assertEquals(count1, 5);
		assertEquals(count2, 5);
		assertEquals(count3, 5);
	}
	
	@Test
	public void simpleLocks() {
		String result = compileAndRunFile("SimpleLocks", 5);
		String output = "";
		for (int i = 0; i < 4; i++)
			output += "12345\n";

		assertNotNull("Compiling or executing went wrong", result);
		assertSanitized(output, result);
	}
	
	@Test
	public void simpleGlobals() {
		String result = compileAndRunFile("SimpleGlobals", 1);
		String output = "";
		output += "0\n";
		assertNotNull("Compiling or executing went wrong", result);
		assertSanitized(output, result);
	}

	@Test
	public void simpleFib() {
		String result = compileAndRunFile("Fib");
		String output = "0\n1\n3\n8\n21\n55\n";
		assertNotNull("Compiling or executing went wrong", result);
		assertSanitized(output, result);
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
		Program program = Sprockell.scaryCompileFile(BASE_DIR, "SimpleWhile.symbol");
		System.out.println(program.prettyString(0, true));

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
		assertSanitized(output, result);
	}
	
//	@Test
	public void simpleArray() {
		// TODO: Fix this!
		String result = compileAndRunFile("SimpleArray");
		String output = "0123456789101112131415161718192021222324252627282930313233343536373839404142434445464748495051525354555657585960616263646566676869707172737475767778798081828384858687888990919293949596979899";
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(output, result);
	}
	
	@Test
	public void simpleOverloading() {
		String result = compileAndRunFile("SimpleOverloading");
		String output = "3\n1\n25\n";
		assertNotNull("Compiling or executing went wrong", result);
		assertSanitized(output, result);
	}
	
//	@Test
	public void multiDimArray() {
		// TODO: Fix this!
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
		assertEquals(output, result);
	}
	
	@Test
	public void comments() {
		String result = compileAndRunFile("Comments");
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals("225", result);
	}
	
	@Test
	public void simpleIncExpr() {
		String result = compileAndRunFile("SimpleIncExpr");
//		int z = 0;
//		for (int i = 0; i < 11; i++) {
//			z++;
//		}
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals("11", result);
	}
	
	@Test
	public void simpleDecExpr() {
		String result = compileAndRunFile("SimpleDecExpr");
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals("19", result);
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
		assertNotNull("Compiling or executing went wrong", result);
		assertEquals(output, result);
	}

//	@Test
	public void simpleChar() {
		// TODO: Fix this!
		String result = compileAndRunFile("SimpleChar");
		String output = "Hello world\n\n";
		assertNotNull("Compiling or executing went wrong", result);
		assertSanitized(output, result);
	}
	
	@Test
	public void simpleSwitch() {
		String result = compileAndRunFile("SimpleSwitch");
		String output = "";
		int x = 2;
		
		switch (x) {
			case 1:
				output += 1;
				break;
			case 2:
				output += 2;
				break;
			case 3: 
				output += 3;
				break;
			default:
				output += 0;
				break;
		}
		
		output += "btrue";
		assertNotNull("Compiling or executing went wrong", result);
		assertSanitized(output, result);
	}
	
//	@Test
	public void importTest() {
		// TODO: Fix this!
		// compile file with imports
		String result = compileImportAndRun(new File(BASE_DIR, "Import" + EXT), 1);
		System.out.println(result);
	}
	
	/**
	 * Utility functions
	 */
	
	public static String compileAndRun(String progStr, int cores) {
		return compileAndRun(new ANTLRInputStream(progStr), cores);
	}

	public static String compileAndRun(String progStr) {
		return compileAndRun(progStr, 1);
	}
	
	public static String compileAndRunFile(String filename) {
		return compileAndRunFile(filename, 1);
	}
	
	
	public static String compileAndRunFile(String filename, int cores) {
		try {
			return compileAndRun(new ANTLRInputStream(new FileReader(new File(BASE_DIR, filename + EXT))), cores);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String compileAndRun(ANTLRInputStream input) {
		return compileAndRun(input, 1);
	}
	
	public static String compileAndRun(ANTLRInputStream input, int cores) {
		Program prog = Sprockell.compile(input);
		//System.out.println(prog.prettyString(0, true));
		int file = 5;
		
		if (prog == null) {
			System.out.println("There were errors");
			return null;
		}
	
		try {
			prog.writeToSir("program.sir");
			
			Runtime rt = Runtime.getRuntime();
			Process buildPr = rt.exec("sprint.exe " + cores); // rt.exec("ghc -i/sprockell/src -e main test.hs"
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
	
	public static String compileImportAndRun(File file, int cores) {
		Program prog = Sprockell.compileImport(file);
		
		if (prog == null) {
			System.out.println("There were errors");
			return null;
		}
	
		try {
			prog.writeToSir(5 + ".sir");
			
			Runtime rt = Runtime.getRuntime();
			Process buildPr = rt.exec("bobe.exe " + cores);
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
	
	public static void assertSanitized(String expected, String actual) {
		assertEquals(expected.replaceAll("\r\n", "\n"), actual.replaceAll("\r\n", "\n"));
	}
} 
