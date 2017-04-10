package org.nabelab.solar.pfield;

import java.util.ArrayList;
import java.util.List;

import org.nabelab.solar.Clause;
import org.nabelab.solar.Env;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Options;
import org.nabelab.solar.PLiteral;
import org.nabelab.solar.SymTable;
import org.nabelab.solar.Unifiable;
import org.nabelab.solar.parser.ParseException;


public class PFieldCheckerCardConstraints extends PFieldCheckerWithSubst {

	public PFieldCheckerCardConstraints(Env env, PField pfield) throws Exception {
		super(env, pfield);

		for (PFCardConstraint constraint : pfield.getAddConstraints()){
			// create constraint counters 
			PFieldCounter pfCtr=new PFieldCounter(constraint.getMaxLengthGroup());
			// search literals in the group
			for (PLiteral lit:constraint.getGroup()){
				if (lit.isSpecial()){ // special literal : the constraint is only applied to all allowed literals (of correct sign) defined by language
					SymTable symTable = env.getSymTable();
					for (int name=0; name < symTable.getNumSyms(PREDICATE); name++) {

						switch (lit.getSign()) {
						case PLiteral.POS:
							if (positives[name]!=null) positives[name].addToGroup(pfCtr); 
							break;
						case PLiteral.NEG:
							if (negatives[name]!=null) negatives[name].addToGroup(pfCtr); 
							break;
						case PLiteral.BOTH:      
							if (positives[name]!=null) positives[name].addToGroup(pfCtr); 
							if (negatives[name]!=null) negatives[name].addToGroup(pfCtr); 
							break;
						default:
							assert(false);
						}
					}
				}
				else if (lit.isMaxGeneral()){ // //max general : if in the language, suffices to add it to the group, otherwise add to subsumed literals
					int name = lit.getName();
					int sign = lit.getSign();

					boolean checkTree=false;
					switch (sign) {
					case PLiteral.POS:
						if (positives[name]!=null) positives[name].addToGroup(pfCtr);
						else checkTree=true;
						break;
					case PLiteral.NEG:
						if (negatives[name]!=null) negatives[name].addToGroup(pfCtr);
						else checkTree=true;
						break;
					case PLiteral.BOTH:        
						if (positives[name]!=null) positives[name].addToGroup(pfCtr);
						else checkTree=true;
						if (negatives[name]!=null) negatives[name].addToGroup(pfCtr);
						else checkTree=true;
						break;
					default:
						assert(false);
					}	    	    	  
					if (checkTree) {
						//TODO check if more specific form of this literal are preent in the disc tree ??
						if (pfieldItems != null){
							PFieldItem i = pfieldItems.contains(Literal.parse(env, new Options(env), lit.toString()));
							if( i == null){
								PFieldItem item = pfieldItems.containsOtherForm(Literal.parse(env, new Options(env), lit.toString()));
								if (item == null){
									//									switch (sign) {
									//									case PLiteral.POS: 
									//										positives[name]= new PFieldItem(lit, maxLenCounter);
									//										positives[name].addToGroup(pfCtr);
									//										break;
									//									case PLiteral.NEG:
									//										negatives[name]= new PFieldItem(lit, maxLenCounter);
									//										negatives[name].addToGroup(pfCtr);
									//										break;
									//									case PLiteral.BOTH: 
									//										positives[name]= new PFieldItem(lit, maxLenCounter);
									//										positives[name].addToGroup(pfCtr);
									//										negatives[name]= new PFieldItem(lit, maxLenCounter);
									//										negatives[name].addToGroup(pfCtr);
									//									}
									//									pfieldItems.add(Literal.parse(env, new Options(env), lit.toString()), item);
									throw new Exception(lit + " does not belong to pf");
								}
							}
						}
					}

				}
				else {
					// check if this specific literal is already present
					boolean addNode = true;
					if (pfieldItems!=null){
						Literal refLitPos=new Literal(env, true,  lit.getTerm());
						Literal refLitNeg=new Literal(env, false,  lit.getTerm());
						boolean pos= (lit.getSign()!=PLiteral.NEG);
						boolean neg= (lit.getSign()!=PLiteral.POS);
						boolean both = (lit.getSign()==PLiteral.BOTH);
						if (pos){
							PFieldItem item=pfieldItems.contains(refLitPos);
							if (item!=null){
								item.addToGroup(pfCtr);
								addNode = false;
							}
						}
						if (neg){
							PFieldItem item=pfieldItems.contains(refLitNeg);
							if (item!=null){
								item.addToGroup(pfCtr);
								addNode = false;
							}	    	    	
						}
						//TODO Problem : double sign // special literals and cardinality constraints on single sign ?
						if (both){
							PFieldItem itemNeg=pfieldItems.contains(refLitNeg);
							PFieldItem itemPos=pfieldItems.contains(refLitPos);
							if (itemNeg != null){
								itemNeg.addToGroup(pfCtr);
							}
							if (itemPos != null){
								itemPos.addToGroup(pfCtr);
							}
						}  		
					}


					//TODO if not present, add a new node ? not unless there is a subsuming node (or a max general instance)
					if (addNode){

						switch (lit.getSign()) {
						case PLiteral.POS:      
						{
							Literal l = new Literal(env, true, lit.getTerm());
							if (pfieldItems.isSubsumed(l) == null){
								PFieldItem item = new PFieldItem(lit, maxLenCounter);
								pfieldItems.add(l, item);

							}
						}
						break;
						case PLiteral.NEG:
						{
							Literal l = new Literal(env, false, lit.getTerm());
							if (pfieldItems.isSubsumed(l) == null){

								PFieldItem item = new PFieldItem(lit, maxLenCounter);
								pfieldItems.add(l, item);
								break;
							}
						}
						case PLiteral.BOTH:
						{
							Literal l1 = new Literal(env, true,  lit.getTerm());
							Literal l2 = new Literal(env, false, lit.getTerm());
							PFieldItem item = new PFieldItem(lit, maxLenCounter);
							pfieldItems.add(l1, item);
							pfieldItems.add(l2, item);
							break;
						}
						}
					}
				}
			}   
		}
	}


