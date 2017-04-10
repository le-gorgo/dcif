package distNewCarc.partition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.hamcrest.core.IsInstanceOf;
import org.nabelab.solar.Clause;
import org.nabelab.solar.Literal;
import org.nabelab.solar.PLiteral;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.pfield.PField;

import distNewCarc.partition.newAsynch.NewAsyncProtocol;

import base.BasicTheoryAgent;
import base.TheoryAgent;
import solarInterface.CFSolver;
import solarInterface.IndepPField;
import solarInterface.SolProblem;
import stats.ConsFindingAgentStats;
import logicLanguage.CNF;
import logicLanguage.IndepClause;
import logicLanguage.IndepLiteral;
import agLib.agentCommunicationSystem.BasicAgent;
import agLib.agentCommunicationSystem.CanalComm;
import agLib.agentCommunicationSystem.CommunicationModule;
import agLib.agentCommunicationSystem.Network;
import agLib.agentCommunicationSystem.protocols.MainProtocol;
import agLib.masStats.StatCounter;

public class IncConsFindingAgent extends BasicTheoryAgent implements TheoryAgent, 
		PBICFAgent {

	
	public IncConsFindingAgent(int id, SolProblem pb, 
			CanalComm systComm, Network net, ConsFindingAgentStats das, boolean useNewConsAsAxiom, boolean inDepthPruning){
		super(id, pb, systComm, net, das);
		newConsAsAxiom = useNewConsAsAxiom;
		pruneInDepth = inDepthPruning;
	}
	
	public Collection<Clause> getAllTopClauses() {
		
		return theory.getTopClauses();
	}

	// remove from clauses the clauses that are subsumed by clauses in pruningSet
	public CNF pruneWith(Collection<Clause> clauses, Collection<Clause> pruningSet) throws ParseException{
		CNF result = new CNF();
		if (!pruneInDepth)
			for (Clause cl:clauses){
				boolean toKeep = true;
				for (Clause cl2:pruningSet){
					if (cl2.subsumes(cl)){
						toKeep = false;
						break;
					}
				}
				if (toKeep)
					result.add(cl);
			}
		else {
			result.addAll(CFSolver.pruneClauseSetFromCons(getEnv(), clauses, pruningSet, true, theory.getDepthLimit(), -1, stats.getSolarCtrList()));
		}
		return result;		
	}


	
	public Collection<Clause> computeNewCons(Collection<Clause> newCl) throws Exception {
		if(Thread.currentThread().isInterrupted())
			return null;
		CNF result = new CNF();
		//prune(newCl);
		CNF axioms = new CNF(theory.getAxioms());
		if (newConsAsAxiom) 
			axioms.addAll(listCsq);
		else
			axioms.addAll(receivedCl);
		CNF prunedTC = pruneWith(newCl, axioms);
		if (newConsAsAxiom)
			prunedTC = pruneWith(prunedTC, receivedCl);
		else
			prunedTC = pruneWith(prunedTC, listCsq);
		if (verbose){ 
			System.out.println(this+" receives "+newCl);
			System.out.println("After pruning "+prunedTC);
			System.out.println("Input Languages : "+inputLanguages);
		}
		if (prunedTC.isEmpty())
			return result;
		//determine pField as LP, Output Language and reduc(newCl, OutputL)
		PField originalPField = theory.getPField();
		PField computationPField = addReduc(outputLanguage, newCl);
		computationPField = IndepPField.mergeWith(getEnv(), getOptions(), computationPField, originalPField, IndepPField.MRG_UNION_INITFIT);
		if(Thread.currentThread().isInterrupted())
			return null;
		//compute newcarc
		SolProblem pb = new SolProblem(getEnv(), getOptions(), axioms, prunedTC, computationPField);
		pb.setDepthLimit(theory.getDepthLimit());	
		boolean incremental = false; boolean trueNC = false;
		if(cAg.gbProtocol instanceof NewAsyncProtocol)
			CFSolver.solveToClause(pb, -1, stats.getSolarCtrList(), result, incremental, trueNC, this.cAg);
		else 
			CFSolver.solveToClause(pb, -1, stats.getSolarCtrList(), result, incremental, trueNC, null);
		if(Thread.currentThread().isInterrupted())
			return null;
		//update receivedCl (after computation in case so that original state is used during it)
		receivedCl = pruneWith(receivedCl, prunedTC);
		receivedCl.addAll(prunedTC);

		return result;
	}
		

	public PField addReduc(PField base, Collection<Clause> clauses) throws ParseException{
		List<PLiteral> knownLits = base.getPLiterals();
		List<PLiteral> unknownLits = new ArrayList<PLiteral>();
		for (Clause cl:clauses){
			for (Literal lit:IndepClause.getVocabulary(getEnv(), cl)){
				//TODO : use contains or subsumption ? check on free literals ?
				PLiteral plit = IndepPField.toPLiteral(lit);
				if (knownLits.contains(plit) || knownLits.contains(IndepPField.negate(plit)))
					continue;
				else
					if (!unknownLits.contains(plit))
						unknownLits.add(plit);
			}
		}
		PField newpf = IndepPField.copyPField(getEnv(), getOptions(), base);
		IndepPField.addPLiterals(newpf, unknownLits);
		return newpf;
	}
	
	public boolean canResolve(CanalComm target, Clause cl) {
		PField pf = inputLanguages.get(target);
		boolean out = false;
		try {
			out = IndepPField.partlyBelongsTo(getEnv(), pf, cl);
		} catch (ParseException e) {
			// Never happens
			e.printStackTrace();
		}
		return out;
	}

	public boolean isPossibleOutput(Clause cl) {
		boolean out = false;
		try {
			out = IndepPField.belongsTo(getEnv(), theory.getPField(), cl);
		} catch (ParseException e) {
			// Never happens
			e.printStackTrace();
		}
		return out;
	}
	
	public void updateListNewCons(Collection<Clause> newCsq) {
		try {
			if (newConsAsAxiom) {
				//remove old csq submused by new ones
				CNF updatedList;
				updatedList = pruneWith(listCsq, newCsq);
				// prune receivedCl
				receivedCl = pruneWith(receivedCl, newCsq);
				//add new cons
				updatedList.addAll(newCsq);
				//store update
				listCsq = updatedList;
			}
			else {
				//remove old csq submused by new ones
				CNF updatedList = pruneWith(listCsq, newCsq);
				//add new cons
				updatedList.addAll(newCsq);
				//store update while pruning with receivedCl
				listCsq = updatedList;
					//pruneWith(updatedList, receivedCl);			
			}
		} catch (ParseException e) {
			// never happens
			e.printStackTrace();
		}
		if (verbose){ 
			System.out.println(this+": updated receivedCl "+receivedCl);
			System.out.println("updated lstCsq "+listCsq);
		}

	}

	public Collection<Clause> getListConseq() {
		return listCsq;
	}

	
	public void setOutputLanguage(PField language){
		outputLanguage = language;
	}
	
	public void setInputLanguage(PField language, CanalComm ag){
		inputLanguages.put(ag, language);
	}
	
	public void setProtocol(MainProtocol prot){
		cAg.setProtocol(prot);
	}
	
	public CommunicationModule getCommModule(){
		return cAg;
	}
	
	protected boolean newConsAsAxiom = true; // else use receivedCl
	protected boolean pruneInDepth = false; //prune by searching consequences and not jsut subsumption
	
	public static boolean verbose = false;
	
	protected Collection<Clause> listCsq = new CNF();
	protected Collection<Clause> receivedCl = new ArrayList<Clause>();
	protected PField outputLanguage;
	protected HashMap<CanalComm, PField> inputLanguages = new HashMap<CanalComm, PField>();
	
	
	public void send(Collection<Clause> cl) {
		send(cl);
		
	}
}
