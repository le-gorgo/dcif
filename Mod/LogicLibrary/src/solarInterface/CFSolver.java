package solarInterface;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import logicLanguage.CNF;
import logicLanguage.IndepClause;

import org.nabelab.solar.CFP;
import org.nabelab.solar.Clause;
import org.nabelab.solar.Conseq;
import org.nabelab.solar.Env;
import org.nabelab.solar.ExitStatus;
import org.nabelab.solar.SOLAR;
import org.nabelab.solar.SolarDistr;
import org.nabelab.solar.Strategy;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.pfield.PField;



import agLib.agentCommunicationSystem.CommunicationModule;
import agLib.masStats.StatCounter;




public class CFSolver {
	// complementary SOLAR stats :
	public static final int SOLST_CPU_TIME=-1;
	public static final int SOLST_INF=-2;

	public static <IncConsFindingAgent> int solveToClause(SolProblem pb, long deadline, List<StatCounter<Integer>> ctr, 
			Collection<Clause> resultingCons, boolean incremental, boolean trueNewC, CommunicationModule cAg) throws Exception{
		if(Thread.currentThread().isInterrupted())
			return ExitStatus.UNKNOWN;
		List<Conseq> tempRes=new ArrayList<Conseq>();
		int status;
		pb.getEnv().setDepthTerm(depthterm);
		if (!incremental){
			if(verbose){
				System.out.println("Starting SolveToClause...");
				System.out.println("Launching solver...");
				System.out.println(pb);
			}
			//pb.getOptions().setVerboseLv(2);
			//SolProblem newpb = new SolProblem();
			//for(Clause cl:pb.getClauses())
			//	newpb.addClause(Clause.parse(newpb.getEnv(), newpb.getOptions(), cl.toString()));
			//newpb.setPField(PField.parse(newpb.getEnv(), newpb.getOptions(), "pf("+pb.getPField().toString()+")."));
			
			solve(pb.getEnv(), pb, pb.getDepthLimit(),deadline, ctr, cAg);
			if(Thread.currentThread().isInterrupted())
				return ExitStatus.UNKNOWN;
		//	problem.getConseqSet().validate();
			if(verbose) System.out.println("Solver finished.");
			tempRes = pb.getConseqSet().get();
			status = pb.getStatus();
		}
		else {
			status=incSolve(pb,deadline, ctr,tempRes);
		}
		
		//if(verbose) System.out.println("Converting to IndepClause...");
		List<Clause> tempRes2=new ArrayList<Clause>();
		for (Conseq cons:tempRes){
			tempRes2.add(new Clause(cons));
		}
		if (trueNewC && !pb.getTopClauses().isEmpty()){
			if(verbose) System.out.println("Pruning NewCarc...");
			//CNF res=pruneNewCarc(pb.getAxioms(true), tempRes2, ctr, typeStat);
			CNF res = pruneNewCarc(pb.getEnv(), pb.getAxioms(), pb.getDepthLimit(), deadline, tempRes2, null);
			resultingCons.addAll(res);
		}			
		else 
			resultingCons.addAll(tempRes2);
		if(Thread.currentThread().isInterrupted())
			return ExitStatus.UNKNOWN;
		if(verbose) System.out.println("SolveToClause Finished!");
		return status;
	}
	
	public static int incSolve(SolProblem pb, long deadline, List<StatCounter<Integer>> ctr, 
			Collection<Conseq> resultingCons) throws Exception{
		int status=ExitStatus.SATISFIABLE;
		List<Clause> top = pb.getTopClauses();
		List<Clause> base = pb.getAxioms();
		if (top.isEmpty()){
			top = base;
			base = new ArrayList<Clause>();
		}
		
		for (int i=0;i<top.size();i++) {
			if(Thread.currentThread().isInterrupted())
				return ExitStatus.UNKNOWN;
			Clause cl=top.get(i);
			SolProblem problem = new SolProblem(pb.getEnv(), base, CNF.singleCNF(cl), pb.getPField());
			solve(pb.getEnv(), problem, pb.getDepthLimit(), deadline, ctr, null);
			problem.getConseqSet().validate();
			
			List<Conseq> tempRes=problem.getConseqSet().get();
			resultingCons.addAll(tempRes);
			if (problem.getStatus()==ExitStatus.UNSATISFIABLE){
				resultingCons.retainAll(tempRes); 
				status=ExitStatus.UNSATISFIABLE;
				return status;
			}
			if (problem.getStatus()==ExitStatus.UNKNOWN)
				status=ExitStatus.UNKNOWN;
			base.add(cl);
		}
		return status;
	}
	
	
	public static CNF pruneNewCarc(Env env, List<? extends Clause> axioms, int depthLimit, long deadline, List<? extends Clause> conseq,
			List<StatCounter<Integer>> ctr) throws ParseException  {
		CNF newcarc = new CNF();
    	    		
    	for (Clause ncl : conseq){
    		if (!isConsequence(env, ncl,axioms,depthLimit,deadline, ctr))
    				newcarc.add(ncl);
    	}
    	return newcarc;
	}

