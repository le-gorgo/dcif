package solarInterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import logicLanguage.CNF;
import logicLanguage.IndepClause;
import logicLanguage.IndepLiteral;

import org.nabelab.solar.Clause;
import org.nabelab.solar.Env;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Options;
import org.nabelab.solar.PLiteral;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.pfield.PFCardConstraint;
import org.nabelab.solar.pfield.PField;

import genLib.tools.Pair;
import genLib.tools.ToolIndex;



public class IndepPField {
	
	public static class PFCond{
		public PFCond(){
			super();
		}
		
		public PFCond(int maxDepth, int maxLength){
			this.maxDepth=maxDepth;
			this.maxLength=maxLength;
		}
		// if constraint, then favor more constraining value
		public static PFCond merge(PFCond c1, PFCond c2, boolean constrain){
			int maxD=c2.maxDepth;
			if (ToolIndex.isBefore(c1.maxDepth, c2.maxDepth) && constrain)
				maxD=c1.maxDepth;
			else if (ToolIndex.isBefore(c2.maxDepth, c1.maxDepth) && !constrain)
				maxD=c2.maxDepth;
			int maxL=c2.maxLength;
			if (ToolIndex.isBefore(c1.maxLength, c2.maxLength) && constrain)
				maxL=c1.maxLength;
			else if (ToolIndex.isBefore(c2.maxLength, c1.maxLength) && !constrain)
				maxD=c2.maxLength;
			return new PFCond(maxD,maxL);			
		}
		
		public String toString(){
			String res="";
			if (maxDepth>-1)
				res+=":"+maxDepth;
			if (maxLength>-1)
				res+="<="+maxLength;
			return res;	
		}
		protected int maxDepth=-1;
		protected int maxLength=-1;
	}

	/*
	public IndepPField(){
		super();
	}
	
	public IndepPField(Collection<IndepLiteral> literals){
		super();
		for (IndepLiteral lit:literals)
			addLiteral(lit);
	}
	
	public IndepPField(List<IndepLiteral> literals, List<PFCond> localCond){
		super();
		for (int i=0;i<literals.size();i++) {
			IndepLiteral lit=literals.get(i);
			if (localCond!=null && i<localCond.size())
				addLiteral(lit,localCond.get(i));
			else 
				addLiteral(lit);
		}
	}
	
	public IndepPField(List<IndepLiteral> literals, List<PFCond> localCond, PFCond gbCond){
		this(literals, localCond);
		globalConditions=gbCond;
	}
	
	public IndepPField(List<IndepLiteral> literals, PFCond cond){
		this(literals);
		globalConditions=cond;
	}
	
	public IndepPField(List<IndepLiteral> literals, int maxDepth, int maxLength){
		this(literals);
		globalConditions=new PFCond(maxDepth,maxLength);
	}*/
	
	/*
	public static IndepPField parse(String repr){
		if (repr==null || repr.equals("")) {
			return new IndepPField();
		}
		if (repr.startsWith("pf("))
			repr=repr.substring(3,repr.lastIndexOf(')'));
		IndepPField res=new IndepPField();
		try {
			Env env=new Env();
			Options opt=new Options(env);
			PField pf=PField.parse(env, opt, "pf("+repr+").");
			for (PLiteral plit:pf.getPLiterals()){
				int maxLength=plit.getMaxLength();
				int maxDepth=plit.getMaxTermDepth();
				IndepLiteral lit=IndepLiteral.parse(plit.getTerm().toString()); 
				if (plit.getSign()==PLiteral.BOTH || plit.getSign()==PLiteral.POS)
					res.addLiteral(lit,maxDepth,maxLength);
				if (plit.getSign()==PLiteral.BOTH || plit.getSign()==PLiteral.NEG)
					res.addLiteral(lit.negate(false),maxDepth,maxLength);
			}
			res.globalConditions=new PFCond(pf.getMaxTermDepth(),pf.getMaxLength());			
		} catch (ParseException e) {
			System.out.println("ParseException during parsing of IndepPField : "+repr);
			e.printStackTrace();
		}
		return res;
	}
	*/
	
