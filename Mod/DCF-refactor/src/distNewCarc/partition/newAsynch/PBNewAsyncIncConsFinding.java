package distNewCarc.partition.newAsynch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.nabelab.solar.Clause;

import distNewCarc.partition.IncConsFindingAgent;
import distNewCarc.partition.PBICFAgentBuilder;
import distNewCarc.partition.asynchronous.AsyncProtocol;
import logicLanguage.IndepClause;
import problemDistribution.DCFProblem;
import problemDistribution.DistributedConsequenceFindingProblem;
import problemDistribution.XMLDistributedProblem;
import solarInterface.SolProblem;
import stats.ConsFindingAgentStats;
import systemStructure.AgentBuilder;
import systemStructure.PartitionGraph;
import systemStructure.Tree;
import agLib.agentCommunicationSystem.CanalComm;
import agLib.agentCommunicationSystem.CommunicationModule;
import agLib.agentCommunicationSystem.SystemMessage;
import base.ActivityChecker;
import base.CFSystemAgent;




public class PBNewAsyncIncConsFinding {
	
	public PBNewAsyncIncConsFinding(){
		super();
	}
	
	public PBNewAsyncIncConsFinding(DistributedConsequenceFindingProblem<SolProblem> data, boolean newCons, boolean inDepthPrune, long deadline) throws Exception{
		system=new CFSystemAgent();
		sys = system.getComm();
		
		//other data (useful??)
		this.nbAgents = data.getNbAgents();
		stats = new ConsFindingAgentStats();
		
		// Set Partition Graph (and create agents)
		newConsAsAxiom=newCons;
		inDepthPruning=inDepthPrune;
		AgentBuilder<IncConsFindingAgent> builder=new PBICFAgentBuilder(newConsAsAxiom,inDepthPruning);
		graph=new PartitionGraph<IncConsFindingAgent>(data, "Clique_", sys,  builder,deadline);
		//set protocol
		for (IncConsFindingAgent ag:graph.getAgents()){
			ag.setProtocol(new NewAsyncProtocol(ag.getCommModule(),ag,sys));
		}
			//compute useful languages
		graph.computeOutputInputLanguages();
			//change structure if necessary (and adapt networks of cAg) : here, not needed
		    //set agent output and input languages (and set hierarchy if needed)
		for (IncConsFindingAgent ag:graph.getAgents()){
			int id=graph.identifier(ag);
			ag.setOutputLanguage(graph.getOutputLanguage(id));
			for (IncConsFindingAgent ag2:graph.getAgents()){
				if (ag!=ag2){
					int id2=graph.identifier(ag2);
					ag.setInputLanguage(graph.getInputLanguage(id2), ag2.getComm());
				}
			}				
		}
		cSys = graph.getCommunicationModule();
		system.setCommunicationModule(cSys);
		
		//create ActivityChecker
		activ=new ActivityChecker(cSys, graph.getAgents());
	}
	
	
	
	/**
	 * Start the thread and the experiment.
	 * @return false if timeout
	 */
	public synchronized boolean startExpe(long deadline){
		for (IncConsFindingAgent ag:graph.getAgents())
			ag.start();
		system.start();
		activ.start();
		
		while (!system.finished){
			try {
				wait(1000);
				if(deadline != -1 && System.currentTimeMillis() > deadline){
					//for (IncConsFindingAgent ag:graph.getAgents())
						//ag.stop();
					activ.stop();
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
	

	

	protected PartitionGraph<IncConsFindingAgent> graph;
	protected CommunicationModule cSys;
	protected CanalComm sys;
	protected CFSystemAgent system;
	protected ActivityChecker activ;
	protected int nbAgents;
	protected ConsFindingAgentStats stats;
	public static boolean verbose=false;
	
	protected boolean newConsAsAxiom=false;
	protected boolean inDepthPruning=false;
}