	//remove consequences that are consequences of the others
	public static void pruneConseqSet(Env env, List<? extends Clause> conseqList, int depthLimit, long deadline, 
			List<StatCounter<Integer>> ctr) throws ParseException  {
    	int saveMaxLength=lengthLimit;
    	lengthLimit=1;
    	for (int i=conseqList.size()-1;i>=0;i--){
    		Clause cons=conseqList.get(i);
    		List<Clause> otherCons=new ArrayList<Clause>(conseqList);
    		otherCons.remove(i);
    		if (isConsequence(env, cons,otherCons, depthLimit, deadline, ctr))
    				conseqList.remove(i);
    	}
    	lengthLimit=saveMaxLength;
	}

	//remove clauses of toPrune that are consequences of base or (if included is true) base U toPrune 
	public static List<Clause> pruneClauseSetFromCons(Env env, Collection<? extends Clause> toPrune,Collection<? extends Clause> base, boolean include, 
			int depthLimit, long deadline, List<StatCounter<Integer>> ctr) throws ParseException  {
    //	int saveMaxLength=lengthLimit;
    //	lengthLimit=1;
    	List<Clause> toRetain=new ArrayList<Clause>();
    	List<Clause> theory=new ArrayList<Clause>();
    	if (base!=null) theory.addAll(base);
    	if (include) theory.addAll(toPrune);
    	for (Clause cl:toPrune){
    		if (include)
    			theory.remove(cl);
    		if (!isConsequence(env, cl,theory, depthLimit, deadline, ctr)){
    			toRetain.add(cl);
    			if (include) theory.add(cl);
    		}
    	}
    //	lengthLimit=saveMaxLength;
    	return toRetain;
	}
	
	
	public static boolean isConsequence(Env env, Clause goal, 
			List<? extends Clause> axioms, int depthLimit, long deadline, List<StatCounter<Integer>> ctr) throws ParseException{
		List<Clause> topClauses=new ArrayList<Clause>();
		topClauses.addAll(IndepClause.getNegation(env, goal, true));
		
		SolProblem problem = new SolProblem(env, axioms, topClauses);
		
		solve(env, problem, depthLimit, deadline, ctr, null);
		
		return (problem.getStatus()==ExitStatus.UNSATISFIABLE);
	}

	
	
	
	
	public static void solve(Env env, CFP problem, int depthLimit, long deadline, List<StatCounter<Integer>> statCtr, CommunicationModule cAg){
		SolarDistr s;
		
		long timeLimit= (deadline- System.currentTimeMillis());
		if (deadline!=-1){
			if (timeLimit<0)
				timeLimit=50;
			problem.getOptions().setTimeLimit(timeLimit);
		//	System.out.println("Time Limit :"+ timeLimit);
		}
		
			
		if (depthLimit>=0)
			problem.getOptions().setDepthLimit(depthLimit);
		//problem.getOptions().setMaxLenConseqs(lengthLimit);
		problem.getOptions().setStrategy(Strategy.DF);
	/*	while (busy){
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		busy=true; */
	//	notifyAll();
		if(Thread.currentThread().isInterrupted())
			return;
		if (verbose) System.out.println(">>>>>>>>>>>> Solving at depth : "+depthLimit+"( real "+problem.getOptions().getDepthLimit()+")");
		s = new SolarDistr(env, problem);
		List<Long> measures=new ArrayList<Long>();
		if (statCtr!=null)
			for (StatCounter<Integer> ctr:statCtr){
				int k=ctr.getKey();
				measures.add(getSolStat(s,k));
		}
		//for(int i = 0; i < env.getDebug().length; i++)
			//env.getDebug()[i] = false;
		try {
			if(cAg!=null){
				s.solve(cAg);
				
			}
			else s.solve();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(Thread.currentThread().isInterrupted())
			return;
		if (statCtr!=null)
			for (int i=0;i<statCtr.size();i++){		
				int k=statCtr.get(i).getKey();
				measures.set(i,getSolStat(s,k)-measures.get(i));
				statCtr.get(i).inc(measures.get(i).intValue());
		}
		
		if (verbose) System.out.println(">>>>>>>>>>>> Solve - measure : "+measures);
	//	busy=false;
	//	notifyAll();
	}
	
	public static Long getSolStat(SOLAR s, int key){
		if (key>0)
			return s.getStats().get(key);
		switch (key){
		case SOLST_CPU_TIME:
			return s.getCPUTime();
		case SOLST_INF:
			return s.getStats().inf();
		}
		return new Long(0);
	}
	
	public static boolean verbose=true;
	public static int lengthLimit=-1;
	public static boolean depthterm=false;
	
	
}
