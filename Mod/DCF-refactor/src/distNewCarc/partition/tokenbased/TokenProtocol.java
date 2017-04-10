package distNewCarc.partition.tokenbased;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.nabelab.solar.Clause;
import org.nabelab.solar.parser.ParseException;

import systemStructure.HierarchicalAgent;
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


public class TokenProtocol extends BaseMainProtocol implements CFMessageTypes{

	
	public TokenProtocol(CommunicationModule cAg, PBICFAgent ag, HierarchicalAgent hAg, CanalComm output) {
		super(cAg);
		this.ag=ag;
		this.output=output;
		order=new CircuitAgent(hAg);
	}

	@Override
	public boolean gereParProtocol(Message<?> m) {
		if (m instanceof CFTokenBasedMessage){
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
	public void receiveMessage(Message<?> m) {
		if (m instanceof CFTokenBasedMessage){
			switch(m.getCode()){
			case PBM_SEND_CLAUSES:
				receiveCl(((CFTokenBasedMessage)m).getArgument());
				break;
			case PBM_SEND_CONSEQS:
				break;
			}
		}
		if (m instanceof SystemMessage){
			switch(m.getCode()){
			case SystemMessageTypes.SYS_LAUNCH:
				Collection<Clause> emptySet=new ArrayList<Clause>();
				
				TokenMessageArgument initArg=new TokenMessageArgument(emptySet, FORWARD, new HashSet<CanalComm> (), new HashSet<CanalComm> ());
				receiveCl(initArg);
			}
		}				

	}

	@Override
	public void initProtocol() {
		firstRun=true;
	}

	
	
	
	
	
	protected static final int FORWARD=1;
	protected static final int BACKWARD=-1;
	
	protected void receiveCl(TokenMessageArgument arg){
		if(Thread.currentThread().isInterrupted())
			return;
		boolean allFlagged=false;
		int dir=arg.getDirection();
		Collection<Clause> newCl=new ArrayList<Clause>();
		Collection<Clause> tempCsq=new ArrayList<Clause>();
		//Initialisation
		if (firstRun) {
			firstRun=false;
			allFlagged=true; // each agent should run at least once in the given order
			newCl.addAll(ag.getAllTopClauses());
			newCsq.put(FORWARD, new ArrayList<Clause>());
			newCsq.put(BACKWARD, new ArrayList<Clause>());
		}
		
		for(Clause cl:arg.getSentCl())
			newCl.add(new Clause(ag.getEnv(), cl));	//this is necessary because of the vartable
		
		//newCl.addAll(arg.getSentCl());
		//Computing new consequence
		try {
			tempCsq=ag.computeNewCons(newCl);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(Thread.currentThread().isInterrupted())
			return;
		//Attributing new cons to relevant direction
		CanalComm next=order.nextAg(dir);
		CanalComm prev=order.nextAg(-dir);
		for (Clause cl:tempCsq){
			boolean unaffected=true;
			if (ag.canResolve(prev,cl)){
				newCsq.get(-dir).add(cl);
				arg.getNextFlags().add(cAg.getComm());
				unaffected=false;
			}			
			if (ag.canResolve(next,cl) || unaffected || ag.isPossibleOutput(cl)){
				newCsq.get(dir).add(cl);
			}
		}
		arg.getCurrentFlags().remove(cAg.getComm());
		if(Thread.currentThread().isInterrupted())
			return;
		if (!newCsq.get(dir).isEmpty() || !arg.getCurrentFlags().isEmpty() || (allFlagged)){
			//Sending new cons in current direction (and reinit newCons(dir))
			ag.updateListNewCons(newCsq.get(dir));
			Collection<Clause> sentCl=new ArrayList<Clause>();
			sentCl.addAll(newCsq.get(dir));
			newCsq.get(dir).clear();
			TokenMessageArgument newArg=new TokenMessageArgument(sentCl, dir, arg.getCurrentFlags(),arg.getNextFlags());
			cAg.send(new CFTokenBasedMessage(CFMessageTypes.PBM_SEND_CLAUSES,newArg), next);			
		}
		else {
			// changing direction
			arg.getNextFlags().remove(cAg.getComm());
			if (!newCsq.get(-dir).isEmpty() || !arg.getNextFlags().isEmpty()) {
				//Sending new cons in reverse direction (and reinit newCons(dir))
				ag.updateListNewCons(newCsq.get(-dir));
				Collection<Clause> sentCl=new ArrayList<Clause>();
				sentCl.addAll(newCsq.get(-dir));
				newCsq.get(-dir).clear();
				TokenMessageArgument newArg=new TokenMessageArgument(sentCl, -dir, arg.getNextFlags(),new HashSet<CanalComm>());
				cAg.send(new CFTokenBasedMessage(CFMessageTypes.PBM_SEND_CLAUSES,newArg), prev);							
			}
			else {
				//Termination : send output
				ArrayList<Clause> csqBatch=new ArrayList<Clause>();
				for (Clause cl:ag.getListConseq()){			
					if (ag.isPossibleOutput(cl))
						csqBatch.add(cl);
				}
				if (!csqBatch.isEmpty())
					cAg.send(new CFMessage(CFMessageTypes.PBM_SEND_CONSEQS,csqBatch), output);
				cAg.send(new SystemMessage(SystemMessageTypes.SYS_FINISH,cAg.getComm()),cAg.commSystem);
			}
		}
	}
	
	
	protected boolean firstRun=true;
	protected PBICFAgent ag;
	protected CircuitAgent order;
	protected CanalComm output;
	
	protected HashMap<Integer,Collection<Clause>> newCsq=new HashMap<Integer,Collection<Clause>>();
	public static boolean verbose = true;


	@Override
	public void send(Message<?> m) {
		// TODO Auto-generated method stub
		
	}

	
}
