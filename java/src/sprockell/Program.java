package sprockell;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import sprockell.Sprockell.Op;

public class Program {
	private List<Instr> instr;
	
	public Program() {
		instr = new ArrayList<Instr>();
	}
	
	public void emit(Op opCode, Operand... args) {
		instr.add(new Instr(opCode, args));
	}
	
	public void writeToFile(String fileName) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(fileName);
		
		writer.println("import Sprockell.System\n");
		writer.println("prog = [");
		
		for (int i = 0; i < instr.size() - 1; i++) {
			if (!checkArgLength(instr.get(i))) {
				System.out.println("Number of arguments does not match op.");
			} else if (!checkArgTypes(instr.get(i))) {
				System.out.println("Argument types do not match constructor");
			} else {
				writer.println("\t\t" + instr.get(i).toString() + ",");
			}
		}
		
		if (!checkArgLength(instr.get(instr.size() - 1))) {
			System.out.println("Number of arguments does not match op.");
		} else if (!checkArgTypes(instr.get(instr.size() - 1))) {
			System.out.println("Argument types do not match constructor");
		} else {
			writer.println("\t\t" + instr.get(instr.size() - 1).toString() + "\n\t\t]\n");
		}
		
		writer.println("main = run 1 prog");
		
		writer.close();
	}
	
	private boolean checkArgLength(Instr in) {
		return true;
	}
	
	private boolean checkArgTypes(Instr in) {
		return true;
	}
}
