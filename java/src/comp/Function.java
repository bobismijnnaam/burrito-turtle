package comp;

class Function {
	public Type[] args;
	public String id;
	public Type returnType;
	public String label;
	
	public Function(Type[] args, String id, Type returnType, String label) {
		this.args = args;
		this.id = id;
		this.returnType = returnType;
		this.label = label;
	}
}