	/*
	// no access to modifications -> read only
	public List<IndepLiteral> getLiterals(){
		List<IndepLiteral> lits=new ArrayList<IndepLiteral>(literals);
		return lits;
	}*/
	
	/*
	public int getMaxTermDepth(){
		return globalConditions.maxDepth;
	}
	public int getMaxLength(){
		return globalConditions.maxLength;
	}
	
	public PFCond getGbConditions(){
		return globalConditions;
	}*/
	//subsumtion check ???
	
	/**Converts a Literal into a PLiteral and adds it to a PField.
	 * @param pf The Production Field.
	 * @param lit The Literal.
	 */
	public static void addLiteral(PField pf, Literal lit) {
		addLiteral(pf, lit, PLiteral.NOT_DEFINED, PLiteral.NOT_DEFINED);
	}
	
	/**Converts a Literal into a PLiteral and adds it to a PField.
	 * @param pf The Production Field.
	 * @param lit The Literal.
	 * @param maxDepth The maximum term depth.
	 * @param maxLength The maximum length.
	 */
	public static void addLiteral(PField pf, Literal lit, int maxDepth, int maxLength){
		pf.add(toPLiteral(lit, maxDepth, maxLength));
	}
	
	/**Turns a Literal into a PLiteral.
	 * @param lit The Literal.
	 * @return The converted PLiteral.*/
	public static PLiteral toPLiteral(Literal lit) {
		return toPLiteral(lit, PLiteral.NOT_DEFINED, PLiteral.NOT_DEFINED);
	}
	
	/**Turns a Literal into a PLiteral.
	 * @param lit The Literal.
	 * @param maxDepth The maximum term depth.
	 * @param maxLength The maximum length.
	 * @return The converted PLiteral.
	 */
	public static PLiteral toPLiteral(Literal lit, int maxDepth, int maxLength) {
		PLiteral plit = null;
		if(lit.isPositive())
			plit = new PLiteral(PLiteral.POS, lit.getTerm());
		else
			plit = new PLiteral(PLiteral.NEG, lit.getTerm());
		if(maxLength != PLiteral.NOT_DEFINED) plit.setMaxLength(maxLength);
		if(maxDepth != PLiteral.NOT_DEFINED) plit.setMaxTermDepth(maxDepth);
		return plit;
	}
	
	public static Literal toLiteral(PLiteral plit) {
		return toLiteral(new Env(), plit);
	}
	
	public static List<PLiteral> toPLiterals(List<Literal> lits){
		List<PLiteral> plits = new ArrayList<PLiteral>();
		for(Literal lit:lits)
			plits.add(toPLiteral(lit));
		return plits;
	}
	
	public static List<PLiteral> toPLiterals(List<Literal> lits, int[] maxDepths, int[] maxLengths){
		List<PLiteral> plits = new ArrayList<PLiteral>();
		for(int i = 0; i < lits.size(); i++)
			plits.add(toPLiteral(lits.get(i), maxDepths[i], maxLengths[i]));
		return plits;
	}
	
	public static Literal toLiteral(Env env, PLiteral plit) {
		return new Literal(env, !(plit.getSign() == PLiteral.NEG), plit.getTerm());
	}
	
	public static List<Literal> toLiterals(List<PLiteral> plits){
		return toLiterals(new Env(), plits);
	}
	
	public static List<Literal> toLiterals(Env env, List<PLiteral> plits){
		List<Literal> lits = new ArrayList<Literal>();
		for(PLiteral pl:plits)
			lits.add(toLiteral(env, pl));
		return lits;
	}
	
	public static PField createPField(Env env, Options opt, List<? extends Object> lits){
		PField pf = new PField(env, opt);
		for(Object lit:lits){
			if(lit instanceof PLiteral)
				pf.add((PLiteral) lit);
			else if(lit instanceof Literal)
				pf.add(toPLiteral((Literal) lit));
		}
		return pf;
	}
	
