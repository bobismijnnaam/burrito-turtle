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

import comp.ErrorListener;
import comp.ParseException;

public class SyntaxTest {
	
	@Test
	public void expressionsCorrect() throws ParseException {
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
	
	@Test (expected=ParseException.class) 
	public void expressionsWrong() throws ParseException {
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
	public void controlFlowCorrect() throws ParseException {
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
	
	@Test (expected=ParseException.class) 
	public void controlFlowWrong() throws ParseException {
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
	public void dataTypeCorrect() throws ParseException {
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
	
	@Test (expected=ParseException.class) 
	public void dataTypeWrong() throws ParseException {
		// wrong array init
		String testProgram = "void program() int a[] = [1, 2, 3, 4]; <-; .";
		parse(testProgram);
	}
	
	@Test
	public void concurrencyCorrect() throws ParseException {
		// threads
		String testProgram = "void~ thread1() . void program() -> thread1; <-; .";
		parse(testProgram);
		
		// locks
		testProgram = "Lock l; void program() lock l; unlock l; <-; .";
		parse(testProgram);
	}
	
	@Test (expected=ParseException.class) 
	public void concurrencyWrong() throws ParseException {
		String testProgram = "~void thread1() . void program() -> thread1; <-; .";
		parse(testProgram);
		
		testProgram = "void program() unlock; <-; .";
		parse(testProgram);
	}
	
	@Test
	public void importCorrect() throws ParseException {
		String testProgram = "pls \"ruben\"; void program() .";
		parse(testProgram);
		
		testProgram = "pls \"ruben/llama\"; void program() .";
		parse(testProgram);
	}
	
	@Test (expected=ParseException.class) 
	public void importWrong() throws ParseException {
		String testProgram = "pls ruben; void program() .";
		parse(testProgram);
	}
	
	/**
	 * @param testProgram the string input to parse
	 * @return the generated ParseTree from the string
	 * @throws ParseException 
	 */
	private ParseTree parse(String testProgram) throws ParseException {
		ErrorListener listener = new ErrorListener();

		CharStream input = new ANTLRInputStream(testProgram);
		Lexer lexer = new BurritoLexer(input);
		lexer.removeErrorListeners();
		lexer.addErrorListener(listener);
		
		TokenStream tokens = new CommonTokenStream(lexer);
		BurritoParser parser = new BurritoParser(tokens);
		parser.removeErrorListeners();
		parser.addErrorListener(listener);
		
		ParseTree result = parser.program();
		
		listener.throwException();
		
		return result;
	}
	
}
