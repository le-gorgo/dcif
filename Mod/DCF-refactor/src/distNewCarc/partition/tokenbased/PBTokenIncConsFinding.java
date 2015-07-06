/**

 * 

 */
package distNewCarc.partition.tokenbased;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.nabelab.solar.Clause;
import org.nabelab.solar.PLiteral;
import org.nabelab.solar.pfield.PField;

import distNewCarc.partition.HPBICFAgentBuilder;
import distNewCarc.partition.HierarchicalIncConsFindingAgent;
import distNewCarc.partition.IncConsFindingAgent;
import distNewCarc.partition.PBICFAgentBuilder;
import logicLanguage.IndepClause;
import logicLanguage.IndepLiteral;
import problemDistribution.DCFProblem;
import problemDistribution.DistributedConsequenceFindingProblem;
import problemDistribution.XMLDistributedProblem;
import solarInterface.IndepPField;
import solarInterface.SolProblem;
import stats.ConsFindingAgentStats;
import systemStructure.AgentBuilder;
import systemStructure.HierarchicalAgent;
import systemStructure.PartitionGraph;
import systemStructure.Tree;
import agLib.agentCommunicationSystem.CanalComm;
import agLib.agentCommunicationSystem.CommunicationModule;
import agLib.agentCommunicationSystem.SystemMessage;
import base.ActivityChecker;
import base.CFSystemAgent;

/**
 * @author Gauvain Bourgne
 *
 */
public class PBTokenIncConsFinding  {
	
	public PBTokenIncConsFinding(){
		super();
	}
	
	public PBTokenIncConsFinding(DistributedConsequenceFindingProblem<SolProblem> data, boolean newCons, boolean inDepthPrune, List<Integer> order, long deadline) throws Exception{	
		
		//create system agent
		system=new CFSystemAgent();
		sys = system.getComm();
		system.launchAll=false;
		system.startingAg=order.get(0);
		
		//other data (useful??)
		this.nbAgents = data.getNbAgents();
		stats = new ConsFindingAgentStats();
		
		// Set Partition Graph (and create agents)
		newConsAsAxiom=newCons;
		inDepthPruning=inDepthPrune;
		AgentBuilder<HierarchicalIncConsFindingAgent> builder=new HPBICFAgentBuilder(newConsAsAxiom,inDepthPruning);
		graph=new PartitionGraph<HierarchicalIncConsFindingAgent>(data, "Clique_", sys,  builder,deadline);
		//set protocol
		for (HierarchicalIncConsFindingAgent ag:graph.getAgents()){
			ag.setProtocol(new TokenProtocol(ag.getCommModule(),ag,ag,sys));
		}
			//compute useful languages
		graph.computeLocalCommLanguages();
			//change structure if necessary
		graph.makeCircuitFormation(order);
			//set hierarchy
		CanalComm previous=graph.getAgent(order.get(order.size()-1)).getComm();
		for (int i=0;i<order.size();i++){
			HierarchicalAgent ag=graph.getAgent(order.get(i));
			Collection<CanalComm> prev=new ArrayList<CanalComm>();
			prev.add(previous);
			ag.setUpperAgents(prev);
			previous=ag.getComm();
		}
		    //set agent output and input languages (difference between root and leaves)		
		for (HierarchicalIncConsFindingAgent ag:graph.getAgents()){
			int id=graph.identifier(ag);
			PField temp;
			List<PLiteral> outputLits=new ArrayList<PLiteral>();			
			for (HierarchicalIncConsFindingAgent neighbor:graph.getVoisins(ag)){
				int id2=graph.identifier(neighbor);
				temp=graph.getCommLanguage(id,id2);
				ag.setInputLanguage(temp, neighbor.getComm());
				outputLits.addAll(temp.getPLiterals());
			}
			temp= IndepPField.createPField(ag.getEnv(), ag.getOptions(), outputLits);
			ag.setOutputLanguage(temp);
		}
		cSys = graph.getCommunicationModule();
		system.setCommunicationModule(cSys);
	}
	
	
	
	/**
	 * Start the thread and the experiment.
	 * @return false if timed out
	 */
	public synchronized boolean startExpe(long deadline){
		for (IncConsFindingAgent ag:graph.getAgents())
			ag.start();
		system.start();
		
		while (!system.finished){
			try {
				wait(1000);
				if(deadline != -1 && System.currentTimeMillis() > deadline){
					//for (IncConsFindingAgent ag:graph.getAgents())
						//ag.stop();
					cSys.send(new SystemMessage(CFSystemAgent.SYS_TIMEUP,null), sys);
					while(!system.finished)
						wait(100);
					break;
				}
			} catch (InterruptedException e) { }	
		}
		return !system.timeOut;
	}
	