	public static PField copyPField(Env env, Options opt, PField pf){
		PField newpf = createPField(env, opt, pf.getPLiterals());
		newpf.setMaxLength(pf.getMaxLength());
		newpf.setMaxTermDepth(pf.getMaxTermDepth());
		if (pf.getAddConstraints() != null)
			for (PFCardConstraint constraint : pf.getAddConstraints())
				newpf.addConstraint(constraint);
		return newpf;
	}
	
	public static PLiteral negate(PLiteral plit){
		PLiteral negated = new PLiteral(plit);
		negated.negate();
		return negated;
	}
	
	/*
	protected void addLiteral(IndepLiteral lit, PFCond cond){
		literals.add(lit);
		localConditions.add(new PFCond(cond.maxDepth, cond.maxLength));
	}
	
	protected void addLiteral(IndepLiteral lit){
		addLiteral(lit,new PFCond());
	}*/
	
	
	// newPLiterals must begin by "[" and end by"]"
//	public IndepPField replacePLiterals(String newPLiterals) throws ParseException{
//		if (newPLiterals==null || newPLiterals.startsWith("[[") || !newPLiterals.startsWith("["))
//			throw new ParseException("Incorrect argument in replacePLiterals("+newPLiterals+") : must be of the form [plit0,plit1,...]");
//		String suffix=strRepr.substring(strRepr.lastIndexOf(']')+1);
//		return new IndepPField(newPLiterals+suffix);
//	}
	
	public static String getPredicate(Env env, PLiteral literal){
		return env.getSymTable().get(literal.getName(), literal.getTerm().getStartType());
	}
	
	/**Adds a prefix to a production literal's predicate.
	 * @param env The environment.
	 * @param prefix Prefix to be added.
	 * @param cumul Prefix will be added on top of previous ones if true.
	 * @param literal The literal.
	 * @throws ParseException*/
	public static PLiteral addPrefix(Env env, String prefix, boolean cumul, PLiteral literal) throws ParseException{
		return addPrefix(env, new Options(env), prefix, cumul, literal);
	}
	
	/**Adds a prefix to a production literal's predicate.
	 * @param env The environment.
	 * @param prefix Prefix to be added.
	 * @param cumul Prefix will be added on top of previous ones if true.
	 * @param literal The literal.
	 * @throws ParseException*/
	public static PLiteral addPrefix(Env env, Options opt, String prefix, boolean cumul, PLiteral literal) throws ParseException{
		String pred=getPredicate(env, literal);
		if (cumul || !pred.startsWith(prefix))
				pred=prefix+pred;
		if(literal.getSign() == PLiteral.POS)
			pred = "+"+pred;
		else if(literal.getSign() == PLiteral.NEG)
			pred = "-"+pred;
		else if(literal.getSign() == PLiteral.BOTH)
			pred = "+-"+pred;
		return PLiteral.parse(env, opt, pred);
	}
	
	public static PField addPrefix(Env env, PField pf, String prefix, boolean cumul) throws ParseException{
		return addPrefix(env, new Options(env), pf,  prefix,  cumul);
	}
	
	public static PField addPrefix(Env env, Options opt, PField pf, String prefix, boolean cumul) throws ParseException{
		List<PLiteral> newLits=new ArrayList<PLiteral>();
		for (PLiteral lit:pf.getPLiterals()){
			//add the new literal (possibily unchanged)
			PLiteral pl = addPrefix(env, prefix, cumul, lit);
			pl.setMaxLength(lit.getMaxLength());
			pl.setMaxTermDepth(lit.getMaxTermDepth());
			newLits.add(pl);
		}
		//Options opt = new Options(env);
		PField newpf = createPField(env, opt, newLits);
		newpf.setMaxLength(pf.getMaxLength());
		newpf.setMaxTermDepth(pf.getMaxTermDepth());
		return newpf;
	}
	
	public static PLiteral removePrefix(Env env, PLiteral pl, String prefix) throws ParseException{
		return removePrefix( env, new Options(env), pl, prefix);
	}
	
	public static PLiteral removePrefix(Env env, Options opt, PLiteral pl, String prefix) throws ParseException{
		String newPred;
		String pred=getPredicate(env, pl);
		if (pred.startsWith(prefix))
			newPred=pred.substring(prefix.length());
		else
			newPred=pred;
		return PLiteral.parse(env, opt, newPred);
	}
	
