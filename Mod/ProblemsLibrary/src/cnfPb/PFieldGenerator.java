package cnfPb;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.nabelab.solar.Clause;
import org.nabelab.solar.Env;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Options;
import org.nabelab.solar.PLiteral;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.pfield.PField;

import logicLanguage.IndepClause;
import logicLanguage.IndepLiteral;

import solarInterface.IndepPField;
import solarInterface.SolProblem;

public class PFieldGenerator {

	public PFieldGenerator(SolProblem source){
		this.source=source;
	}
	
	protected void setVocabulary(){
		List<Literal> tempVocab;
		for (Clause c:source.getClauses()){
			tempVocab = c.getLiterals();
			int i=0;
			for (Literal lit:tempVocab){
				vocabulary.add(lit);
				reordering.add(i);i++;
				frequency.add(0);
			}
		}
		setVocab=true;
	}
	
	protected int getIndexLiteral(Literal lit) throws ParseException{
		Literal seek=lit;
		if(seek.isNegative())
			seek.negate();
		for (int i=0;i<vocabulary.size();i++){
			Literal vocab=vocabulary.get(i);
			if (seek.equals(vocab))
				return i;
		}
		return -1;
	}
	
	protected void setFrequency() throws ParseException{
		//assume vocabulary is set.
		for (Clause cl:source.getClauses())
			for (Literal lit:cl.getLiterals()){
				int ind=getIndexLiteral(lit);
				if (ind>=0)
					frequency.set(ind, frequency.get(ind)+1);					
			}
		setFreq=true;
	}
	
	protected void setOrdering(boolean increasing, int nbElt){
		int ind, i, extremum, indext;
		if (nbElt==-1)
			nbElt=reordering.size();
		for (ind=0;ind<nbElt;ind++){
			indext=ind;
			extremum=frequency.get(reordering.get(indext));
			for (i=ind;i<reordering.size();i++){
				int weight=frequency.get(reordering.get(i));
				if ((increasing && weight<extremum) || (!increasing && weight>extremum)){
					indext=i;
					extremum=weight;
				}	
			}
			//exchange ind and indext to put extrema at ind
			Integer newExtremum=reordering.remove(indext);
			reordering.add(ind, newExtremum);
		}
	}
	
	public PField setFreqPField(boolean mostRare, double proportion) throws ParseException{
		
		if (!setVocab) setVocabulary();
		if (!setFreq) setFrequency();
		
		int nbElt=(int)(proportion*vocabulary.size());
		
		setOrdering(mostRare, nbElt);
		Env env = new Env();
		PField pfLit = new PField(env, new Options(env));
		
		for (int i=0;i<nbElt;i++){
			PLiteral l=PLiteral.parse(env, new Options(env), vocabulary.get(reordering.get(i)).toSimpString());
			if (!pfLit.contain(l)){
				pfLit.add(l);
				PLiteral pf = new PLiteral(l);
				
				pf.negate();
				pfLit.add(pf);
				
			}
			
		}
		
		output= pfLit;
		return output;
	}
	
	public PField setGlobalPField(){
		/*List<Literal> vocab=source.getClauses().getVocabulary();
		output=new PField(vocab);*/
		output = source.getPField();
		return output;
	}
	
	
	public PField output;
	protected boolean setVocab=false;
	protected boolean setFreq=false;
	protected SolProblem source;
	protected List<Literal> vocabulary=new ArrayList<Literal>();
	protected List<Integer> frequency=new ArrayList<Integer>();
	protected List<Integer> reordering=new LinkedList<Integer>();
	
}