	public Collection<Clause> getOutput(){
		return system.consequences;
	}
	
	public List<ConsFindingAgentStats> getAllStats(){
		List<ConsFindingAgentStats> result=new ArrayList<ConsFindingAgentStats>();
		for (IncConsFindingAgent ag:graph.getAgents()){
			result.add(ag.stats);
		}
		return result;
	}
	

	/*
	public static void printHelp(){
		System.out.println("Usage :");
		System.out.println("    pb-dcf [Options] -NbAg=N filename.dcf");
		System.out.println("-NbAg=N  indicates the number of agents (must be compatible with the given file)");
		System.out.println("Options");
		System.out.println("-method=C  indicates which method to use \n"+
						   "               S for sequential (default) \n"+
						   "               P for parallel\n" +
						   "		       H for hybrid");
		System.out.println("-graph=prefix  indicates which graph topology to use (name before _ in filename of xml graph) (by default Clique) \n");
		System.out.println("-root=N  indicates which agent should be taken as root (by default, agent 0) \n");
		System.out.println("-d=N  indicates the depth limit");
		System.out.println("-l=N  indicates the length limit");
		System.out.println("-inc  indicates to use incremental computations");
		System.out.println("-prune  indicates to use pruning of consequences");
		System.out.println("-vcomm  verbose communications");
		System.out.println("-vsolv  verbose computations");
		System.out.println("-vagent  verbose agent");
	}
	

	public static void main(String [] args) {

		int i=0;
		int nbAgents = 0;
		boolean prune=false;
		boolean inc=false;
		int root=0;
		int depth=-1;
		int length=-1;
		int method=LocalPBProtocol.SEQUENTIAL;
		String graphName = "Clique_";
		CanalComm.verbose=false;
		CFSolver.verbose=false;
		PBAgent.verbose=false;
		
		while (i<args.length && args[i].startsWith("-")) {
			if (args[i].startsWith("-nbAg=")){
				nbAgents=Integer.parseInt(args[i].substring(args[i].indexOf("=")+1));
				i++;
				continue;
			}
			if (args[i].trim().equals("-prune")){
				prune=true;
				i++;
				continue;
			}
			if (args[i].trim().equals("-vcomm")){
				CanalComm.verbose=true;
				i++;
				continue;
			}
			if (args[i].trim().equals("-vsolv")){
				CFSolver.verbose=true;
				i++;
				continue;
			}
			if (args[i].trim().equals("-vagent")){
				PBAgent.verbose=true;
				i++;
				continue;
			}
			if (args[i].trim().equals("-inc")){
				inc=true;
				i++;
				continue;
			}
			if (args[i].startsWith("-root=")){
				root=Integer.parseInt(args[i].substring(args[i].indexOf("=")+1));
				i++;
				continue;
			}
			if (args[i].startsWith("-d=")){
				depth=Integer.parseInt(args[i].substring(args[i].indexOf("=")+1));
				i++;
				continue;
			}
			if (args[i].startsWith("-l=")){
				length=Integer.parseInt(args[i].substring(args[i].indexOf("=")+1));
				i++;
				continue;
			}
			if (args[i].startsWith("-method=")){
				char m=args[i].substring(args[i].indexOf("=")+1).charAt(0);
				switch(m){
				case 'S':case 's':
					method=LocalPBProtocol.SEQUENTIAL;
				break;
				case 'P':case 'p':
					method=LocalPBProtocol.PARALLEL;
				break;
				case 'H':case 'h':
					method=LocalPBProtocol.HYBRID;
				}
				i++;
				continue;
			}
			if (args[i].startsWith("-graph=")){
				graphName=args[i].substring(args[i].indexOf("=")+1).trim();
				if (!graphName.endsWith("_"))
					graphName=graphName+"_";
				
				i++;
				continue;
			}
			else{
				printHelp();
				return;
			}
		}
		if (args.length<=i){
			printHelp();
			return;			
		}
		String filenameWithExt=args[i].trim();

		PBAgent.refinedPF=true;
		PBAgent.pruneCsq=prune;
		PBAgent.incremental=inc;
		try {
			new PBIncConsFinding(filenameWithExt, graphName, root, nbAgents, method, length, System.currentTimeMillis()+600000);
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	*/

	protected PartitionGraph<HierarchicalIncConsFindingAgent> graph;
	protected CommunicationModule cSys;
	protected CanalComm sys;
	protected CFSystemAgent system;
	protected int nbAgents;
	protected ConsFindingAgentStats stats;
	public static boolean verbose=false;
	protected boolean newConsAsAxiom=false;
	protected boolean inDepthPruning=false;

}

