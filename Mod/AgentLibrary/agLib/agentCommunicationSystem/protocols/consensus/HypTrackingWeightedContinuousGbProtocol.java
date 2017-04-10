package agLib.agentCommunicationSystem.protocols.consensus;

import java.util.ArrayList;
import java.util.List;

import agLib.agentCommunicationSystem.CanalComm;
import agLib.agentCommunicationSystem.CommunicationModule;
import agLib.agentCommunicationSystem.Message;
import agLib.agentCommunicationSystem.SystemMessage;
import agLib.agentCommunicationSystem.SystemMessageTypes;
import agLib.agentCommunicationSystem.protocols.LocalProtocol;


public class HypTrackingWeightedContinuousGbProtocol<Hypothesis> extends
		HypTrackingMainProtocol<Hypothesis> implements GlobalWeightedMessageTypes,SystemMessageTypes {

	public HypTrackingWeightedContinuousGbProtocol(CommunicationModule cAg, ConsensusAgent<Hypothesis> agent) {
		super(cAg);
		currentInterlocutor=null;
		waitingList=new ArrayList<GlobalWeightedMessage>();
		this.ag=agent;
	}
	
	public HypTrackingWeightedContinuousGbProtocol(CommunicationModule cAg, ConsensusAgent<Hypothesis> agent, LocalProtocol baseLocalProt) {
		super(cAg);
		currentConv=baseLocalProt;
		currentInterlocutor=null;
		waitingList=new ArrayList<GlobalWeightedMessage>();
		this.ag=agent;
	}
	
	public void setBasicLocalProtocol(LocalProtocol baseLocalProt){
		currentConv=baseLocalProt;
	}

	protected CanalComm getCandidate(){
		for (int i=0; i<agentsCompetents.size();i++){
			if (!ag.equalOwnCssValue(confirmationsHyp.get(i),agentsCompetents.get(i))){
				return agentsCompetents.get(i);
			}
		}
		return null;
	}
	public void initProtocol(){
		CanalComm target=getCandidate();
		
		if (target!=null){
			sentWeight=Math.random();
			GlobalWeightedMessage m=new GlobalWeightedMessage(GWM_WEIGHTEDREQUEST, new Double(sentWeight));
			cAg.send(m, target);
		}
		else {
			// TODO send message to system ?
			ag.setDormant(true);
		}
				
	}
	
	@Override
	public boolean gereParProtocol(Message<?> m) {
		if (m==null) return false;
		if (m instanceof GlobalWeightedMessage){
			switch(m.getCode()){
			case GWM_WEIGHTEDREQUEST:
			case GWM_ACCEPTREQUEST:
			case GWM_ENDLOCALCONV:
				return true;
			}
		}
		if (m instanceof SystemMessage){
			switch(m.getCode()){
			case SYS_LAUNCH:
				return true;
			}
		}
		if (localMessage(m))
			return true;	
		return false;
	}

	public boolean localMessage(Message<?> m){
		return (m!=null 
				&& currentInterlocutor!=null 
				&& m.getSender().equals(currentInterlocutor)
				&& currentConv.gereParProtocol(m));
	}
	
	public boolean busy(){
		return currentInterlocutor!=null;
	}
	
	@Override
	public void receiveMessage(Message<?> m) {
		if (m==null) return;
		ag.setDormant(false);
		if (localMessage(m))
			currentConv.receiveMessage(m);
		if (m!=null && m instanceof GlobalWeightedMessage){
			GlobalWeightedMessage gm=(GlobalWeightedMessage)m;
			switch(gm.getCode()){
			case GWM_WEIGHTEDREQUEST:
				receiveRequest(gm);
			break;
			case GWM_ACCEPTREQUEST:
				receiveAccept(gm);
			break;
			case GWM_ENDLOCALCONV:
				endLocalConv();
			break;
			}
		}
		if (m!=null && m instanceof SystemMessage){
			switch(m.getCode()){
			case SYS_LAUNCH:
				initProtocol();
			}
		}
		
	}
	
	protected void receiveRequest(GlobalWeightedMessage gm){
		double w=((Double)gm.getArgument()).doubleValue();
		if (!busy() && w>sentWeight) {
			currentInterlocutor=gm.getSender();
			currentConv=currentConv.acceptProtocol(currentInterlocutor,cAg.stats);
			cAg.send(new GlobalWeightedMessage(GWM_ACCEPTREQUEST,null), gm.getSender());
		}
		else {
			waitingList.add(gm);
		}
	}
	
	protected void receiveAccept(GlobalWeightedMessage gm){
		if (!busy()) {
			//Init new protocol
			currentInterlocutor=gm.getSender();
			currentConv=currentConv.initProtocol(gm.getSender(),cAg.stats);
			sentWeight=0;
			currentConv.start();
		}
		else {
			waitingList.add(0, gm);
		}
	}
	
	protected void endLocalConv(){
		currentInterlocutor=null;
		//Check WaitingList
		if (!waitingList.isEmpty()){
			Message<?> m=waitingList.get(0);
			waitingList.remove(0);
			receiveMessage(m);
		}
		else {
			initProtocol();
		}
	}
	
	protected LocalProtocol currentConv;
	protected CanalComm currentInterlocutor;
	protected List<GlobalWeightedMessage> waitingList;
	protected double sentWeight;
	protected ConsensusAgent<Hypothesis> ag;
	public void send(Message<?> m) {
		// TODO Auto-generated method stub
		
	}

}
