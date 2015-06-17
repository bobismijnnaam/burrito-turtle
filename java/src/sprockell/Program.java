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
				System.out.printf("Arg length does not match [line %i].\n", i);
			} else if (!checkArgTypes(instr.get(i))) {
				System.out.printf("Argument types do not match [line %i].\n", i);
			} else {
				writer.println("\t\t" + instr.get(i).toString() + ",");
			}
		}
		
		if (!checkArgLength(instr.get(instr.size() - 1))) {
			System.out.printf("Arg length does not match [line %i].\n", instr.size() -1);
		} else if (!checkArgTypes(instr.get(instr.size() - 1))) {
			System.out.printf("Argument types do not match [line %i].\n", instr.size() -1);
		} else {
			writer.println("\t\t" + instr.get(instr.size() - 1).toString() + "\n\t\t]\n");
		}
		
		writer.println("main = run 1 prog");
		
		writer.close();
		
		System.out.printf("Valid program (Y) compiled succesfully!\n ->[%s]", fileName);
	}
	
	private boolean checkArgLength(Instr in) {
		return in.op.sig.length == in.args.length;
	}
	
	private boolean checkArgTypes(Instr in) {
		boolean valid = true;
		
		for (int i = 0; i < in.args.length; i++) {
			valid &= in.op.sig[i] == in.args[i].getType();
		}
		return valid;
	}
}