	public static PFieldCheckerCardConstraints createChecker(Env env, PField pfield) throws Exception {
		return new PFieldCheckerCardConstraints(env, pfield);

	}


	public boolean belongs(Clause c) {

		List<PFCardConstraint> constraints = pfield.getAddConstraints();
		for (Literal lit : c.getLiterals()) {
			List<Unifiable<PFieldItem>> unifs = getUnifiableItems(lit);
			if (unifs == null)
				return false;
		} 
		Clause p = c.instantiate();
		if(constraints.size()!=0){
			int count[] = new int[constraints.size()];
			for (int i= 0 ; i< constraints.size(); i++){
				count[i]= constraints.get(i).getMaxLengthGroup();
			}
			List<Literal> lits = new ArrayList<Literal>();

			for (Literal lit : p.getLiterals()) {
				for (PFCardConstraint crt : constraints){
					if (crt.contains(env, new Options(env), lit) != null){
						lits.add(lit);
						break;
					}

				}
			}


			if(lits.isEmpty() || lits.size() != p.getLiterals().size())
				return true;
			return checkGroup(lits, count, constraints,  0);
		}
		return true;
	}


	private boolean checkGroup(List<Literal> lits, int[] count,
			List<PFCardConstraint> constraints, int i) {

		for (int j = 0; j< count.length; j++)
			if (count[j] == -1) return false;


		if (i >= lits.size()) return true;
		Literal lit = lits.get(i);

		for (int j = 0; j<constraints.size(); j++){
			if (constraints.get(j).contains(env, new Options(env), lit) != null){

				count[j]--;
				if (checkGroup(lits, count, constraints, i=i+1)){
					return true;
				}
				else {
					count[j]++;
					i--;
				}
			}
		}
		return false;
	}
}