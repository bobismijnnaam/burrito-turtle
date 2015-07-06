package comp;

import java.util.ArrayList;
import java.util.List;

import lang.BurritoBaseVisitor;
import lang.BurritoParser.ImpContext;

import org.antlr.v4.runtime.tree.ParseTree;

public class Importer extends BurritoBaseVisitor<Integer> {

	private List<String> imports;
	
	public Importer(ParseTree tree) {
		imports = new ArrayList<String>();
		tree.accept(this);
	}
	
	@Override
	public Integer visitImp(ImpContext ctx) {
		imports.add(ctx.STRING().getText().replaceAll("\"", ""));
		return 0;
	}
	
	public List<String> getImports() {
		return imports;
	}
}