	public static PField removePrefix(Env env, PField pf, String prefix) throws ParseException{
		return removePrefix( env, new Options(env), pf,  prefix);
	}
	
	public static PField removePrefix(Env env, Options opt, PField pf, String prefix) throws ParseException{
		List<PLiteral> newLits=new ArrayList<PLiteral>();
		for (PLiteral lit:pf.getPLiterals()){
			newLits.add(removePrefix(env, opt, lit, prefix));
			//newLits.add(lit.removePrefix(prefix));
		}
		return createPField(env, opt, newLits);
	}
	
	/*
	public IndepPField removePrefix(String prefix){
		List<IndepLiteral> newLits=new ArrayList<IndepLiteral>();
		for (IndepLiteral lit:getLiterals()){
			newLits.add(lit.removePrefix(prefix));
		}
		return new IndepPField(newLits,localConditions,globalConditions);
	}*/
	
	/**
	 * Adds a list of PLiterals to a PField.
	 * @param pf The production field.
	 * @param plits The list of PLiterals.
	 */
	public static void add(PField pf, List<PLiteral> plits) {
		for(PLiteral pl:plits)
			pf.add(pl);
	}
		
	// add Literals
	// if literal already present (same string), do not add (without comparing localCond)
	// otherwise add
	/*
	public IndepPField addToLiterals(List<IndepLiteral> newLiterals, List<PFCond> localCondNewLits){
		List<IndepLiteral> lits=getLiterals();
		List<PFCond> localConds=new ArrayList<PFCond>(localConditions);
		int i;
		for (i=0;i<newLiterals.size();i++) {
			IndepLiteral toAddLit = newLiterals.get(i);
			if (!lits.contains(toAddLit)){
				lits.add(toAddLit);
				localConds.add(localCondNewLits.get(i));
			}
		}
		return new IndepPField(lits,localConds,globalConditions);
	}*/
	
	/**
	 * Converts a list of Literals to PLiterals and adds them to a PField.
	 * @param pf The production field.
	 * @param lits The literals.
	 */
	public static void addLiterals(PField pf, List<Literal> lits){
		for(Literal lit:lits)
			pf.add(toPLiteral(lit));
	}
	
	public static void addPLiterals(PField pf, List<PLiteral> lits){
		for(PLiteral lit:lits)
			pf.add(lit);
	}
	
	/**
	 * Converts a list of Literals to PLiterals and adds them to a PField.
	 * @param pf The production field.
	 * @param lits The literals.
	 * @param conds The pairs of conditions <maxDepth, maxLength>
	 * @throws Exception
	 */
	public static void addLiterals(PField pf, List<Literal> lits, List<Pair<Integer, Integer>> conds) throws Exception{
		if(conds.size() == 0){
			addLiterals(pf, lits);
			return;
		}
		if(lits.size() != conds.size())
			throw new Exception("Wrong production literals condition list size!");
		for(int i = 0; i < lits.size(); i++)
			pf.add(toPLiteral(lits.get(i), conds.get(i).getLeft(), conds.get(i).getRight()));
	}

	// add Literals
	// if literal already present, do not add (without comparing localCond)
	// otherwise add
	public static PField addToLiteralsFullCheck(Env env, PField pf, List<PLiteral> newLiterals){
		return addToLiteralsFullCheck(env, new Options(env), pf, newLiterals);
	}
	
	public static PField addToLiteralsFullCheck(Env env, Options opt, PField pf, List<PLiteral> newLiterals){
		List<Integer> toRemove=new LinkedList<Integer>();
		List<PLiteral> lits=pf.getPLiterals();
		//List<PFCond> localConds=new ArrayList<PFCond>(localConditions);
		int i,j;
		for (i=0;i<newLiterals.size();i++) {
			PLiteral toAddLit = newLiterals.get(i);
			boolean add=true;
			for (j=0;j<lits.size();j++){
				PLiteral initLit=lits.get(j);
				Literal lInitLit = toLiteral(env, initLit);
				Literal lToLiteral = toLiteral(env, toAddLit);
				if (lInitLit.subsumes(lToLiteral) != null){
					add=false;
					break;
				}
				if (lToLiteral.subsumes(lInitLit) != null)
					toRemove.add(0, j); //add first the larger number
			}
			if (add) {
				lits.add(toAddLit);
			}
			if (!toRemove.isEmpty())
				for (Integer index:toRemove){
					// this starts from larger indexes so
					// indexes that remains to remove are not affected
					lits.remove(index);
				}
		}
		return createPField(env, opt, lits);
	}

