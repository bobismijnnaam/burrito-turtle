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
				+ "int add(int x, int y) <- x + y; ."
				+ "int program()"
				+ "	false|;"
				+ "	10|;"
				+ "	false|;"
				+ "	13|;"
				+ "	false|;"
				+ "	2+3|;"
				+ "	false|;"
				+ "	add(2, 3)|;"
				+ "	false|;"
				+ "	500|;"
				+ "	false|;"
				+ "	123456789|;"
				+ "	false|;"
				+ "	10|;"
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
