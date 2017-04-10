package agLib.agentCommunicationSystem.protocols;

import java.util.ArrayList;
import java.util.Collection;


import jdk.nashorn.internal.runtime.ParserException;
import agLib.agentCommunicationSystem.CanalComm;
import agLib.agentCommunicationSystem.Message;


public interface MainProtocol {
	public void initProtocol();
	public void receiveMessage(Message<?> m) throws  Exception;
	public boolean gereParProtocol(Message<?> m);
	//public boolean protocolTrigger(TriggerArgument trigger, boolean ownModif);
	// network and competence evolution
//	public boolean useCompetence();
	public void addNewNeighbour(CanalComm ag);
	public void reconnectNeighbour(CanalComm ag);
	public void disconnectNeighbour(CanalComm ag);
	public void destroyNeighbour(CanalComm ag);
	//public <Conseq> void send(Collection<Clause> newCons);
	public void send(Message<?> m);

	
}

