package org.nabelab.solar;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nabelab.solar.operator.Operator;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.proof.Proof;

import agLib.agentCommunicationSystem.CanalComm;
import agLib.agentCommunicationSystem.CommMessage;
import agLib.agentCommunicationSystem.CommunicationModule;
import agLib.agentCommunicationSystem.Message;
import agLib.agentCommunicationSystem.SystemMessage;

public class SolarDistr extends SOLAR{

	public SolarDistr(Env env, CFP cfp) {
		super(env, cfp);
	}



	public  void solve(CommunicationModule cAg) throws Exception {

		if(Thread.currentThread().isInterrupted())
			return;

		stats.setProds(Stats.ORG_CLAUSES,  cfp.getNumClauses());
		stats.setProds(Stats.ORG_LITERALS, cfp.getNumLiterals());

		long time = 0;

		StringWriter str = new StringWriter();
		PrintWriter  out = new PrintWriter(str);

		if (opt.use(USE_BRIDGE_FORMULA_TRANSLATION))
			cfp.convertToStablePField(out);

		cfp.initFVecMap();
		if (opt.use(USE_CLAUSE_SUBSUMP_MINIZING) || opt.use(USE_PURE_LITERAL_ELIMINATION)) {
			time = getCPUTime();
			if (env.dbg(DBG_VERBOSE)) System.out.print(", Minimizing ");
			if (opt.use(USE_CLAUSE_SUBSUMP_MINIZING))
				cfp.convertToSubsumpMinimal(out);
			if (opt.use(USE_PURE_LITERAL_ELIMINATION))
				cfp.applyPureLitElimination(out);
			if (env.dbg(DBG_VERBOSE)) System.out.print((getCPUTime() - time) / 1000.0 + "s");
		}

		if (opt.use(USE_FREQ_COMMON_LITS_EXTRACTION)) {
			cfp.applyFreqCommonLitsExtraction(out);
			cfp.initFVecMap();
		}
		boolean sorted = false;
		boolean addedEqRef = false;

		if(Thread.currentThread().isInterrupted())
			return;
		if (opt.getEqType() != CFP.EQ_AXIOMS_REQUIRED && cfp.useEquality()) {

			addedEqRef = cfp.addEqReflexivity();  // For checking the unit subsumption etc..

			time = getCPUTime();
			if (env.dbg(DBG_VERBOSE)) System.out.print(", Sorting ");

			cfp.initFVecMap();
			cfp.initClauseProperties(out);
			cfp.convertToSortedClauses(out);
			sorted = true;
			if (env.dbg(DBG_VERBOSE)) System.out.print((getCPUTime() - time) / 1000.0 + "s");

			time = getCPUTime();
			if (env.dbg(DBG_VERBOSE)) System.out.print(", Modifying ");
			cfp.convertToNoEqualityFormat(out);
			if (env.dbg(DBG_VERBOSE)) System.out.print((getCPUTime() - time) / 1000.0 + "s");

			cfp.initFVecMap();
			if (opt.use(USE_CLAUSE_SUBSUMP_MINIZING)) {
				time = getCPUTime();
				if (env.dbg(DBG_VERBOSE)) System.out.print(", Minimizing ");
				cfp.convertToSubsumpMinimal(out);
				if (env.dbg(DBG_VERBOSE)) System.out.print((getCPUTime() - time) / 1000.0 + "s");
			}

			if (opt.use(USE_FREQ_COMMON_LITS_EXTRACTION)) {
				cfp.applyFreqCommonLitsExtraction(out);
				cfp.initFVecMap();
			}
		}

		cfp.initClauseProperties(out);
		if (!sorted) {
			time = getCPUTime();
			if (env.dbg(DBG_VERBOSE)) System.out.print(", Sorting ");
			cfp.convertToSortedClauses(out);
			if (env.dbg(DBG_VERBOSE)) System.out.print((getCPUTime() - time) / 1000.0 + "s");
		}

		cfp.calcReductionOrder();
		cfp.initTautologyFreeness(out);
		cfp.initUnitSubsumptionChecking();

		cfp.initTopClauses();
		cfp.initSkipMinimality(out);
		cfp.initIncCarcComputation(out);

		if (addedEqRef)
			cfp.removeEqReflexivity();

		if (env.dbg(DBG_VERBOSE)) System.out.println(")");
		if (str.getBuffer().length() != 0)
			System.out.println(str);

		stats.setProds(Stats.CLAUSES,  cfp.getNumClauses());
		stats.setProds(Stats.LITERALS, cfp.getNumLiterals());

		if (env.dbg(DBG_PROBLEM)) System.out.println(cfp);

		if (env.dbg(DBG_SYMBOL_TABLE)) {
			System.out.println("[Symbol table]");
			System.out.println(env.getSymTable());
		}

		if (cfp.getStatus() == TRIVIALLY_SATISFIABLE)
			return;

		if(Thread.currentThread().isInterrupted())
			return;
		tableau = new Tableau(env, cfp, stats);

		Strategy strategy = cfp.getStrategy();
		SearchParam param = null;
		boolean loop = true;
		Collection<Conseq> oldConsq = new ArrayList<>();

		Message<?> ms ;

		CommMessage m;

		Collection<Clause> newCl;

		while ((param = strategy.getNextSearchParam(stats, getCPUTime(), tableau.getMaxNumSkipped(), param)) != null){
			
				if(Thread.currentThread().isInterrupted())
					return;
				stats.inc(Stats.STAGE);
				stats.setDepth(param.getDepthLimit());

				if (env.dbg(DBG_VERBOSE)) {
					if (stats.get(Stats.STAGE) != 1 && env.dbg(DBG_TABLEAUX))
						System.out.println();
					System.out.printf("Stage %d  (%s inf:%d)\n",
							stats.get(Stats.STAGE), param.toString(), stats.inf());
				}
				
				tableau.reset();
				
				tableau.setSearchParam(param);
			
				
				while (loop){
					oldConsq = cfp.getConseqSet().get();
					loop = solve(param);

					Collection<Conseq> cons;
					Collection<Clause> toSent;
					cons = cfp.getConseqSet().get();
					toSent = compare(oldConsq, cons);
					//envoyer les consq trouvées
					if(!toSent.isEmpty()){
						m = new CommMessage(1, toSent, cAg.commAgent);
						cAg.send(m);
					}

					//checker les clauses reçu
					while(!cAg.getComm().isEmpty()){
						ms = cAg.getComm().get();
						if(!( ms instanceof SystemMessage)){
							Collection<Clause> collection = (Collection<Clause>)ms.getArgument();
							newCl = collection;
							for(Clause cl: newCl){
								cl.setType(ClauseTypes.TOP_CLAUSE);
								cfp.addClause(new Clause(env,cl));
							}
						}
					}
				}
			
		}
		if (env.dbg(DBG_VERBOSE))
			System.out.println();

		env.getVarTable().backtrackTo(0);
		env.getVarTable().removeAllVars();
	}

