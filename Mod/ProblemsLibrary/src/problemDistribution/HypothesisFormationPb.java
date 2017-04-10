package problemDistribution;

import java.util.ArrayList;
import java.util.List;

import logicLanguage.CNF;
import logicLanguage.IndepClause;
import logicLanguage.UnitClauseCNF;
import solarInterface.IndepPField;

public class HypothesisFormationPb {


	public HypothesisFormationPb(){
		locTheory=new CNF();
		manifestations=new UnitClauseCNF();
		hypothesisField=new IndepPField();
		favHypotheses=new ArrayList<UnitClauseCNF> ();
		
	}

	public HypothesisFormationPb(CNF theory, UnitClauseCNF manifs, IndepPField hypField){
		locTheory=theory;
		manifestations=manifs;
		setHypothesisField(hypField);
		favHypotheses=new ArrayList<UnitClauseCNF> ();
		
	}
	
	////VARIABLES
	
	public void addToLocTheory(CNF locTheory) {
		locTheory.addAll(locTheory);
	}
	public void addClauseToLocTheory(IndepClause cl) {
		locTheory.add(cl);
	}

	public CNF getLocTheory() {
		return locTheory;
	}

	public void addManifestation(IndepClause manif) {
		manifestations.add(manif);
	}
	public void addManifestations(UnitClauseCNF newManifestations) {
		manifestations.addAll(newManifestations);
	}

	public UnitClauseCNF getManifestations() {
		return manifestations;
	}

	public void setHypothesisField(IndepPField hypothesisField) {
		this.hypothesisField = hypothesisField;
	}

	public IndepPField getHypothesisField() {
		return hypothesisField;
	}

	public void addFavHypothesis(UnitClauseCNF favHypothesis) {
		favHypotheses.add(favHypothesis);
	}
	public void addFavHypotheses(List<UnitClauseCNF> newHypotheses) {
		favHypotheses.addAll(newHypotheses);
	}
	public void removeHypothesis(UnitClauseCNF hypothesis) {
		for (int i=0;i<favHypotheses.size();){
			UnitClauseCNF hyp=favHypotheses.get(i);
			if (hypothesis.toString().equals(hyp.toString()))
				favHypotheses.remove(i);
			else
				i++;			
		}
	}
	

	public List<UnitClauseCNF> getFavHypotheses() {
		return favHypotheses;
	}

	/** individual theory of an agent */
	private CNF locTheory;
	/** individual observations of an agent that should be explained*/
	private UnitClauseCNF manifestations;
		
	/** hypotheses specification */
	private IndepPField hypothesisField;

	/** Candidate hypotheses 
	/** Favored Hypotheses*/
	private List<UnitClauseCNF> favHypotheses;

}
