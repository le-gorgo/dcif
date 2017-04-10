/**
 * 
 */
package logicLanguage;

import org.nabelab.solar.Clause;
import org.nabelab.solar.ClauseTypes;
import org.nabelab.solar.Env;
import org.nabelab.solar.Options;
import org.nabelab.solar.parser.ParseException;

import genLib.tools.Arguments;
import genLib.tools.Pair;

/**
 * @author Bourgne Gauvain
 * 
 * This class aims at provinding tools for parsing, storing and converting formulas from problems files.
 * For generality, it represents formula with a type and a String description.
 * Others fields such as roles and names are taken from TPTP formalism.
 * Conversion is proposed towards solar clauses (for .sol file), and some special ASP representation
 * that is used to distribute the rules among agents. 
 *
 */
/**
 * @author Gauvain Bourgne
 *
 */
public class PbFormula {

	
	
	
	/**
	 * Build a new PbFormula with following values
	 * @param type, the type of the formula ("cnf", "fof"/ "sol" / "asp")
	 * @param name, the name of the formula
	 * @param role, the role of the formula (axiom, hypothesis, negated_conjecture, top_clause...)
	 * @param formula, the String description of the formula
	 */
	public PbFormula(String type, String name, String role, String formula) {
		this.formula = formula;
		this.name = name;
		this.role = role;
		this.type = type;
	}

	/**
	 * Build a new PbFormula from an IndepClause:
	 *  - type is set to "sol"
	 *  - name and formula are taken from the IndepClause
	 *  @param cl, the clause to be used for name and description
	 *  @param role, the role of the formula ("axiom" or "top_clause")
	 */
	public PbFormula(Clause cl, String role) {
		this.formula = cl.toString();
		this.name = cl.getName();
		this.role = role;
		this.type = "sol";
	}

	
	/**
	 * @return name, the name of the formula
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return role, the role of the formula (@see role)
	 */
	public String getRole() {
		return role;
	}
	
	/**
	 * @return type, the type of the formula ("cnf", "fof", "ttf" / "sol" / "asp")
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * @return formula, the string description of the formula itself
	 */
	public String getFormula() {
		return formula;
	}
	
	
	public static PbFormula parseFormulaBlock(String block, String source){
		String type, name, role, formula, temp;
		temp=block.replace('\n', ' ');
		//if (source.equals("TPTP"))
		type=temp.substring(0, block.indexOf("("));
		if (source.equals("SOLAR") && type.equals("cnf"))
				type="sol";			
		//extract arguments
		temp=temp.substring(temp.indexOf("(")+1, temp.lastIndexOf(")."));
		Arguments arg=Arguments.parse("["+temp+"]");
		//attribute arguments to correct string
		if (type.equals("pf")){
			name="production field";
			role="production_field";
			formula=arg.get(0).trim();
		}
		else{
			name=arg.get(0).trim();
			role=arg.get(1).trim();
			formula=arg.get(2).trim();			
		}
		return new PbFormula(type,name,role,formula);
	}
	
	
	/**
	 * Convert the current formula to another type.
	 * Currently supported : 
	 *			cnf -> sol, 
	 *			cnf -> asp, 
	 *			sol -> asp,
	 *			asp -> sol (not yet)
	 * @param targetType, the type 
	 * @return
	 */
	public PbFormula convertTo(String targetType){
		if (type.equals("cnf")){
			if (targetType.equals("sol"))
				return cnf2sol();
			if (targetType.equals("asp"))
				return cnf2sol().sol2asp();
		}
		if (type.equals("sol")){
			if (targetType.equals("asp"))
				return sol2asp();
		}
		
		return null;
	}
	
	private PbFormula cnf2sol(){
		String base=formula.trim(); 
		if (base.startsWith("("))
			base=base.substring(1,base.lastIndexOf(")")).trim();
		base=base.replace('|', ',');
		base=base.replace('~', '-');
		while (base.indexOf(" ,")>=0) base=base.replace(" ,", ",");
		while (base.indexOf("- ")>=0) base=base.replace("- ", "-");
		return new PbFormula("sol",name, role, "["+base+"]");
	}
	
