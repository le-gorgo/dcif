package agLib.agentCommunicationSystem.protocols;

import agLib.agentCommunicationSystem.CommunicationModule;
import agLib.agentCommunicationSystem.Message;

public class EmptyMainProtocol extends BaseMainProtocol {

	public EmptyMainProtocol(CommunicationModule cAg) {
		super(cAg);
	}

	@Override
	public boolean gereParProtocol(Message<?> m) {
		return false;
	}

	@Override
	public void receiveMessage(Message<?> m) {
	}

	@Override
	public void initProtocol() {
	}

	public void send(Message<?> m) {
		// TODO Auto-generated method stub
		
	}

}
