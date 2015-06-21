package sprockell;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sprockell.Sprockell.Op;

public class Program {
	private List<Instr> instr = new ArrayList<Instr>();;
	private Map<String, Integer> labelMap = new HashMap<String, Integer>();
	private Map<Integer, String> lineMap = new HashMap<Integer, String>();
	
	/**
	 * Emits an instruction
	 * @param opCode
	 * @param args
	 */
	public void emit(Op opCode, Operand... args) {
		instr.add(new Instr(opCode, args));
	}
	
	/**
	 * Emits an instruction with a label on the instruction
	 * @param label
	 * @param opCode
	 * @param args
	 */
	public void emit(String label, Op opCode, Operand... args) {
		putLabel(label, instr.size());
		emit(opCode, args);
	}

	/**
	 * Assigns a label to a given line
	 * @param label
	 * @param line
	 */
	public void putLabel(String label, int line) {
		labelMap.put(label, line);
		lineMap.put(line, label);
	}
	
	/**
	 * Prints every instruction line by line
	 * @param indent The amount of tabs to be inserted before each line
	 * @return All the instructions as one gigantic string
	 */
	public String prettyString(int indent, boolean printLabels) {
		String result = "";
		
		for (int i = 0; i < instr.size() - 1; i++) {
			if (printLabels) {
				if (lineMap.containsKey(i)) {
					result += lineMap.get(i) + ": ";
				}
			}
			for (int j = 0; j < indent; j++) {
				result += "\t";
			}
			result += instr.get(i).toString();
			
			if (i != instr.size() - 1) {
				result += ",\n";
			}
		}
		
		return result;
	}
	
	/**
	 * Checks if a program is well-formed, i.e. proper amount of arguments and proper argument types.
	 * @return True if it's fine, otherwise false (and a println)
	 */
	public boolean isWellFormed() {
		for (int i = 0; i < instr.size(); i++) {
			if (!checkArgLength(instr.get(i))) {
				System.out.printf("Arg length does not match [line %i].\n", i);
				return false;
			} else if (!checkArgTypes(instr.get(i))) {
				System.out.printf("Argument types do not match [line %i].\n", i);
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Writes the program to a file. Does no syntax checking, or label substitution.
	 * @param fileName The file to be written to
	 * @throws FileNotFoundException Not sure why this would ever happen
	 */
	public void writeToFile(String fileName) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(fileName);
		
		writer.println("import Sprockell.System\n");
		writer.println("prog = [");
		
		writer.println(prettyString(2, false) + "\n]");
		
		writer.println("main = run 1 prog");
		
		writer.close();
		
		System.out.printf("Written to file succesfully!\n ->[%s]\n", fileName);
	}
	
	/**
	 * Checks if a given instruction has the proper amount of arguments according to its opcode.
	 * @param in The instruction
	 * @return True if it's correct, otherwise false
	 */
	private boolean checkArgLength(Instr in) {
		return in.op.sig.length == in.args.length;
	}
	
	/**
	 * Checks for a given instruction if all the argument types match
	 * @param in The instruction
	 * @return True if it's correct, otherwise false
	 */
	private boolean checkArgTypes(Instr in) {
		boolean valid = true;
		
		for (int i = 0; i < in.args.length; i++) {
			valid &= in.op.sig[i] == in.args[i].getType();
		}
		return valid;
	}
	
	/**
	 * Does label subsitution. Actually changes arguments in said program, so if you want to keep the original around make a copy!
	 */
	public void fixLabels() {
		for (Instr inst : instr) {
			inst.fixLabel(labelMap);
		}
	}
}