	/*
	public IndepPField addToLiterals(List<IndepLiteral> newLiterals){
		List<PFCond> localCond=new ArrayList<PFCond>();
		for (int i=0;i<newLiterals.size();i++)
			localCond.add(new PFCond());
		return addToLiterals(newLiterals, localCond);
	}*/
		/*
	public IndepPField addToLiterals(IndepLiteral toAddLit){
		List<IndepLiteral> lits=new ArrayList<IndepLiteral>();
		lits.add(toAddLit);
		List<PFCond> localCond=new ArrayList<PFCond>();
		localCond.add(new PFCond());
		return addToLiterals(lits,localCond);
	}*/
/*
	public IndepPField setLocalCond(IndepLiteral lit, int maxDepth, int maxLength){
		List<PFCond>localCond=new ArrayList<PFCond>(localConditions);
		int index=literals.indexOf(lit);
		if (index>=0)
			localCond.set(index, new PFCond(maxDepth,maxLength));
		return new IndepPField(literals,localCond,globalConditions);
	}
	
	public IndepPField setMaxLength(int maxLength){
		PFCond gb=new PFCond(globalConditions.maxDepth,maxLength);
		return new IndepPField(literals,localConditions,gb);
	}*/
	
	// TODO other merge.
	// and symmetrical addLiteral(constrain)
	public static PField mergeWith(Env env, PField one, PField other, int mergeType){
		return mergeWith( env, new Options(env),  one,  other,  mergeType);
	}
	
	public static PField mergeWith(Env env, Options opt, PField one, PField other, int mergeType){
//		System.out.println("Parsing for mergeWith : "+this+" and "+other);
	//	IndepPField res=new IndepPField(literals,localConditions, 
	//			PFCond.merge(globalConditions, other.globalConditions));
		//IndepPField res=new IndepPField(literals,localConditions, globalConditions);
		PField res = copyPField(env, opt, one);
		PField add = copyPField(env, opt, other);
		constrainAllLocalWith(add, add);
		IndepPField.addPLiterals(res, add.getPLiterals());
		for(PFCardConstraint csr : add.getAddConstraints()){
			res.addConstraint(csr);
		}

		return res;
		//return res.addToLiterals(other.getLiterals(),
			//	other.constrainAllLocalWith(other.globalConditions).localConditions);
	}
	
	public static void constrainAllLocalWith(PField target, PField origin){
		int newlength = origin.getMaxLength(), newdepth = origin.getMaxTermDepth();
		for(PLiteral plit:target.getPLiterals()){
			if(ToolIndex.isBefore(newlength, plit.getMaxLength()))
				plit.setMaxLength(origin.getMaxLength());
			if(ToolIndex.isBefore(newdepth, plit.getMaxTermDepth()))
				plit.setMaxTermDepth(newdepth);
		}
	}
	/*
	public IndepPField constrainAllLocalWith(PFCond constraint){
		List<PFCond> localCond=new ArrayList<PFCond>();
		for (PFCond initialCond: localConditions){
			localCond.add(PFCond.merge(initialCond, constraint, true));
		}
		return new IndepPField(literals, localCond, globalConditions);
	}*/

	
	public static PField fitToCNF(Env env, Options opt,  CNF clauses, boolean negated) throws ParseException{
		PField pf;
		if (negated)
			pf= createPField(env, opt, CNF.getNegatedVocabulary(env, clauses));
		else
			pf= createPField(env, opt, CNF.getVocabulary(env, clauses));
		
		/*for (Clause cl:clauses)
			pf = IndepPField.fitConditionsToClause(pf, cl, negated);*/
		return pf;
	}
	
