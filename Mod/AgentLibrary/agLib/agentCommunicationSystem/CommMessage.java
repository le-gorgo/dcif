package agLib.agentCommunicationSystem;

public class CommMessage extends MessageBase <Object> {

	public CommMessage(int code, Object argument, CanalComm sender){
		super(code, argument,sender);
	}
	@Override
	public int getSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String toString() {
		switch(code){
		case 1:
			return "Send Clauses : "+argument.toString();
		case 2:
			return "Send Consequences : "+argument.toString();
		}
		return "Unknown message";
	}

}
