package mars.reasoning;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import logicLanguage.CNF;
import logicLanguage.IndepClause;
import logicLanguage.IndepLiteral;
import logicLanguage.UnitClauseCNF;

import org.nabelab.solar.CFP;
import org.nabelab.solar.Clause;
import org.nabelab.solar.ClauseTypes;
import org.nabelab.solar.Conseq;
import org.nabelab.solar.Env;
import org.nabelab.solar.ExitStatus;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Options;
import org.nabelab.solar.Stats;
import org.nabelab.solar.TermTypes;
import org.nabelab.solar.parser.ParseException;

import agLib.masStats.StatCounter;

import solarInterface.CFSolver;
import solarInterface.IndepPField;
import solarInterface.SolProblem;




public class LocalTheory implements ExitStatus, ClauseTypes, TermTypes{

	public static final int NCT_NO_FILTER=0;
	public static final int NCT_NEWC_WRT_LOCTHEO=1;
	public static final int NCT_NEWC_WRT_FULLAXIOMS=2;
	public static final int PUT_NO_FILTER=0;
	public static final int PUT_DISCARD_TOP_CLAUSES=1;
	public static final int PUT_KEEP_TOP_CLAUSES=2;
	public static final int PUT_KEEP_HYPOTHESIS=3;

	
	
	//// CONSTRUCTORS
	
	
	public LocalTheory(CFSolver solver, int id) {
		this.solver=solver;
		locTheory=new CNF();
		manifestations=new UnitClauseCNF();
	//	uncertainTheory=new ArrayList<Clause>();
		favHypothesis=new FullHypothesis();
		hypothesesField=null;
		hypOrdering=null;
//		extendedAbduciblesField=new IndepPField();
//		extendedHypField=new PField(env);
		refNumber=id;
	}
	
	public LocalTheory(CFSolver solver, CNF rules, int id) {
		this.solver=solver;
		locTheory=new CNF();
		locTheory.addAll(rules);
		manifestations=new UnitClauseCNF();
	//	uncertainTheory=new ArrayList<Clause>();
		favHypothesis=new FullHypothesis();
		hypothesesField=null;
		hypOrdering=null;
//		extendedAbduciblesField=new IndepPField();
//		extendedHypField=new PField(env);
		refNumber=id;
	}
	
	// TODO : pb : information about topclauses is lost
	public LocalTheory(CFSolver solver, SolProblem cfp, int id) {
		this.solver=solver;
		locTheory=new CNF();
		for (int i=0;i<cfp.getNumClauses();i++){
			locTheory.add(cfp.getClause(i));
		}
		manifestations=new UnitClauseCNF();
		//	uncertainTheory=new ArrayList<Clause>();
		favHypothesis=new FullHypothesis();
		hypothesesField=cfp.getPField();
		hypOrdering=null;
//		extendedAbduciblesField=new IndepPField();
//		extendedHypField=new PField(env);
		refNumber=id;
	}

	
	public LocalTheory(CFSolver solver, List<Clause> rules, UnitClauseCNF obs,
			 IndepPField hfield, IndepPField ofield,
			 Comparator<IndepClause> order, int id) {
		this.solver=solver;
		locTheory=new CNF(rules);
		manifestations=new UnitClauseCNF(obs);
//		uncertainTheory=uncertaintyRule;
		setFavHypothesis(new UnitClauseCNF(),false);
		hypothesesField=hfield;
		hypOrdering=order;
//		extendedAbduciblesField=ofield;
	//	extendedHypField=ufield;
		refNumber=id;
	}

	
	
	//// GETTER, SETTER & DELEGATES (no update fields)
			//// STATS COUNTERS
	public void setStatCounters(List<StatCounter<Integer>> hyp, List<StatCounter<Integer>> hypNC, 
			List<StatCounter<Integer>> extHyp, List<StatCounter<Integer>> extHypNC, List<StatCounter<Integer>> ctx, List<StatCounter<Integer>> ctxNC){
		ctrHyp=hyp;
		ctrHypNC=hypNC;
		ctrExtHyp=extHyp;
		ctrExtHypNC=extHypNC;
		ctrCtx=ctx;
		ctrCtxNC=ctxNC;
	}
		
			//// WEIGHTING
	/**
	 * @param hypOrdering the hypOrdering to set
	 */
	public void setHypOrdering(Comparator<IndepClause> hypOrdering) {
		this.hypOrdering = hypOrdering;
	}
	
			//// PFIELD
	public IndepPField getHypothesesField() {
		return hypothesesField;
	}
	