	public static PField fitConditionsToClause(PField pf, Clause cl, boolean negated){
		//List<PFCond> localConds=new ArrayList<PFCond>();
		int maxSize=pf.getMaxLength();
		List<PLiteral> plits = pf.getPLiterals();
		for (int i=0;i<plits.size();i++){
			PLiteral plit=plits.get(i);
			if (negated) plit.negate();
			int occ=cl.countOccurences(plit);
			PFCond litCond=localConditions.get(i);
			if (occ>0 && occ>litCond.maxLength)
				localConds.add(new PFCond(litCond.maxDepth, occ));
			else 
				localConds.add(litCond);
		}
		if (cl.getLiterals().size()>maxSize)
			return new PField(literals,localConds,
					new PFCond(globalConditions.maxDepth,maxSize));
		return new PField(literals,localConds, globalConditions);
	}
	
	public static boolean belongsTo(Env env, PField pf, Clause cl) throws ParseException{
		return belongsTo( env, new Options(env),  pf,  cl);
	}

	//Note : IndepPField only contains freed literals.
	// No depth check
	public static boolean belongsTo(Env env, Options opt, PField pf, Clause cl) throws ParseException{
		if (pf.getMaxLength() >= 0 && cl.getLiterals().size() > pf.getMaxLength()) 
			return false;
		int[] occurences=new int[pf.getPLiterals().size()];
		int i;
		for (i=0; i<occurences.length; i++)
			occurences[i]=0;
		for (Literal lit:cl.getLiterals()){
			int ind = pf.getPLiterals().indexOf(toPLiteral(IndepLiteral.getFreedLiteral(env, opt, lit), pf.getMaxTermDepth(), pf.getMaxLength()));
			if (ind>=0){
				occurences[ind]++;
				int mlength = pf.getPLiterals().get(ind).getMaxLength();
				if (mlength > 0 
						&& occurences[ind] > mlength )
					return false;
			}
			else
				return false;
		}
		return true;
	}
	
	//Note : IndepPField should not contains literals that subsumes each others.
	// No depth check
	public static boolean belongsToFullCheck(Env env, PField pf, Clause cl){
		if (pf.getMaxLength() >= 0 && cl.getLiterals().size() > pf.getMaxLength()) 
			return false;
		int[] occurences=new int[pf.getPLiterals().size()];
		int i;
		for (i=0; i < occurences.length; i++)
			occurences[i]=0;
		for (Literal lit:cl.getLiterals()){
			boolean belongs=false;
			for (i=0; i < pf.getPLiterals().size(); i++){
				Literal fieldLit = toLiteral(env, pf.getPLiterals().get(i));
				if (IndepLiteral.subsumes(fieldLit, lit)){
					occurences[i]++;
					int mlength = pf.getPLiterals().get(i).getMaxLength();
					if (mlength > 0 
							&& occurences[i] > mlength)
						return false;
					belongs=true;
					break;
				}
			}
			if (!belongs)
				return false;
		}
		return true;
	}

	public static List<PLiteral> getLinkedLiterals(PField pf) throws ParseException{
		List<PLiteral> res=new ArrayList<PLiteral>();
		//Env env=new Env();
		for (PLiteral lit:pf.getPLiterals())
			if (!lit.isMaxGeneral()){
				res.add(lit);
		}
		return res;
	}

	
	//return true if a non empty-subclause of cl belongs to the PF
	// assumes only freed literals in PF
	public static boolean partlyBelongsTo(Env env, PField pf, Clause cl) throws ParseException {
		for (Literal lit:cl.getLiterals())
			if (pf.getPLiterals().contains(toPLiteral(IndepLiteral.getFreedLiteral(env, lit))))
				return true;
		return false;
	}

