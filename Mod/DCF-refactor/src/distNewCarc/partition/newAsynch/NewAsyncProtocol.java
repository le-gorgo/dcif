package distNewCarc.partition.newAsynch;


import java.util.ArrayList;
import java.util.Collection;

import org.nabelab.solar.Clause;
import org.nabelab.solar.Conseq;
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


public class NewAsyncProtocol extends BaseMainProtocol implements CFMessageTypes{

	
	public NewAsyncProtocol(CommunicationModule cAg, PBICFAgent ag, CanalComm output) {
		super(cAg);
		this.ag=ag;
		this.output=output;
	}

	@Override
	public boolean gereParProtocol(Message<?> m) {
		if (m instanceof CFMessage){
			switch(m.getCode()){
			case PBM_SEND_CLAUSES:
				return true;
			case PBM_SEND_CONSEQS:
				return false;
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
//				m = new SystemMessage(SystemMessageTypes.SYS_FINISH, cAg.commAgent);
//				cAg.commAgent.enqueue(commSystem, m);
				break;
			case SystemMessageTypes.SYS_FINISH:
				ag.setDormant(true);
//				m = new SystemMessage(SystemMessageTypes.SYS_FINISH, cAg.commAgent);
//				cAg.commAgent.enqueue(commSystem, m);
				//ag.getComm().getAgentThread().destroy();
				break;
			}
		}				
	}
//	@Override
//	public void receiveMessage(Message<?> m) throws Exception {
//		loop:while (true){
//			if (m instanceof SystemMessage){
//				switch(m.getCode()){
//				case SystemMessageTypes.SYS_LAUNCH:
//					receiveMessage(new CFMessage(PBM_SEND_CLAUSES, ag.getAllTopClauses()));
//					break;
//				case SystemMessageTypes.SYS_FINISH:
//					break loop;
//				}
//			}
//			if(m instanceof CFMessage){
//				switch(m.getCode()){
//				case PBM_SEND_CLAUSES:
//					receiveCl(((CFMessage)m).getArgument());
//					break;
//				case PBM_SEND_CONSEQS:
//					break;
//				}
//			}
//			
//			break;
//		}
//	}

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
//		//newCl.addAll(sentCl);
		if (Thread.currentThread().isInterrupted()){
			ag.setDormant(true);
			return;
		}
		if (newCl.isEmpty()){
			ag.setDormant(true);
//			while(cAg.getComm().isEmpty()){
//				Message<?> m= cAg.getComm().get();
//				receiveMessage(m);
				return;
			//}
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
		ArrayList<Clause> csqBatch=new ArrayList<Clause>();
		for (Clause cl:newCsq){			
			if (ag.isPossibleOutput(cl))
				csqBatch.add(cl);
		}
	
		if (!csqBatch.isEmpty())
			cAg.send(new CFMessage(CFMessageTypes.PBM_SEND_CONSEQS,csqBatch), commSystem);
		ag.updateListNewCons(newCsq);
		ag.setDormant(true);
	}	
	
	public void send(Collection<Clause> newCsq) {
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
		
	}
	
	
	protected boolean firstRun=true;
	protected PBICFAgent ag;
//	protected CommunicationModule cAg;
	protected CanalComm output;
	
	public static boolean verbose = true;

	public void send(Message<?> m) {
		CFMessage ms = new CFMessage(m.getCode(), (Collection<Clause>) m.getArgument());
		Collection<Clause> sentCl = ms.getArgument();
		send(sentCl);
		

	}
	
}
