package comp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A function is comprised of a return value, id, and its overloads.
 * Each overload contains the label (location in the final program),
 * it's arguments (each overload has a unique sequence of arguments),
 * and a pointer to the common function class. 
 * Batteries included.
 *
 */
class Function {
	class Overload {
		public String label;
		public Type[] args;
		public Function func;
		public boolean parallel;
		public int sprockell;
		
		/**
		 * Prints a pretty representation of the overload.
		 */
		@Override
		public String toString() {
			String r = func.returnType + " " + func.id + "(";

			if (args.length > 0) {
				if (args[0] == null) {
					System.out.println("A null argument happened. Doublecheck or remove the break");
				}
				r += args[0];
				
				for (int i = 1; i < args.length; i++) {
					Type arg = args[i];

					if (arg == null) {
						System.out.println("A null argument happened. Doublecheck or remove the break");
						break; 
					}

					r += ", " + arg.toString();
				}
			}
			
			r += " ";
					
			if (parallel) r += " is parallel on sprockell " + sprockell + " )";
			else
				r += ")";
			
			return r;
		}
	}
	
	public List<Overload> overloads = new ArrayList<>();
	public String id;
	public Type returnType;
	
	public Function(String id, Type returnType) {
		this.id = id;
		this.returnType = returnType;
	}
	
	/**
	 * Find the overload corresponding with the given argument sequence.
	 * @param args The argument signature of the overload you're looking for.
	 * @return If the overload exists, a pointer to the overload. Otherwise null.
	 */
	public Overload getOverload(Type[] args) {
		for (Overload overload : overloads) {
			if (overload.args.length == 0 && args.length == 0)
				return overload;
			if (Arrays.equals(overload.args, args)) {
				return overload;
			}
		}
		
		return null;
	}
	
	/**
	 * Register a certain overload of this function.
	 * @param args The arguments of the overload
	 * @param label The location of the overload in the final program
	 * @return An error (in the form of a string) if something went wrong. null if it went ok.
	 */
	public String registerOverload(Type[] args, String label, boolean parallel, int sprockell) {
		Overload overload = new Overload();
		
		overload.args = args;
		overload.label = label;
		overload.func = this;
		overload.parallel = parallel;
		overload.sprockell = sprockell;
		
		for (Overload ol : overloads) {
			if (ol.args.length == 0 && overload.args.length == 0) return "Overload already registered";
			if (Arrays.equals(ol.args, overload.args)) return "Overload is already registered";
			if (ol.label.equals(overload.label)) return "Label already in use";
		}
		
		overloads.add(overload);
		
		return null;
	}
	
	/**
	 * Prints a pretty representation of all the functions and the overloads
	 * in a multiline string.
	 */
	@Override
	public String toString() {
		String r = "";
		System.out.println("lets do this");

		for (Overload ol : overloads) {
			r += "- " + ol.toString() + "\n"; 
			System.out.println("done 1");
		}
		
		return r;
	}
}