	public void setHypothesesField(IndepPField hypothesesField) {
		this.hypothesesField = hypothesesField;
	}

/*	public IndepPField getExtendedHypField() {
		return extendedAbduciblesField;
	}

	public void addToExtAbduciblesField(IndepPField eField) throws ParseException {
		extendedAbduciblesField=extendedAbduciblesField.mergeWith(eField);
		//add prefix 'p_' to all Literals if it has been forgotten
		extendedAbduciblesField=extendedAbduciblesField.addPrefix("p_", false);
	}
*/	
	public void setContextLanguage(IndepPField ctxLanguage) {
		contextLanguage=ctxLanguage;
		extHypLanguage=new IndepPField(ctxLanguage.getLiterals());
	}
	
/*	public void addClauseToObservationField(IndepClause clToAdd) throws ParseException{
		//convert clToAdd by adding prefix
		IndepPField pf=new IndepPField(clToAdd.toString());
		pf=pf.addPrefix("p_", false);
		pf=extendedAbduciblesField.addToPLiterals(pf.getPLiterals().toString());
		extendedAbduciblesField=pf;
		//TODO Check if negation also needed;
	} */


		//// THEORY AND MANIFESTATION
	
	public UnitClauseCNF getManifestations(){
		return manifestations;		
	}

	
	/**
	 * @param observations the observations to add
	 * @throws ParseException 
	 */
	public boolean addManifestations(UnitClauseCNF observations, List<StatCounter<Integer>> ctr) throws ParseException {
		boolean modifHyp=false;
		for (IndepClause cl:observations)
			modifHyp=addManifestation(cl,ctr)||modifHyp;
		return modifHyp;
	}
	/**
	 * @param observations the observation to add
	 * @throws ParseException 
	 */
	public boolean addManifestation(IndepClause obs, List<StatCounter<Integer>> ctr) throws ParseException {
		boolean added=manifestations.add(obs);
	//	if (added)
	//		addClauseToObservationField(obs);
		//Test if hypothesis should be modified
		if (added && updatedHyp){
			if (!isConsequence(obs, favHypothesis.getBase(),ctr)){		
				updatedHyp=false;
				//updatedCtx=false; will be changed if hypothesis is since updatedHyp==false
				return true;//hyp modified
			}
			updatedCand=false;
		}
		return false;
	}
	

	/**
	 * @return the locTheory
	 */
	public CNF getTheory() {
		return locTheory;
	}

	/**
	 * @param rules the rules to add
	 * @throws ParseException 
	 */
	public boolean addToTheory(CNF clauses, List<StatCounter<Integer>> ctr, boolean init) throws ParseException {
		boolean added=false;
		if (clauses.isEmpty()) return false;
		clauses.removePrefix("p_");
		System.out.print("Clauses : "+clauses);
		added=locTheory.addAll(clauses);
		if (added) {
			System.out.print(" added to Theory of ag"+this.refNumber);
			if (!init) updateExtHypField(clauses);
			IndepClause negHyp=favHypothesis.getBase().getNegation(true);
			// if fav hyp is partial, test might not be accurate if we do not remove prefix
			negHyp=negHyp.removePrefix("p_");
			if (updatedHyp && 
					(negHyp.isEmpty() || 
							isConsequence(negHyp, new CNF(),ctr))){
				System.out.println(" (hyp needs update)");
				updatedHyp=false;
				updatedCtx=false;
			}
			else {
				System.out.println(" (hyp (neg "+negHyp+") still ok)");
				updatedCand=false;
				favHypothesis.context.removeAllSubsumedbyAny(clauses);
			}		
		}
		else
			System.out.println(" already in theory of ag"+this.refNumber);
		return added;
		//TODO check if hypothesis should be modified
	}
		
	
	public void updateExtHypField(CNF clauses) throws ParseException{
		for (IndepClause cl:clauses)
			for (IndepClause cl2 : cl.getNegation(false)) //check Skolem
				if (!extHypLanguage.belongsTo(cl2))
					extHypLanguage=extHypLanguage.addToLiterals(cl2.getLiterals());
	}
	
	//HYPOTHESES (fav, ctx, conseqSet) 	
	
	/**
	 * @return the favHypothesis
	 */
	public UnitClauseCNF getFavHypothesis() {
		if (!updatedHyp)
			try {
				partialHyp=!computeHypothesisCandidates();
				if (partialHyp)
					computeExtendedHypothesis();      		
				updatedCand=true;
				computeFavHypothesis();
			} catch (Exception e) {e.printStackTrace();}
		return favHypothesis.getBase();
	}
	
