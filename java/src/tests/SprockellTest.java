package tests;

import static org.junit.Assert.*;
import static sprockell.Operator.Which.Add;
import static sprockell.Reg.Which.RegA;
import static sprockell.Reg.Which.RegB;
import static sprockell.Reg.Which.RegC;
import static sprockell.Sprockell.Op.Compute;
import static sprockell.Sprockell.Op.Const;
import static sprockell.Sprockell.Op.EndProg;
import static sprockell.Sprockell.Op.Jump;
import static sprockell.Sprockell.Op.Write;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import sprockell.MemAddr;
import sprockell.Operator;
import sprockell.Program;
import sprockell.Reg;
import sprockell.Target;
import sprockell.Value;

public class SprockellTest {
	
	@Test
	public void hashMapTest() {
		String a = new String("a");
		String b = new String("c");
		b = b.replace("c", "a");
		
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put(a, 3);
		map.put(b, 1800);
	
		System.out.println("a is " + a);
		System.out.println("b is " + b);
		System.out.println("a equals " + map.get("a"));
		// (y) (y)
	}
	
	@Test
	public void sprockellTest() {
		Program prog = new Program();
		prog.emit(Const, new Value(2), new Reg(RegA));
		prog.emit(Const, new Value(3), new Reg(RegB));
		prog.emit("Ruben's Dan examen!!", Compute, new Operator(Add), new Reg(RegA), new Reg(RegB), new Reg(RegC));
		prog.emit(Jump, new Target("Ruben's Dan examen!!"));
		prog.emit(Write, new Reg(RegC), new MemAddr("stdio"));
		prog.emit(EndProg);
		
		assertTrue(prog.isWellFormed());
		
		System.out.println("Before subsitution: ");
		System.out.println(prog.prettyString(0, true));
		System.out.println("After subsitution");
		prog.fixLabels();
		System.out.println(prog.prettyString(0, false));
		
//		try {
////			prog.writeToFile("program.hs");
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
}
