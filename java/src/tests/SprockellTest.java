package tests;

import java.io.FileNotFoundException;

import org.junit.Test;

import static sprockell.Sprockell.Reg.*;
import static sprockell.Sprockell.Operator.*;
import sprockell.Program;
import sprockell.Sprockell.Op;
import sprockell.Reg;
import sprockell.Operator;

public class SprockellTest {
	
	@Test
	public void SprockellTest() {
		Program prog = new Program();
		
		prog.emit(Op.Compute, new Operator(Add), new Reg(RegA), new Reg(RegB));
		prog.emit(Op.EndProg);
		try {
			prog.writeToFile("program.hs");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
