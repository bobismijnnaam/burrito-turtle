package tests;

import static org.junit.Assert.*;
import static tests.SprockellTest.*;

import org.junit.Test;

import sprockell.Program;
import sprockell.Sprockell;

public class ConcurrencyTest {
	@Test
	public void petersons() {
		String result = SprockellTest.compileAndRunFile("Peterson", 3);
		assertNotNull(result);
		assertSanitized("0\n", result);
	}
}
