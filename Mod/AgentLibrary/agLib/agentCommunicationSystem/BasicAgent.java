package agLib.agentCommunicationSystem;

/**
 * This is a basic implementation of an agent, ensuring that the proper methods 
 * from Agent and Runnable are done (though in a basic way).
 * It just runs by checking its CanalComm and processing any relevant 
 * incoming message through its main protocol. 
 * It is thus communicationally functional, but has no data, something that
 * has to be corrected in most applications.
 * It is meant to be extended by agents fitting the actual application.
 * It factorizes the common methods of runnable (creating and running the agent
 * Thread) and includes communication capabilites (with infrastructure for 
 * using protocols).
 * Note that start() and finish() would often have to be overridden to adjust to an
 * application, but most other methods might just be inherited (especially run())
 * 
 *  
 *   * @author gauvain bourgne
 */


public class BasicAgent implements ThreadAgent,SystemMessageTypes{
	

	public BasicAgent(){
		super();
	}
	
	public BasicAgent(String name){
		this.name=name;
	}
	
	public void start() {
		if (agentThread == null) {
			agentThread = new Thread(this, name);
			agentThread.start();
		}
	}
	
	public void stop() {
		// TODO Auto-generated method stub
		if(agentThread != null){
			agentThread.interrupt();
			finish();
		}
	}
	
	public void finish() {
		agentThread=null;
	}
	public void run(){
		
	    Thread myThread = Thread.currentThread();
        while (agentThread == myThread) {

        	Message<?> m= cAg.getComm().get();
    		if (agentThread != null && !agentThread.isInterrupted() && cAg.gereParProtocol(m)) {
        		cAg.receiveMessage(m);
        	}
        	if(agentThread == null || agentThread.isInterrupted()){
    			m = null;
    		}
        	if (m!=null) {
        		if (m instanceof SystemMessage) switch(m.getCode()){
        		case SYS_FINISH:
        		//case SYS_LAUNCH:
        			finish();
        		break;
        		}
        		m=null;
        	}
        }
	}
	

	public boolean isAlive() {
		if (agentThread==null) {
			getComm().die();
		}
		return agentThread!=null;
	}
	
	public void finalize(){
		agentThread=null;
		cAg=null;
		//comm.finalize();
	}
	
	public CanalComm getComm() {
		return cAg.getComm();
	}
	
	public String toString(){
		return this.name;
	}
	
	public boolean isDormant(){
		return dormant;
	}
	public synchronized void setDormant(boolean val){
		dormant=val;
		notifyAll();
	}
	public Thread getThread(){
		return agentThread;
	}
	protected volatile Thread agentThread = null;
	public String name;
	
	protected CommunicationModule cAg;
	
	protected boolean dormant=false;
	//protected MainProtocol protocol;

}
