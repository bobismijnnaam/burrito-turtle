package tests;

import static sprockell.Sprockell.Operator.*;
import static sprockell.Sprockell.Op.*;
import static sprockell.Sprockell.Reg.*;

import java.io.FileNotFoundException;

import org.junit.Test;

import sprockell.*;
import sprockell.MemAddr;
import sprockell.Operator;
import sprockell.Reg;
import sprockell.Sprockell.*;
import sprockell.Sprockell.*;
import sprockell.Value;

public class SprockellTest {
	
	@Test
	public void SprockellTest() {
		Program prog = new Program();
		prog.emit(Const, new Value(2), new Reg(RegA));
		prog.emit(Const, new Value(3), new Reg(RegB));
		prog.emit(Compute, new Operator(Add), new Reg(RegA), new Reg(RegB), new Reg(RegC));
		prog.emit(Write, new Reg(RegC), new MemAddr("stdio"));
		prog.emit(EndProg);
		try {
			prog.writeToFile("program.hs");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
