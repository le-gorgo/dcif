package distNewCarc.partition.asynchronous;

import java.util.ArrayList;
import java.util.Collection;

import org.nabelab.solar.Clause;
import org.nabelab.solar.parser.ParseException;

import distNewCarc.partition.PBICFAgent;
import logicLanguage.IndepClause;
import agLib.agentCommunicationSystem.CanalComm;
import agLib.agentCommunicationSystem.CommunicationModule;
import agLib.agentCommunicationSystem.Message;
import agLib.agentCommunicationSystem.SystemMessage;
import agLib.agentCommunicationSystem.SystemMessageTypes;
import agLib.agentCommunicationSystem.protocols.BaseMainProtocol;
import base.CFMessage;
import base.CFMessageTypes;


public class AsyncProtocol extends BaseMainProtocol implements CFMessageTypes{

	
	public AsyncProtocol(CommunicationModule cAg, PBICFAgent ag, CanalComm output) {
		super(cAg);
		this.ag=ag;
		this.output=output;
	}

	@Override
	public boolean gereParProtocol(Message<?> m) {
		if (m instanceof CFMessage){
			switch(m.getCode()){
			case PBM_SEND_CLAUSES:
			case PBM_SEND_CONSEQS:
				return true;
			}
		}		
		if (m instanceof SystemMessage){
			switch(m.getCode()){
			case SystemMessageTypes.SYS_LAUNCH:
				return true;
			}
		}		
		return false;
	}

	@Override
	public void receiveMessage(Message<?> m) throws Exception {
		if (m instanceof CFMessage){
			switch(m.getCode()){
			case PBM_SEND_CLAUSES:
				receiveCl(((CFMessage)m).getArgument());
				break;
			case PBM_SEND_CONSEQS:
				break;
			}
		}
		if (m instanceof SystemMessage){
			switch(m.getCode()){
			case SystemMessageTypes.SYS_LAUNCH:
				Collection<Clause> emptySet=new ArrayList<Clause>();
				receiveCl(emptySet);
			}
		}				
	}

	@Override
	public void initProtocol() {
		firstRun=true;
	}

	
	
	
	
	
	protected void receiveCl(Collection<Clause> sentCl) throws Exception{
		if(Thread.currentThread().isInterrupted())
			return;
		ag.setDormant(false);
		Collection<Clause> newCl=new ArrayList<Clause>();
		Collection<Clause> newCsq=new ArrayList<Clause>();
		//Initialisation
		if (firstRun) {
			firstRun=false;
			newCl.addAll(ag.getAllTopClauses());
		}
		for(Clause cl:sentCl)
			newCl.add(new Clause(ag.getEnv(), cl));	//this is necessary because of the vartable
		//newCl.addAll(sentCl);
		if (newCl.isEmpty() || Thread.currentThread().isInterrupted()){
			ag.setDormant(true);
			return;
		}
		//Computing new consequence
		try {
			newCsq = ag.computeNewCons(newCl);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(Thread.currentThread().isInterrupted()){
			ag.setDormant(true);
			return;
		}
		ag.updateListNewCons(newCsq);
		//Send relevant new consequences to other agents
		for (CanalComm target:cAg.getNeighbours()){
			Collection<Clause> clBatch=new ArrayList<Clause>();
			for (Clause cl:newCsq){
				if (ag.canResolve(target,cl))
					clBatch.add(cl);
			}
			if (!clBatch.isEmpty())
				cAg.send(new CFMessage(CFMessageTypes.PBM_SEND_CLAUSES,clBatch), target);
			if(Thread.currentThread().isInterrupted()){
				ag.setDormant(true);
				return;
			}
		}
		//Check new consequences as output
		ArrayList<Clause> csqBatch=new ArrayList<Clause>();
		for (Clause cl:newCsq){			
			if (ag.isPossibleOutput(cl))
				csqBatch.add(cl);
		}
		if (!csqBatch.isEmpty())
			cAg.send(new CFMessage(CFMessageTypes.PBM_SEND_CONSEQS,csqBatch), output);
		ag.setDormant(true);
	}
	
	
	protected boolean firstRun=true;
	protected PBICFAgent ag;
//	protected CommunicationModule cAg;
	protected CanalComm output;
	
	public static boolean verbose = true;

	@Override
	public void send(Message<?> m) {
		// TODO Auto-generated method stub
		
	}

	
}