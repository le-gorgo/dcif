package agLib.agentCommunicationSystem.protocols;

import java.util.List;

import jdk.nashorn.internal.runtime.ParserException;



import agLib.agentCommunicationSystem.CanalComm;
import agLib.agentCommunicationSystem.CommunicationModule;
import agLib.agentCommunicationSystem.Message;



public abstract class BaseMainProtocol implements MainProtocol {

	//Paramètres
	protected CommunicationModule cAg;	
	public CanalComm commSystem;
	
	public BaseMainProtocol(CommunicationModule cAg){
		this.cAg=cAg;
		commSystem=cAg.commSystem;
	}
	
	public int identifier(CanalComm inconnu){
		int indice = getRelevantNeighbours().indexOf(inconnu);
		if ((indice==-1) && (inconnu.equals(commSystem)) ) {
				indice = CommunicationModule.INDICE_SYSTEM;
		}
		return indice;
	}
	
	public List<CanalComm> getRelevantNeighbours(){
		return cAg.getNeighbours();
	}

	public void addNewNeighbour(CanalComm ag){}
	public void disconnectNeighbour(CanalComm ag){}
	public void reconnectNeighbour(CanalComm ag){}
	public void destroyNeighbour(CanalComm ag){}
	
	
	public abstract boolean gereParProtocol(Message<?> m);
	
	public abstract void receiveMessage(Message<?> m) throws Exception;

	public abstract void initProtocol();
}
