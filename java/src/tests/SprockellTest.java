package tests;

import static sprockell.Sprockell.Operator.Add;
import static sprockell.Sprockell.Reg.RegA;
import static sprockell.Sprockell.Reg.RegB;
import static sprockell.Sprockell.Reg.RegC;

import java.io.FileNotFoundException;

import org.junit.Test;

import sprockell.MemAddr;
import sprockell.Operator;
import sprockell.Program;
import sprockell.Reg;
import sprockell.Sprockell.Op;
import sprockell.Value;

public class SprockellTest {
	
	@Test
	public void SprockellTest() {
		Program prog = new Program();
		prog.emit(Op.Const, new Value(2), new Reg(RegA));
		prog.emit(Op.Const, new Value(3), new Reg(RegB));
		prog.emit(Op.Compute, new Operator(Add), new Reg(RegA), new Reg(RegB), new Reg(RegC));
		prog.emit(Op.Write, new Reg(RegC), new MemAddr("stdio"));
		prog.emit(Op.EndProg);
		try {
			prog.writeToFile("program.hs");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
