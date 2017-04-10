/**

 * 

 */

package base;



import java.util.List;

import org.nabelab.solar.Clause;
import org.nabelab.solar.Env;
import org.nabelab.solar.Options;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.pfield.PField;

import logicLanguage.CNF;
import logicLanguage.IndepClause;
import solarInterface.CFSolver;
import solarInterface.IndepPField;
import solarInterface.SolProblem;
import agLib.masStats.StatCounter;


/**
 * @author Viel Charlotte and Gauvain Bourgne
 *
 */

public class ConsFindingLocalTheory extends SolProblem {

	
	public ConsFindingLocalTheory(SolProblem theory, int id) {
		super(theory,true);
		refNumber=id;
	}


	public ConsFindingLocalTheory(int id) {
		refNumber=id;
	}


	public SolProblem getTheory(boolean ref){
		if (ref)
			return this;
		else
			return new SolProblem(this,true);
	}

	/**
	 * @param rules the rules to add 
	 */
	public boolean addToTheory(CNF clauses)  {
		if (clauses.isEmpty()) return false;
		boolean added = false;

		for (Clause cl:clauses)
			added=added||!getClauses().contains(cl);

		addAxioms(clauses);

		if (!added)
			System.out.println(" already in theory of ag" + this.refNumber);
		return added;
	}



	////	TOOLS FOR COMPUTATIONS
	
	public CNF consequenceFinding(Env env, Options opt, PField pField, List<Clause> topClauses,
					List<StatCounter<Integer>> ctr, boolean usePrevTopClauses, long deadline) throws Exception {
		
		SolProblem pb;
		if (!usePrevTopClauses){
			pb = new SolProblem(env, opt, getClauses(), topClauses, pField);
		}else{
			CNF tc= new CNF();
			tc.addAll(getTopClauses());
			tc.addAll(topClauses);
			pb = new SolProblem(env, opt, getAxioms(), tc, pField);
		}
		pb.setDepthLimit(getDepthLimit());	
		CNF result = new CNF();
		// TODO !!! Attention : trueNewCarc was at false for PB base approaches... Not sure if better. TO TEST.
		boolean trueNewCarc=!(topClauses==null || topClauses.isEmpty());
		CFSolver.solveToClause(pb, deadline, ctr, result, incremental, trueNewCarc, null);
		return result;
	}

	

	public String toString() {
		return "Theory "+refNumber+": "+super.toString();		
	}
	
//	protected SolProblem theory=new SolProblem();
	public int refNumber=0;
	public boolean incremental=false;
	public static final int CARC = 0;
	public static final int NEW_CARC = 1;	
//	private boolean verbose;

}

