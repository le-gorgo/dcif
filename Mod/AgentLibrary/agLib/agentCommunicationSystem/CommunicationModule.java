package agLib.agentCommunicationSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;




import agLib.agentCommunicationSystem.protocols.MainProtocol;


public class CommunicationModule implements SystemMessageTypes{
	
	public CanalComm commAgent, commSystem;
	
	private Network commBC;
	
	public CommStatsUpdater stats;
	
	public MainProtocol gbProtocol;
	
	public static final int INDICE_SYSTEM=-10;
	
	public boolean system;
	
	public CommunicationModule(CanalComm ag, CanalComm syst, Network net, CommStatsUpdater stats){
		commAgent=ag;
		commSystem=syst;
		gbProtocol=null;
		system=(ag==syst);
		this.stats=stats;
		commBC=net;
	}
	
	public void setProtocol(MainProtocol p){
		gbProtocol=p;
	}
	
	public boolean gereParProtocol(Message<?> m){
		if (m!=null) {
    		switch(m.getCode()){
    		case SYS_CRASH:
    		case SYS_DISCONNECT:
    		case SYS_NEW_NEIGHBOUR:
    		case SYS_RECONNECT:
    		case SYS_FINISH:
    		case SYS_START:
    			return true;
    		}    		
    	}
		return gbProtocol.gereParProtocol(m);
	}
	
	public void receiveMessage(Message<?> m) {
   	  if (m!=null) {
   		if (!system && stats!=null)
   			stats.receivedMessages(m);
   		 if (gbProtocol.gereParProtocol(m))
			try {
				gbProtocol.receiveMessage(m);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
   		 if (m instanceof SystemMessage) 
   	 switch(m.getCode()){
 		case SYS_CRASH: gbProtocol.destroyNeighbour(m.getSender());
 		break;
		case SYS_DISCONNECT: gbProtocol.disconnectNeighbour(m.getSender());
		break;
		case SYS_NEW_NEIGHBOUR: gbProtocol.addNewNeighbour(m.getSender());
		break;
		case SYS_RECONNECT: gbProtocol.reconnectNeighbour(m.getSender());
		break;
		case SYS_FINISH:
		break;
		case SYS_START:
			send(new SystemMessage(SYS_STARTED, commAgent), commSystem);
		break;
		
   		}
   	  }
   }
	
	public void send(Message<?> mToSend, CanalComm destinataire) {
		if (mToSend.getSender()==null) mToSend.setSender(this.commAgent);
		//stat
		if (!system && stats!=null) 
			stats.sentMessages(mToSend, 1);
		//send
		
		destinataire.enqueue(commAgent,mToSend);		
	}
	
	public void sendBC(Message<?> mToSend) {
		if (mToSend.getSender()==null) mToSend.setSender(this.commAgent);
		//choose recipients
    	List <CanalComm> v=commBC.getNeighbours(commAgent);
    	//group sending
    	groupSending(mToSend, v);
	}	
	
	public void sendBCAmong(Message<?> mToSend,List <CanalComm> filter) {
		if (mToSend.getSender()==null) mToSend.setSender(this.commAgent);
		//choose recipients
    	List <CanalComm> v=commBC.getNeighboursAmong(commAgent,filter);
    	//group sending
    	groupSending(mToSend, v);
	}	

	public void sendBCExcluding(Message<?> mToSend,List <CanalComm> filter) {
		if (mToSend.getSender()==null) mToSend.setSender(this.commAgent);
		//choose recipients
    	List <CanalComm> v=commBC.getNeighboursExcluding(commAgent,filter);
    	//group sending
    	groupSending(mToSend, v);
	}	
	
	public void groupSending(Message<?> mToSend, List<CanalComm> target){
    	//Stat
		if (!system && stats!=null) 
			stats.sentMessages(mToSend, target.size());
    	// send
		for (int i=0;i < target.size(); i++ ) {
        	if (!target.get(i).equals(commAgent)){ //TODO check if this test is needed
        		target.get(i).enqueue(commAgent,mToSend);	        		
        	}            	
    	}
	}
	
	public List<CanalComm> getNeighbours(){
		return commBC.getNeighbours(commAgent);
	}
	
	public int getNbNeighbours(){
		return commBC.getNeighbours(commAgent).size();
	}
	
	public Network getNetwork(){
		return commBC;
	}

	public  void setNetwork(Network net){
		commBC=net;
	}

	public CanalComm getComm() {
		return commAgent;
	}
	
	public void Init(){
		commBC.Init();
	}
	
	public void finalize(){
		commAgent=null;
		commSystem=null;
		commBC=null;
		stats=null;
		gbProtocol=null;
	}

	public void send(Message<?> m) {
		// TODO Auto-generated method stub
		gbProtocol.send(m);
	}
	



}