	private Collection<Clause> compare(Collection<Conseq> oldConsq, Collection<Conseq> cons) {
		ArrayList<Clause> n  = new ArrayList<>();
		for(Conseq csq : cons)
			if(!oldConsq.contains(csq))
				try {
					n.add(Clause.parse(env, opt, csq.getLiterals().toString()));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		return n;
	}

	private boolean solve(SearchParam param) throws ParseException {

		if(Thread.currentThread().isInterrupted())
			return false;

		// Running time checking (checks each 1024 steps because getCpuTime() is a little bit slow).
		if (stats.inf() % 1024 == 0) {
			if (param.getTimeLimit() != 0 && param.getTimeLimit() < getCPUTime()) {
				param.setExhaustiveness(false);
				return false;
			}
		}
		if (param.getMaxNumInfs() != 0 && param.getMaxNumInfs() == stats.inf()) {
			param.setExhaustiveness(false);
			return false;
		}

		// If some characteristic clauses are received from other threads, then adds them to CFP.
		//if (!carcQueue.isEmpty())
		while (!carcQueue.isEmpty())
			cfp.addCarc(carcQueue.poll());

		stats.incInf();

		Node subgoal = tableau.getNextSubgoal();

		if (env.dbgNow(DBG_TABLEAUX)) {
			System.out.println();
			System.out.println("----------------------------------------");
			System.out.println(stats.inf());
			System.out.println(tableau);
		}
		else if (env.dbgNow(DBG_STEPS)) {
			System.out.println(stats.inf());
		}
		if (env.dbgNow(DBG_APPLIED_OPS)) {
			//System.out.println(stats.inf());
			//System.out.println(tableau.getAppOps().toSimpleString());
			System.out.println(tableau.getLastOperator());
		}

		if (subgoal == null)
			return false;
		Operator op = subgoal.getNextOperator();
		if (op == null) {
			if (tableau.cancel() == false)
				return false;
			if (opt.use(USE_NEGATION_AS_FAILURE) && !tableau.removeClosedNAFSubTableau())
				return false;
			return true;
		}
		if (tableau.apply(op) == false)
			return true;

		if (opt.use(USE_NEGATION_AS_FAILURE) && !tableau.removeClosedNAFSubTableau())
			return false;
		if (!tableau.removeRedundancy())
			return false;
		if (opt.divide()) {
			if (cfp.hasEmptyConseq())
				return false;
			if (param.getMaxNumConseqs() != 0 && cfp.getConseqSet().size() >= param.getMaxNumConseqs()) {
				param.setExhaustiveness(false);
				return false;
			}
		}
		else if (tableau.getNumOpenNodes() == 0) {
			Conseq conseq = tableau.getConseq();

			if (opt.hasVerifyOp()) {
				Proof proof = tableau.getProof(conseq);
				conseq.setProof(proof);
			}
			if (env.dbg(DBG_SOLVED_TABLEAUX)) {
				System.out.println();
				System.out.println("SOLVED");
				System.out.println(stats.inf() + " " + tableau);
			}
			if (tableau.getPFChecker().belongs(conseq) && cfp.addConseq(conseq)) {

				stats.setProds(Stats.CONSEQUENCES, cfp.getConseqSet().size());
				stats.setProds(Stats.CONSEQ_LITS , cfp.getConseqSet().getNumLiterals());

				if (cfp.hasEmptyConseq()) {
					cfp.setStatus(UNSATISFIABLE);
					return false;
				}
				if (cfp.getConseqSet().size() == param.getMaxNumConseqs()) {
					param.setExhaustiveness(false);
					return false;
				}
				tableau.markAs(Tags.SOLVABLE);
			}
			tableau.cancel();

			//return true;
		}

		return true;
	}

}
