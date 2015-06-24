package sprockell;

import java.util.Map;


public class Value extends Operand {
	private int value;
	private String label;
	private Which which;
	
	public Value(int value) {
		this.value = value;
		this.which = Which.IntVal;
	}
	
	public Value(String label) {
		this.label = label;
		this.which = Which.LabelVal;
	}
	
	public String toString() {
		switch (which) {
		case IntVal:
			if (value < 0) return "(" + value + ")";
			return value + "";
		case LabelVal:
			return "[" + label + "]";
		}
		return value + "";
	}
	
	@Override
	public Type getType() {
		return Type.VALUE;
	}
	
	public Which getWhich() {
		return which;
	}
	
	public enum Which {
		IntVal,
		LabelVal
	}

	public boolean fixLabel(Map<String, Integer> labelMap) {
		if (which == Which.LabelVal) {
			if (!labelMap.containsKey(label)) {
				System.out.println(labelMap);
				System.out.println("Undefined label \"" + label + "\" found");
				return false;
			} else {
				int dst = labelMap.get(label);
				
				which = Which.IntVal;
				value = dst;
				label = null;

				return true;
			}
		} else {
			return true;
		}
	}
}