	public boolean hasPartialHyp(){
		getFavHypothesis();
		return partialHyp;
	}
	public UnitClauseCNF getFavPartialHypothesis(IndepPField narrowedPField) throws ParseException, FileNotFoundException {
		UnitClauseCNF candidate=getFavHypothesis();
		if (!partialHyp || narrowedPField==null)
			return candidate;
		IndepPField nField=this.getInverseEntailmentExtHypothesisField(narrowedPField);
		if (!candidate.isEmpty() && updatedCand && nField.belongsTo(candidate.getNegation(false).removePrefix("p_")))
			return candidate;
				
		if (!updatedCand){
			partialHyp=!computeHypothesisCandidates();
			if (partialHyp)
				computeExtendedHypothesis();      		
			updatedCand=true;
			computeFavHypothesis();
			if (!partialHyp) return getFavHypothesis();
		}
		return computeFavHypothesis(narrowedPField);
	}
	

	/**
	 * Changes the favorite hypothesis, iff favHyp is different from the current hypothesis
	 * If the hypothesis is change, the context is cleared
	 * @param favHypothesis the favHypothesis to set
	 */
	public boolean setFavHypothesis(UnitClauseCNF favHyp,boolean internal) {
		boolean unchanged=CNF.isEquiv(favHypothesis.getBase(),favHyp);
		if (!unchanged){
			favHypothesis.setHypothesis(favHyp);
			try {
				partialHyp=!hypothesesField.belongsTo(new IndepClause(favHyp.getLiterals().toString()));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			resetContext();
			updatedCtx=false;
			updatedCand=internal;
		}
		updatedHyp=true;
		return !unchanged;
	}

	/**
	 * @return the context
	 */
	public CNF getContext() {
		// TODO delete getContext(), and never get context without the hypothesis.
		if (!updatedCtx)
			try {
				computeContext();
				updatedCtx=true;
			} catch (Exception e) {e.printStackTrace();}
		return favHypothesis.getContext();
		
	}
	
	private void resetContext(){
		if (favHypothesis!=null)
			favHypothesis.removeCtx();
//		lastAdoptedCtxComputedWith=null;
	}

	/**
	 * if the hyp is new, adopt it and its context (to be extended afterwards)
	 * Otherwise, just add ctx to the context (to be extended afterwards)
	 */
	public void adoptHypWithContext(UnitClauseCNF hyp, CNF ctx, boolean ownHyp,IndepPField producedWith){
		//TODO problem : add or not if !updatedCtx ?
		// depends on the reason for !updatedCtx
		// if this reason could remove part of the Ctx, then need for reset/computation
		// otherwise, ok if hyp did not change
		//Correct solution : add only if this context concern same hypothesis
		//			and theory do not decrease (or in a controled way that cannot decrease ctx)
		boolean changedHyp = setFavHypothesis(hyp,ownHyp);
		// context should only be updated if hypothesis changed or if context changed
		updatedCtx=!favHypothesis.addCtx(ctx) && !changedHyp;
		//if (!updatedCtx) lastAdoptedCtxComputedWith=producedWith;
	}

	//// INDIRECT FIELDS AND PROPERTIES

		//// COMPOSED PRODUCTION FIELDS
	
	public IndepPField getInverseEntailmentHypothesisField() throws ParseException{
		// Generate extended hypothesis field
		List <IndepLiteral> negLits=new ArrayList<IndepLiteral>();
		for (IndepLiteral plit : hypothesesField.getLiterals()) 
			negLits.add(plit.negate(false));
		//use maxLength and maxTermDepth from hypotheses field
		return new IndepPField(negLits,hypothesesField.getGbConditions());
	}
	
	public IndepPField getInverseEntailmentExtHypothesisField(IndepPField narrowedExtPF) throws ParseException{
		// Generate extended hypothesis field
		List <IndepLiteral> negLits=new ArrayList<IndepLiteral>();
		for (IndepLiteral plit : hypothesesField.getLiterals()) 
			negLits.add(plit.negate(false));
		for (IndepLiteral plit : narrowedExtPF.getLiterals()) { 
			negLits.add(plit.negate(false));
		}
		//also need to add manifestations
		for (IndepClause cl : manifestations){
			negLits.add(cl.getLiterals().get(0).negate(false));
		}
		//use maxLength and maxTermDepth from argument PField
		return new IndepPField(negLits,narrowedExtPF.getGbConditions());
	}
	
	
	public IndepPField getManifPF() throws ParseException{
	//	String pfRepr=getManifestations().getLiterals()+"<=1";
		List<IndepLiteral> lits=getManifestations().getLiterals();
		return new IndepPField(lits,-1,1);
	}

	public boolean isPartialHyp(UnitClauseCNF hyp) throws ParseException{
		Env env=new Env();
		//add the literals symbol to env before the checker is built, otherwise the checker does not work !!
		List<Literal> list=hyp.getLiterals(env);
		IndepClause clHyp=new IndepClause(list.toString());
		// The hyp is partial iff the hyp is not in hypothesis field (not extended)
		return !hypothesesField.belongsTo(clHyp);
//		PFieldChecker checker=new PFieldCheckerWithoutSubst(env,hypothesesField.toPField(env));
//		for (Literal lit : list){
//			if (checker.belongs(lit)) 
//				checker.skip(lit);
//			else return true;
//		}
//		checker=null;
//		return false;
	}
	
	//note preferred or equal
	public boolean isPreferredToOwnHyp(UnitClauseCNF hyp) throws ParseException{
		// Note: the ordering is indepent from negation and hypOrdering compare IndepClause
		
		int comp=hypOrdering.compare(hyp.getNegation(false),
				getFavHypothesis().getNegation(false));
		return (comp<=0);
	}

		//// EXTENDED RULES
	
/*	public CNF getExtendedRules() throws ParseException{
		CNF result=new CNF();
		for (String extendedLit:getExtendedHypField().getPLiterals()){
			String original=IndepPLiteral.removePrefixFrom("p_", extendedLit);
			result.addAll(IndepClause.newEntailmentRule(extendedLit, original));
		}
		return result;
	}
*/
	public CNF getExtendedRules(List<? extends IndepClause> input) throws ParseException{
		CNF result=new CNF();
		//notTODO getSumbsumptionMinimalLiterals input to avoid create unnecessary rules
		// though maybe the test is addAll is enough as 
		//		if p_lit1 subsumes p_lit2, we also have [-p_lit1,lit1] subsumes [-p_lit2,lit2] 
		for (IndepClause cl:input)
			for (IndepLiteral lit:cl.getLiterals()){
				String term=lit.getTerm();
				if (term.startsWith("p_")){
					IndepLiteral original=lit.removePrefix("p_");
					result.add(IndepClause.newEntailmentRule("ext_rule",
							lit.toUnitClauseCNF(), original.toIndClause()));
				}			
		}
		return result;
	}
	
	//// BLOCKING AND UNBLOCKING OF UNIT CLAUSES
	public void blockHyp(UnitClauseCNF hypo,boolean temporary,
			List<StatCounter<Integer>> theoryCtr) throws ParseException {
		//IndepClause blocking=hypo.getNegation();
		hypo.removePrefix("p_");
		if (temporary){ //TODO remove prefixe
			if (temporaryBlocks.addAll(hypo)) {
				if (!isBlocked(getFavHypothesis())) 
					return;
				//otherwise, if candidateHyp is valid, no need to recompute hypotheses,
				// only need to change favorite one
				if (updatedCand){
					try {
						computeFavHypothesis();
						
					} catch (ParseException e) {e.printStackTrace();}					
				}
				else 
					updatedHyp=false;
				updatedCtx=false;
			}
		}
		else {
			IndepClause blocking=hypo.getNegation(true); //TODO UNSKOLEMIZE !!!
			addToTheory(CNF.singleCNF(blocking),theoryCtr, false);
		}
	}
	
	public boolean isBlocked(UnitClauseCNF hyp){
		CNF tempHyp=new CNF();
		try {
			tempHyp.addAll(hyp);
			tempHyp.removePrefix("p_");
			tempHyp.removeAllSubsumedbyAny(temporaryBlocks);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return tempHyp.isEmpty();		
	}
	
	public void unblockAllHyp() {
		if (!temporaryBlocks.isEmpty()){
			temporaryBlocks.clear();
			try {
				if (updatedCand)
					computeFavHypothesis();
				else 
					updatedHyp=false;
			} catch (ParseException e) {e.printStackTrace();}
			// if hypothesis change, setFav will change Ctx
		}
	}	

	
	////	TOOLS FOR COMPUTATIONS
	
	private int solve(List<? extends IndepClause> axioms, List<? extends IndepClause> topClauses, 
			IndepPField pf,List<StatCounter<Integer>> ctr, List<Conseq> conseqSet) throws ParseException{
		int status=ExitStatus.SATISFIABLE;
		List<Clause> top=new ArrayList<Clause>();
		top.addAll(topClauses);
		List<IndepClause> base=new ArrayList<IndepClause>();
		base.addAll(axioms);
		if (topClauses.isEmpty()){
			top=base;
			base=new ArrayList<IndepClause>();
		}
		//int k=top.size();
		for (int i=0;i<top.size();i++) {
			Clause cl=top.get(i);
			Env env=new Env();
			CFP problem=set_Problem(env,base,CNF.singleCNF(cl),pf);
			CFSolver.solve(env, problem,depthLimit,-1, ctr);
			problem.getConseqSet().validate();
		//	if (problem.getStatus()==ExitStatus.UNKNOWN && i<k){
		//		top.add(cl); continue;
		//	}
			List<Conseq> tempRes=problem.getConseqSet().get();
			conseqSet.addAll(tempRes);
			if (problem.getStatus()==ExitStatus.UNSATISFIABLE){
				conseqSet.retainAll(tempRes); 
				status=ExitStatus.UNSATISFIABLE;
				return status;
			}
			if (problem.getStatus()==ExitStatus.UNKNOWN)
				status=ExitStatus.UNKNOWN;
			base.add(cl);
		}
		return status; 
	}
	
	
	protected boolean newCarc(List<? extends IndepClause> suppAxioms, 
							List<? extends IndepClause> topClauses, IndepPField pf,
							int newCType, int refutType, UnitClauseCNF hypToKeep, 
							List<StatCounter<Integer>> ctr,List<StatCounter<Integer>> ctrNC,
							CNF result) throws FileNotFoundException, ParseException {
		List<Conseq> conseqSet=new ArrayList<Conseq>();
		CNF axioms=getAxioms(suppAxioms);
		// Solve
		if (topClauses.isEmpty()) { // newcarc of [] is always [] 
			result.clear();
			return true;
		}
		int status=solve(axioms, topClauses, pf, ctr, conseqSet);
	    boolean sat=status!=ExitStatus.UNSATISFIABLE;
	    if (status==103) System.out.println("Computing newcarc - Result Unknown !!!!!!!)");
	   	
	    if (!sat){
	    	getProof(conseqSet,result,refutType,topClauses,hypToKeep,new IndepClause("[]"));
	    }
	    else { 
	    	CNF cons=new CNF(conseqSet);
	    	// get only newcarc to avoid putting already observed data as context 
	    	cons=this.pruneNewCarc(newCType, suppAxioms, cons,ctrNC);
	    	result.clear();
	    	result.addAll(cons);
	    }
	    return sat;
	}

	private boolean proveClause(List<? extends IndepClause> suppAxioms, 
			List<? extends IndepClause> topClauses,
			IndepClause toProve,
			int refutType, List<StatCounter<Integer>> ctr,
			CNF result) throws FileNotFoundException, ParseException {
		
		boolean proved;
		// Set problem
		IndepPField pf=new IndepPField(toProve.getLiterals(),-1,toProve.getLiterals().size());

		List<Conseq> conseqSet=new ArrayList<Conseq>();
		CNF axioms=getAxioms(suppAxioms);
		// Solve
		int status=solve(axioms, topClauses, pf, ctr, conseqSet);
		boolean sat=status!=ExitStatus.UNSATISFIABLE;
	    if (status==103) System.out.println("Computing proof - Result Unknown !!!!!!!)");
	   	
	    if (!sat){
	    	getProof(conseqSet,result,refutType,topClauses,null,new IndepClause("[]"));
	    	proved=(toProve.isEmpty());
	    }
	    else { 
	    	getProof(conseqSet,result,refutType,topClauses,null,toProve);
	    	proved=!result.isEmpty();
	    }
	    return proved;
	}

	private CNF getAxioms(List<? extends IndepClause> suppAxioms) throws ParseException{
		//initialize options and problem 
		CNF axioms=CNF.copy(locTheory);
		axioms.addAll(suppAxioms);
		return axioms;
	}
	
	private CFP set_Problem(Env env,
			List<? extends Clause> axioms, 
			List<? extends Clause> topClauses,
			IndepPField pf) throws ParseException{
		//initialize options and problem 
		Options opt=new Options(env);
		opt.setDepthLimit(depthLimit);
		opt.setUsedClausesOp(true);
		CFP problem= new CFP(env, opt);
		for (Clause cl : axioms)
			problem.addClause(cl);
		for (Clause cl : topClauses)
			problem.addClause(cl);
//		System.out.println("Parsing pf : "+pf);
		if (pf!=null) problem.setPField(pf.toPField(env,opt));
		
		return problem;
	}
	
	private void getProof(List<? extends Conseq> concl, CNF result, int refutType,
			List<? extends IndepClause> topClauses,UnitClauseCNF hypToKeep,
			IndepClause target) throws ParseException{
		result.clear();
		//get correct consequence and set its used clause
		Conseq targetCons=null;
	    for (int i=0;i<concl.size();i++){	    	
	    	IndepClause c=new IndepClause(concl.get(0));
	    	if (IndepClause.isEquiv(c, target)){
	    		targetCons=concl.get(i);
	    		break;
	    	}	    		
	    }
	    if (targetCons==null) return;
	    List<Clause> usedCl=targetCons.getUsedClauses();
	    CNF proof=new CNF(usedCl);
	    //prune used clauses
	    proof.supprRoot();
	    if (refutType==PUT_DISCARD_TOP_CLAUSES)
	    	proof.removeAllSubsumedbyAny(topClauses);
	    if (refutType==PUT_KEEP_TOP_CLAUSES)
	    	proof.retainAll(topClauses);
	    if (refutType==PUT_KEEP_HYPOTHESIS)
	    	proof.retainAll(hypToKeep);
	    //update result	    	
	    result.addAll(proof);
	}

		//PUBLIC TOOLS
	
	//get newcarc from a solved cfp
	public CNF pruneNewCarc(int newCType, 
			List<? extends IndepClause> suppAxioms,
			List<? extends IndepClause> conseq,
			List<StatCounter<Integer>> ctr) throws FileNotFoundException, ParseException{
		if (verbose)
			System.out.println("               ag"+refNumber+" : pruneNewCarc("+newCType+","+suppAxioms+","+conseq+")");
		
		//List<Clause> conseq=Tools.dataCopyCl(problem.getConseqSet().get(), env);
		CNF suppAxioms2=new CNF();
    	if (newCType==NCT_NEWC_WRT_FULLAXIOMS) {
    		suppAxioms2.addAll(suppAxioms);
    	}
    	CNF newcarc = new CNF();
    	if (newCType>0) {    		
    		for (IndepClause ncl : conseq){
    			if (!isConsequence(ncl,suppAxioms2,ctr))
    				newcarc.add(ncl);
    		}
    	}
    	else newcarc.addAll(conseq);
        
    	return newcarc;
	}

	public boolean isConsequence(IndepClause goal, CNF suppAxioms, List<StatCounter<Integer>> ctr) throws ParseException{
		CNF topClauses=new CNF();
		topClauses.addAll(goal.getNegation(true));
		
		List<Conseq> conseqSet=new ArrayList<Conseq>();
		
		int status=solve(getAxioms(suppAxioms), topClauses, null, ctr, conseqSet);
		
		return (status==ExitStatus.UNSATISFIABLE);
	}
	
	//COMPUTATIONS
	
			// Candidate Hypotheses
	//Compute local hypothesis
	//return true if a complete hyp can be found
	public boolean computeHypothesisCandidates() throws FileNotFoundException, ParseException {		
		if (verbose)
			System.out.println("               ag"+refNumber+" : computeHypothesis()");
		
		// no need for hypothesis if there is nothing to explain
		if (getManifestations().isEmpty()){
			candidateNegHypotheses=new CNF();
			return true;
		}
		//define topClauses as negations of manifestations
		CNF topClauses=new CNF();
		topClauses.add(getManifestations().getNegation(true));
		CNF suppAxioms=new CNF();
		//CNF.copy(temporaryBlocks);
		CNF result=new CNF();
		boolean sat=newCarc(suppAxioms, topClauses, 
				getInverseEntailmentHypothesisField(), 
				NCT_NEWC_WRT_FULLAXIOMS, PUT_KEEP_TOP_CLAUSES, null, ctrHyp, ctrHypNC, result);
		
		if (!sat){
			System.out.println("Inconsistent Manifestations: "+result);
			return false;
		}
		
		candidateNegHypotheses = result;      
        return (!result.isEmpty()); 
	}
	

	//Compute local hypothesis
	public CNF computeExtendedHypothesis() throws ParseException, FileNotFoundException {
		
		if (verbose)
			System.out.println("               ag"+refNumber+" : computeExtendedHypothesis()");
		
		CNF topClauses=new CNF();
		topClauses.add(getManifestations().getNegation(true));
		CNF result=new CNF();
		
		IndepPField pf=extHypLanguage;
	//	if (verbose) System.out.println("               ----- language "+pf);
		if (!newCarc(new CNF(), topClauses, getInverseEntailmentExtHypothesisField(pf), 
					NCT_NEWC_WRT_LOCTHEO,PUT_KEEP_TOP_CLAUSES, null, ctrExtHyp, ctrExtHypNC, result)){
				System.out.println("Inconsistent Manifestations in extended: "+result);
				return null;
		}
		
    	candidateNegHypotheses=result;
        return result;
	}
	
			//Favorite Hypothesis

	// compute favoriteHypothesis from a ConseqSet
	public UnitClauseCNF computeFavHypothesis() throws ParseException{
		
		UnitClauseCNF result = computeFavHypothesis(null);
		//setFavHypothesis(result,true);
		return result;
	}

	public UnitClauseCNF computeFavHypothesis(IndepPField negWithin) throws ParseException{
		
		UnitClauseCNF result = new UnitClauseCNF();
		List<IndepLiteral> negLits=new ArrayList<IndepLiteral>();
		for (IndepClause cl : manifestations){
			negLits.add(cl.getLiterals().get(0).negate(true));
		}
		IndepPField fullNegWithin;
		if (negWithin==null)
			fullNegWithin=null;
		else
			fullNegWithin=negWithin.addToLiterals(negLits);	
		if (!candidateNegHypotheses.isEmpty()) {
			Collections.sort(candidateNegHypotheses, hypOrdering);
			//leave out hypothesis that are blocked and take the first non blocked one
			for (IndepClause cl:candidateNegHypotheses){
				// favHypothesis = negation of consequence (IE)
				UnitClauseCNF hyp=cl.getNegation(true);
				if (!isBlocked(hyp) && 
					(negWithin==null //|| fullNegWithin.partlyBelongsTo(cl) 
							|| hasNewElem(hyp,fullNegWithin))){
					result=hyp;
					break;
				}
			}			
		}
		result=identifyPartialParts(result);
		setFavHypothesis(result,true);
		return result;
	}

	// return true if either hyp has a non-blocked abducible or a non-blocked element of focus
	public boolean hasNewElem(UnitClauseCNF hyp, IndepPField negFocus) throws ParseException{
		for (IndepClause cl:hyp){
			UnitClauseCNF subhyp=new UnitClauseCNF();
			subhyp.add(cl);
			if ((//hypothesesField.belongsTo(cl) || 
					(negFocus.belongsTo(subhyp.getNegation(false)))) //TODO check 
					&& (!isBlocked(subhyp)))
				return true;
		}
		return false;
	}
	
	public UnitClauseCNF identifyPartialParts(UnitClauseCNF hyp) throws ParseException{
		UnitClauseCNF reformedClause=new UnitClauseCNF();
		for (IndepLiteral part : hyp.getLiterals()){
			if (!hypothesesField.belongsTo(new IndepClause("["+part+"]")))
				reformedClause.add(
						new IndepClause("["+part.addPrefix("p_", false)+"]"));
			else
				reformedClause.add(new IndepClause("["+part+"]"));
		}
		return reformedClause;
	}
	
			//Context (own or as critic)

	public CNF computeContext() throws FileNotFoundException, ParseException {
		CNF result=new CNF();
		CNF topClauses=CNF.copy(getFavHypothesis()); 
		//getFavHyp will also have updated hypothesis and cleared ctx if needed
		topClauses.addAll(favHypothesis.getContext());
		
		if (verbose)
			System.out.println("               ag"+refNumber+" : ComputeContext()/ topClauses:"+topClauses);
		IndepPField pf=contextLanguage;
		List<IndepLiteral> litList=pf.getLiterals();
		for (IndepClause cl : favHypothesis.getContext())
				litList.addAll(cl.getLiterals());
		pf=pf.addToLiterals(litList);

		//	if (pf.sameString(lastAdoptedCtxComputedWith)) {
		//		result.addAll(favHypothesis.getContext());continue;}
			if (!newCarc(getExtendedRules(getFavHypothesis()), topClauses, pf, 
					NCT_NEWC_WRT_LOCTHEO,PUT_KEEP_TOP_CLAUSES, null, ctrCtx, ctrCtxNC, result)){
				System.out.println("Inconsistency of fav hyp a compute ctx : "+result);
				return null;
			}
			// do not need to add temp to topClauses 
			// 			as any temp is already a consequence of axoms+topClauses 
		
		result.removeAllSubsumedbyAny(getFavHypothesis());
		favHypothesis.setContext(result);
		return result;
	}
	

	public boolean computeContext(List<? extends IndepClause> suppAxioms, 
			List<? extends IndepClause> topClauses, IndepPField pf, 
			List<StatCounter<Integer>> ctr, List<StatCounter<Integer>> ctrNC, boolean prove, UnitClauseCNF hyp, 
			CNF result) throws FileNotFoundException, ParseException {
		if (verbose)
			System.out.println("               ag"+refNumber+" : computeContext("+suppAxioms+","+topClauses+","+pf+",[])");
	
		List<IndepClause> sAxioms=new ArrayList<IndepClause>();
		sAxioms.addAll(suppAxioms);
		CNF temp=new CNF();
		temp.addAll(suppAxioms);
		temp.addAll(topClauses);
		sAxioms.addAll(getExtendedRules(temp));
		int typeProof;
		if (prove) typeProof=PUT_DISCARD_TOP_CLAUSES;
		else typeProof=PUT_KEEP_HYPOTHESIS;
		return newCarc(sAxioms,topClauses,pf,
				NCT_NEWC_WRT_LOCTHEO,typeProof,hyp,ctr, ctrNC,result);
	}

			// Tests & Arguments (proofs and refutations)
	
	public CNF getCoveredManif(UnitClauseCNF hypothesis, List<StatCounter<Integer>> ctr) throws FileNotFoundException, ParseException {
		if (verbose)
			System.out.println("               ag"+refNumber+" : getCoveredManif("+hypothesis+"), manifs:"+getManifestations());
		
		if (getManifestations().isEmpty()) return getManifestations();
		CNF result=new CNF();
		if (!this.newCarc(getExtendedRules(hypothesis), hypothesis,getManifPF(), 
								 NCT_NO_FILTER, PUT_KEEP_TOP_CLAUSES, hypothesis, ctr, null, result)){
			System.out.println("Inconsistent hypothesis during get covered Manif : "+result);
			return null;
		}
		return result;
	}

	public boolean proveManif(List<? extends IndepClause> hypothesis, 
			IndepClause manif, boolean fullExplain, 
			List<StatCounter<Integer>> ctr, CNF proof) throws FileNotFoundException, ParseException {

		if (verbose)
			System.out.println("               ag"+refNumber+" : proveManif("+hypothesis+","+manif+","+proof+")");
	    int proofType=PUT_KEEP_TOP_CLAUSES;
	    if (fullExplain) proofType=PUT_DISCARD_TOP_CLAUSES;
		boolean proved=proveClause(getExtendedRules(hypothesis), hypothesis, manif, 
											proofType, ctr, proof);
		if (proved && !fullExplain){
			//since hypothesis is unitCNF, with keep-top-clause, proof is also unitCNF
			IndepClause empiricProof=
				IndepClause.newEntailmentRule("emp_proof",proof.retainUnitClauses(), manif);
			proof.clear();
			proof.add(empiricProof);
		}
		return proved;
	}
	

	public boolean refute(List<? extends IndepClause> hypothesis, List<StatCounter<Integer>> ctr, CNF refut) 
												throws FileNotFoundException, ParseException {
		if (verbose)
			System.out.println("               ag"+refNumber+" : refute("+hypothesis+","+refut+")");
		
		boolean proved=proveClause(getExtendedRules(hypothesis), hypothesis, new IndepClause("[]"),
											PUT_DISCARD_TOP_CLAUSES, ctr, refut);
		return proved;
	}
	
		//// IMPLEMENTATION OF OBJECT METHODS	
	
	public String toString(){
		String res="T : R =\n";
		for (IndepClause cl:locTheory)
			res+=cl+"\n";
		res+="M = ";
		for (IndepClause m:manifestations)
			res+=m+"\n";
		res+="h = ";
		if (!updatedHyp) res+="(not updated)  ";
		res+=favHypothesis.getBase()+"\n Ctx = ";
		if (!updatedCtx) res+="(not updated)  ";			
		for (IndepClause ctx:favHypothesis.getContext())
				res+=ctx+"\n";
		
		return res;
	}
	
	
			////VARIABLES
		
	/** individual theory of an agent */
	private CNF locTheory;
	/** individual observations of an agent that should be explained*/
	private UnitClauseCNF manifestations;
	/** Candidate hypotheses (negated) 
	 * The set of new consequences of the local theory with the negated manifestations. */
	private CNF candidateNegHypotheses = null;
	/** Favored Hypothesis and its context */
	private FullHypothesis favHypothesis;
	///** Context of the favored hypothesis : 
	// * observations that should also be true if the hypothesis is true, 
	// * but that have not yet been confirmed. 
	// */
	//private CNF temporaryBlocks=new CNF(); 
	private UnitClauseCNF temporaryBlocks=new UnitClauseCNF(); 
	
	
	/** hypotheses specification */
	private IndepPField hypothesesField;
	/** specification for partial hyp
	 * contains extended literals (prefixed by 'p_' for
	 * 1. literals that are shared with other agents (to allow collaborative search for real hyp)
	 * 2. known manifestations (to ensure a partial hypothesis can be produced) */
//	private IndepPField extendedAbduciblesField;
		
	protected int refNumber;
	
	protected CFSolver solver;
	protected int depthLimit=8;
	
	protected Comparator<IndepClause> hypOrdering;

//	protected DiagStats ds; //needed for counting computations of solve
	
	protected boolean updatedCand=false;
	protected boolean updatedHyp=false;
	protected boolean updatedCtx=false;
//	protected IndepPField lastAdoptedCtxComputedWith=null;
	protected boolean partialHyp=false;
	
	protected IndepPField contextLanguage=new IndepPField();
	protected IndepPField extHypLanguage=new IndepPField();
	
	public static boolean verbose=true;
	
	public List<StatCounter<Integer>> ctrHyp;
	public List<StatCounter<Integer>> ctrHypNC;
	public List<StatCounter<Integer>> ctrExtHyp;
	public List<StatCounter<Integer>> ctrExtHypNC;
	public List<StatCounter<Integer>> ctrCtx;
	public List<StatCounter<Integer>> ctrCtxNC;
}
	 	//TODO ensures contextLanguages is initialized
