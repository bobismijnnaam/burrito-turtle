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
				+ "int add(int x, int y)\n"
				+ "	<- x + y;\n"
				+ ".\n"
				+ "bool isEven(int x) <- x % 2 == 0;.\n"
				+ "int program()\n"
				+ "	int total = 0;\n"
				+ "	int x = 0; x < 100 @\n"
				+ "		isEven(x) ?\n"
				+ "			x|\\;\n"
				+ "			total = add(total, x);\n"
				+ "		.\n"
				+ "		x = add(x, 1);"
				+ "	.\n"
				+ " |\\;\n"
				+ " total|\\;\n"
				+ "	<- 0;\n"
				+ ".\n"
				;
//		Program result = Sprockell.compile(prog);
//		System.out.println(result.prettyString(0, true));
		String output = SprockellTest.compileAndRun(prog);
		
		assertNotNull(output);
		System.out.println(output);
		assertFalse(output.equals(""));

		System.out.println("Done!");
	}
}
