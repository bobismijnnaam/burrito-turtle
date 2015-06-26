package tests;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;

import org.junit.Test;

import sprockell.Program;
import sprockell.Sprockell;
import sprockell.Sprockell.Op;

public class GeneratorTest {
	@Test
	public void scary() throws FileNotFoundException {
		String prog = ""
//				+ "int[2] doubler(int x) int[2] res; res[0] = x; res[1] = x; <- res; ."
				+ "int program()"
				+ "	int[3][3][3][3] x;"
				+ "	x[0][0][0][0];"
//				+ "	int[2] scary;"
//				+ "	scary = doubler(12345);"
//				+ "	scary[0]|\\;"
//				+ "	scary[1]|\\;"
				+ "	<- 0;"
				+ "."
				;
		Program result = Sprockell.compile(prog);
		System.out.println(result.prettyString(0, true));
		String output = SprockellTest.compileAndRun(prog);
		
		assertNotNull(output);
		System.out.println(output);
//		assertFalse(output.equals(""));

		System.out.println("Done!");
	}
}
