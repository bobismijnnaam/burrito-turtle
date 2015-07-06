package tests;

import static org.junit.Assert.*;
import static tests.SprockellTest.*;

import org.junit.Test;

public class ConcurrencyTest {
	@Test
	public void petersons() {
		String result = SprockellTest.compileAndRunFile("Peterson", 3);
		assertNotNull("Compiling went wrong", result);
		assertSanitized("0\n", result);
	}
	
	@Test
	public void banking() {
		String result = SprockellTest.compileAndRunFile("Banking", 5);
		assertNotNull("Compiling went wrong", result);
		assertSanitized("9235\n", result);
	}
}