	//return true if a non empty-subclause of cl belongs to the PF
	// and the rest of the clause is in PF2
	// assumes only freed literals in PF
	// does not take into account the length restrictions of PF (only take into account those of PF2)
	public boolean partlyBelongsTo(IndepClause cl, IndepPField pf2) {
		List<IndepLiteral> remaining=new ArrayList<IndepLiteral>();
		int[] occurences=new int[literals.size()];
		int i;
		boolean check=false;
		for (i=0;i<occurences.length;i++)
			occurences[i]=0;
		for (IndepLiteral lit:cl.getLiterals()){
			boolean belongs=false;
			int ind=literals.indexOf(lit.getFreedLiteral());
			if (ind>=0){
				occurences[ind]++;
				if (!(localConditions.get(ind).maxLength>0 
						&& occurences[ind] > localConditions.get(ind).maxLength)){
					belongs=true; check=true;
				}
			}
			if (!belongs){
				remaining.add(lit);
			}
		}
		return (check && pf2.belongsTo(new IndepClause("test",remaining)) );
	}

	
	
	//return true if a non empty-subclause of cl belongs to the PF
	public boolean partlyBelongsToFC(IndepClause cl) {
		for (IndepLiteral lit:cl.getLiterals()){
			boolean belongs=false;
			for (IndepLiteral fieldLit:getLiterals()){
				if (fieldLit.subsumes(lit)){
					// check local constraints : no need
					belongs=true;
					break;
				}
			}
			if (belongs)
				return true;
		}
		return false;
	}

	//return true if a non empty-subclause of cl belongs to the PF
	// and the rest of the clause is in PF2
	public boolean partlyBelongsToFC(IndepClause cl, IndepPField pf2) {
		List<IndepLiteral> remaining=new ArrayList<IndepLiteral>();
		int[] occurences=new int[literals.size()];
		int i;
		boolean check=false;
		for (i=0;i<occurences.length;i++)
			occurences[i]=0;
		for (IndepLiteral lit:cl.getLiterals()){
			boolean belongs=false;
			for (i=0;i<literals.size();i++){
				IndepLiteral fieldLit=literals.get(i);
				if (fieldLit.subsumes(lit)){
					occurences[i]++;
					if (localConditions.get(i).maxLength>0 
							&& occurences[i] > localConditions.get(i).maxLength)
						break;
					belongs=true; check=true;
					break;
				}
			}
			if (!belongs){
				remaining.add(lit);
			}
		}
		return (check && pf2.belongsTo(new IndepClause("test",remaining)) );
	}

	
	
	public PField toPField(Env env, Options opt) throws ParseException{
		//Options opt=new Options(env);
		PField pf=PField.parse(env, opt, toString());
		return pf;
	}	
	
	public boolean sameString(IndepPField comp){
		if (comp==this) {
			System.out.print("[ d-comp ]");
			return true;
		}
		if (comp==null) return (false);
		return toString().equals(comp.toString());
	}
	
	public static String toStringPLit(IndepLiteral lit, PFCond cond){
		return ""+lit+cond; 
	}
	/*
	public String toString(){
		String strRepr="pf(";
		List<String> lits=new ArrayList<String>();
		for (int i=0;i<literals.size();i++){
			lits.add(toStringPLit(literals.get(i), localConditions.get(i)));
		}
		strRepr+=lits.toString()+globalConditions+").";
		// NOTE : need to add literals one by one if local constraints
		return strRepr;
	}*/

	// a priori, this is the same as toString, but in case of future change to toString...
	public static String toSolFileLine(PField pf){
		return "pf("+pf.toString()+").";
	}

	//protected String strRepr="[]";
	
	//protected List<IndepLiteral> literals=new ArrayList<IndepLiteral>();
	
	protected List<PFCond> localConditions=new ArrayList<PFCond>();
	
	//protected PFCond globalConditions=new PFCond();
	
	public static final int MRG_UNION_INITFIT=1;
	
	
	
	
	