	/* For "asp", it would be: 	clause(name). [topClause(name).] 
	 * 							predicate(pred\2). predicate(prop\0). predicate(yet_another_pred\1).
	 * 							hasLit(name,pred1\2,pos,arg2(f(X,c),2)). 
	 * 							hasLit(name,prop\0,neg,arg0). 
	 * 							hasLit(name,yet_another_pred\1, pos, arg1(X)).  								
	 */
	private PbFormula sol2asp(){
		Arguments literals=Arguments.parse(formula);
		String res="clause("+name+"). \n";
		if (role.equals("top_clause"))
			res+="topClause("+name+"). \n";
		String sign;
		int indexArg=1;
		for (String lit:literals){
			if (lit.trim().startsWith("-")) {
				sign="neg";
				lit=lit.substring(lit.indexOf('-')+1).trim();
			}
			else sign="pos";
			Pair<String,String> predAndArgs=getPredAndArgs(lit,indexArg);
			res+="predicate("+predAndArgs.getLeft()+").\n";
			if (predAndArgs.getRight().equals("arg0"))
				res+="hasLit("+name+", "+predAndArgs.getLeft()+", "
							+sign+", "+predAndArgs.getRight()+").\n";
			else {
				res+="hasLit("+name+", "+predAndArgs.getLeft()+", "
				+sign+", arg"+indexArg+").\n";
				res+=predAndArgs.getRight();
				indexArg++;
			}
		}
		return new PbFormula("sol",name, role, res);
	}
	/*
	 * Used for ASP translation
	 * given a literal pred(t1,...,tn), return "pred\n" and "argn(t1,...tn)"
	 * ex : for "pred(f(2,3),X,Y)" returns <"pred\3","argument3(argn, a_f_lpar_2_comma_3_rpar_,a_maj_X,a_maj_Y)"
	 * Need to convert all variable so that they would be interpreted as constant by ASP
	 */
	private Pair<String,String> getPredAndArgs(String lit, int indexArg){
		String pred, args;
		int pos=lit.indexOf('(');
		if (pos>=0) {
			pred=lit.substring(0, pos);
			args=lit.substring(pos+1,lit.lastIndexOf(')'));
			Arguments separateArgs=Arguments.parse("["+args+"]");
			int arity=separateArgs.size();
			pred=pred+"_"+arity;
			args="argument"+arity+"(arg"+indexArg;
			for (String a:separateArgs){
				String aNoMaj="";
				for (int i=0;i<a.length();i++){
					char c=a.charAt(i);
					if (Character.isUpperCase(c))
						aNoMaj+="maj_"+c;
					else if (c=='(')
						aNoMaj+="_lpar_";
					else if (c==')')
						aNoMaj+="_rpar_";
					else if (c==',')
						aNoMaj+="_comma_";
					else if (c!=' ')
						aNoMaj+=a.charAt(i);
				}
				args+=", a_"+aNoMaj;
			}
			args+=").\n";
		}
		else{
			pred=lit+"_0";
			args="arg0";
		}
		return new Pair<String,String>(pred,args);
	}

	
	/**
	 * Convert to an IndepClause
	 * Currently supported as source type : cnf, sol 
	 * @param targetType, the type 
	 * @return
	 */
	public Clause toClause(Env env) throws ParseException{
		PbFormula solVersion;
		if (type.equals("sol"))
			solVersion=this;
		else
			solVersion=convertTo("sol");
		int kind = ClauseTypes.AXIOM;
		if(role == "top_clause")
			kind = ClauseTypes.TOP_CLAUSE;
		return Clause.parse(env, new Options(env), solVersion.getName(), kind, solVersion.getFormula());
	}

	public String toString(){
		return formula;
	}
	
	/**
	 * The name of the formula
	 */
	public String name;
	/**
	 * The role of the formula.
	 * For TPTP formula, it should be one of : axiom, hypothesis, conjecture, negated_conjecture, 
	 * definition, lemma, theorem (see TPTP doc for more info).
	 * For SOLAR formula, it should be one of : axiom, top_clause (though TPTP role might be used
	 *  temporarily)
	 * For ASP conversion, this is not used (take value for TPTP).
	 */
	public String role;
	/**
	 * The type of the formula.
	 * For TPTP, it should be : "cnf", "ttf", "fof",...
	 * Currently, only "cnf" is supported.
	 * For SOLAR, it should be "sol"
	 * For ASP conversion, it should be "asp"
	 */
	public String type;
	/**
	 * The string representing the formula itself.
	 * Syntax depends on the type.
	 * For "cnf", it should be of the form: (pred1(f(X,c),2) | ~ prop | yet_another_pred(X))
	 * For "sol", it should be of the form: [pred1(f(X,c),2), -prop, yet_another_pred(X)]
	 * For "asp", it would be: 	clause(name). 
	 * 							predicate(pred\2). predicate(prop\0). predicate(yet_another_pred\1).
	 * 							hasLit(name,pred1\2,pos,arg2(f(X,c),2)). 
	 * 							hasLit(name,prop\0,neg,arg0). 
	 * 							hasLit(name,yet_another_pred\1, pos, arg1(X)).  								
	 */
	public String formula;
	
	
}
