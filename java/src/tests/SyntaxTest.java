package tests;

import lang.BurritoLexer;
import lang.BurritoParser;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

public class SyntaxTest {
	
	@Test
	public void expressionsCorrect() {
		String testProgram = "void program() int a = 0; int b = 3 * 3; bool c = 1 == 1; int d = (3+3)*3; <-; .";
		parse(testProgram);
		
		testProgram = "void program() int a = (((3+3)+4)*3)+3; <-; .";
		parse(testProgram);
		
		testProgram = "void program() int a = 3; a += (((3+3)+4)*3)+3; <-; .";
		parse(testProgram);
		
		testProgram = "void program() int a = 3; a *= (((3+3)+4)*3)+3; <-; .";
		parse(testProgram);
		
		testProgram = "void program() int a = 3; a /= (((3+3)+4)*3)+3; <-; .";
		parse(testProgram);
		
		testProgram = "void program() int a = 3; a -= (((3+3)+4)*3)+3; <-; .";
		parse(testProgram);
	}
	
	@Test
	public void expressionsWrong() {
		// forget semicolomn
		String testProgram = "void program() int a = 0 <-; .";
		parse(testProgram);
		
		// unmatched parentheses
		testProgram = "void pogram() int a = (3*3; <-; .";
		parse(testProgram);
		
		// wrong structured shorthand assignment
		testProgram = "void program() int a = 3; a =- (((3+3)+4)*3)+3; <-; .";
		parse(testProgram);
	}
	
	@Test
	public void controlFlowCorrect() {
		// IF ELSE
		String testProgram = "void program() int a = 0; a == 0 ? a|; ! a = 3; . <-; .";
		parse(testProgram);
		
		testProgram = "void program() int a = 0; a == 0 ? a|; . <-; .";
		parse(testProgram);
		
		// WHILE
		testProgram = "void program() int a = 0; a < 100 @ a+; . <-; .";
		parse(testProgram);
		
		// SWITCH
		testProgram = "void program() int a = 0; <a> 0: a|; any: a = 4; . <-; .";
		parse(testProgram);
		
		testProgram = "void program() int a = 0; <a> any: a = 4; . <-; .";
		parse(testProgram);
	}
	
	@Test
	public void controlFlowWrong() {
		// wrongly placed if symbol
		String testProgram = "void program() int a = 0; ? a == 0 a|; ! a = 3; . <-; .";
		parse(testProgram);
		
		// wrong while syntax
		testProgram = "void program() int a = 0; @ a < 100 a+; . <-; .";
		parse(testProgram);
		
		// wrong switch statement
		testProgram = "void program() int a = 0; <a any: a = 4; . <-; .";
		parse(testProgram);
	}
	
	@Test
	public void dataTypeCorrect() {
		// one dimension array
		String testProgram = "void program() int[] a = [1, 2, 3, 4]; <-; .";
		parse(testProgram);
		
		// multi dimensional array
		testProgram = "void program() int[4][4] a; <-; .";
		parse(testProgram);
		
		// pointers
		testProgram = "void program() char_ a; <-; .";
		parse(testProgram);
		
		testProgram = "void program() char_ a; _a|;<-; .";
		parse(testProgram);
	}
	
	@Test
	public void dataTypeWrong() {
		// wrong array init
		String testProgram = "void program() int a[] = [1, 2, 3, 4]; <-; .";
		parse(testProgram);
	}
	
	@Test
	public void concurrencyCorrect() {
		// threads
		String testProgram = "void~ thread1() . void program() -> thread1; <-; .";
		parse(testProgram);
		
		// locks
		testProgram = "Lock l; void program() lock l; unlock l; <-; .";
		parse(testProgram);
	}
	
	@Test
	public void concurrencyWrong() {
		String testProgram = "~void thread1() . void program() -> thread1; <-; .";
		parse(testProgram);
		
		testProgram = "void program() unlock; <-; .";
		parse(testProgram);
	}
	
	@Test
	public void importCorrect() {
		String testProgram = "pls \"ruben\"; void program() .";
		parse(testProgram);
		
		testProgram = "pls \"ruben/llama\"; void program() .";
		parse(testProgram);
	}
	
	@Test
	public void importWrong() {
		String testProgram = "pls ruben; void program() .";
		parse(testProgram);
	}
	
	/**
	 * @param testProgram the string input to parse
	 * @return the generated ParseTree from the string
	 */
	private ParseTree parse(String testProgram) {
		CharStream input = new ANTLRInputStream(testProgram);
		Lexer lexer = new BurritoLexer(input);
		TokenStream tokens = new CommonTokenStream(lexer);
		BurritoParser parser = new BurritoParser(tokens);
		return parser.program();
	}
	
}