	public static void main(String[] args) throws ParseException{
		//test of every function
		
		
		Env env = new Env();
		Options opt = new Options(env);
		PField pf = null;
		Clause cl = null;
		try{
			pf = PField.parse(env, opt, "pf([-f, -c, +b, +h]).");
			cl = Clause.parse(env, opt, "[b, -f]");
		}catch(ParseException e){
			//
		}
		System.out.println(belongsTo(env, pf, cl));
		
		/*IndepPField[] p=new IndepPField[20]; 
		IndepPField temp;
		
		p[0]=IndepPField.parse("[]");
		p[1]=IndepPField.parse("[+-p_p_lit<3]");
		p[2]=IndepPField.parse(" [p_f(1,_,t), +-e(gfun(h,k),hfun(e,f)) :3,   +-muf <= 5]:3<8");
		p[3]=IndepPField.parse("[+-e(_,_):2  <=1]");
		p[4]=IndepPField.parse("[-e(_,hfun(e,_)):4]:5<= 3");
		p[5]=IndepPField.parse("[muf<3]<4");
		p[6]=IndepPField.parse("");
		p[7]=IndepPField.parse(null);
		p[8]=IndepPField.parse("[a,b,c]");
		p[9]=IndepPField.parse("[b,c,e,f]<=1");*/
		
//		for (int i=0;i<10;i++){
//			System.out.println("p["+i+"] : "+p[i]);
//			if (p[i]!=null){
//				Env env=new Env();
//				PField pf=p[i].toPField(env);
//				System.out.println("toPField : "+pf);
//				Arguments plits=p[i].getPLiterals();
//				System.out.println("getPLiterals() :"+plits);
//				temp=p[i].addPrefix("p_", false);
//				System.out.println("p["+i+"].addPrefix(p_,false) :"+temp);
//				temp=p[i].addPrefix("p_", true);
//				System.out.println("p["+i+"].addPrefix(p_,true) :"+temp);
//				temp=p[i].removePrefix("p_");
//				System.out.println("p["+i+"].removePrefix(p_) :"+temp);
//				temp=p[i].developPLiterals();
//				System.out.println("p["+i+"].developLiterals() :"+temp);
//				
//				for (int j=i;j<7;j++){
//					temp=p[i].replacePLiterals(p[j].getPLiterals().toString());
//					System.out.println("p["+i+"].replacePLiterals(p["+j+"].getLiterals().toString()) :"+temp);
//					temp=p[i].addToPLiterals(p[j].getPLiterals().toString());
//					System.out.println("p["+i+"].addToPLiterals(p["+j+"].getLiterals().toString()) :"+temp);
//					temp=p[i].mergeWith(p[j]);
//					System.out.println("p["+i+"].mergeWith(p["+j+"]) :"+temp);
//				}
//				System.out.println();
//			}
//			
//		}
//		//TODO test belongsTo
		/*IndepClause[] c=new IndepClause[20]; 
		c[1]=new IndepClause("[b,c,e]");
		c[0]=new IndepClause("[a,b]");
		c[2]=new IndepClause("[a,e,f]");
		c[3]=new IndepClause("[e]");
		for (int i=0;i<4;i++){
			IndepClause cl=c[i];
			System.out.println();
			System.out.println();
			System.out.println(" Clause : "+cl);
			System.out.println(p[8]);
			System.out.println(" belongs to :"+ p[8].belongsTo(cl));
			System.out.println(" FC belongs to :"+ p[8].belongsToFullCheck(cl));
			System.out.println(" partly belongs to :"+ p[8].partlyBelongsTo(cl));
			System.out.println(" FC partly belongs to :"+ p[8].partlyBelongsToFC(cl));
			System.out.println(" partly belongs to wrt "+p[9]+" : "+ p[8].partlyBelongsTo(cl,p[9]));
			System.out.println(" FC partly belongs to wrt "+p[9]+" : "+ p[8].partlyBelongsToFC(cl,p[9]));
			System.out.println();
			System.out.println(p[9]);
			System.out.println(" belongs to "+ p[9].belongsTo(cl));
			System.out.println(" FC belongs to "+ p[9].belongsToFullCheck(cl));
			System.out.println(" partly belongs to "+ p[9].partlyBelongsTo(cl));
			System.out.println(" FC partly belongs to "+ p[9].partlyBelongsToFC(cl));
			System.out.println(" partly belongs to wrt "+p[8]+" : "+ p[9].partlyBelongsTo(cl,p[8]));
			System.out.println(" FC partly belongs to wrt "+p[8]+" : "+ p[9].partlyBelongsToFC(cl,p[8]));
				
		}*/
		
	}
	
}
