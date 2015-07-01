package tests;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.junit.Test;

import sprockell.Program;
import sprockell.Sprockell;

public class GeneratorTest {
	String program = "Lock a; int program() -> inThread; lock a; 123|\\; unlock a; <- 0; . int~ inThread() lock a; 456|\\; unlock a; <- 0; .";
	
	@Test
	public void slow() throws FileNotFoundException {
		int cores = 1;
		
		Program prog = null;
		try {
			prog = Sprockell.scaryCompile(program);
			System.out.println(prog.prettyString(0, true));
			prog = Sprockell.compile(program);
			prog.writeToSir(5 + ".sir");
//			SprockellTest.compileAndRun(program, 5);
		} catch(Exception e) {
		}
		
//		try {
//			prog.writeToFile("test.hs", cores);
//			
//			Runtime rt = Runtime.getRuntime();
//			Process buildPr = rt.exec("ghc -i../sprockell/src -e main test.hs");
//			buildPr.waitFor();
//
//			InputStream is = buildPr.getInputStream();
//			
//			Scanner s = new Scanner(is).useDelimiter("\\A");
//			System.out.println("Slow:");
//			System.out.println(s.hasNext() ? s.next() : "");
//
//		    return;
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//			System.out.println("File not found");
//			return;
//		} catch (IOException e) {
//			e.printStackTrace();
//			System.out.println("IO Exception");
//			return;
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//			System.out.println("We got interrupted");
//			return;
//		} catch (NoSuchElementException e) {
//			e.printStackTrace();
//			System.out.println("Stream was empty");
//			return;
//		}	
	}
	
//	@Test
//	public void fast() throws IOException, InterruptedException {
//		int cores = 1;
//		
//		Program prog = Sprockell.compile(program);
//		
//		Runtime rt = Runtime.getRuntime();
////		Process buildPr = rt.exec("ghci");
//		Process buildPr = rt.exec("ghc -i../sprockell/src -e \"" + prog.toHaskell(2) + "\"");
//
//		InputStream is = buildPr.getInputStream();
//		OutputStream os = buildPr.getOutputStream();
//		
//		os.write("map (+1) [1..5]\n:q\n".getBytes("US-ASCII"));
//		buildPr.waitFor();
//		
//		Scanner s = new Scanner(is).useDelimiter("\\A");
//		System.out.println("Fast:");
//		System.out.println(s.hasNext() ? s.next() : "");
//	}
}
