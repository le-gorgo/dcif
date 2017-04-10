package distNewCarc.partition.starbased;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.nabelab.solar.Clause;
import org.nabelab.solar.parser.ParseException;

import logicLanguage.IndepClause;
import agLib.agentCommunicationSystem.CanalComm;
import agLib.agentCommunicationSystem.CommunicationModule;
import agLib.agentCommunicationSystem.Message;
import agLib.agentCommunicationSystem.SystemMessage;
import agLib.agentCommunicationSystem.SystemMessageTypes;
import agLib.agentCommunicationSystem.protocols.BaseMainProtocol;
import base.CFMessage;
import base.CFMessageTypes;
import distNewCarc.partition.PBICFAgent;

public class StarBasedProtocol extends BaseMainProtocol implements CFMessageTypes{

		
	public StarBasedProtocol(CommunicationModule cAg, PBICFAgent ag, CanalComm output, CanalComm root) {
			super(cAg);
			this.ag=ag;
			this.root=root;
			this.output=output;
			isRoot=root==cAg.getComm();		
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
		public void receiveMessage(Message<?> m) {
			if (m instanceof CFMessage){
				switch(m.getCode()){
				case PBM_SEND_CLAUSES:
					if (isRoot)
						receiveClAsRoot(((CFMessage)m).getArgument(),m.getSender());
					else
						receiveClAsLeaf(((CFMessage)m).getArgument());
					break;
				case PBM_SEND_CONSEQS:
					break;
				}
			}
			if (m instanceof SystemMessage){
				switch(m.getCode()){
				case SystemMessageTypes.SYS_LAUNCH:
					if (isRoot) {
						Collection<Clause> emptySet=new ArrayList<Clause>();
						receiveClAsRoot(emptySet,null);
					}
					
				}
			}
		}

		@Override
		public void initProtocol() {
			firstRun=true;
		}

		
		
		
		
		
		protected void receiveClAsRoot(Collection<Clause> sentCl, CanalComm sender){
			if(Thread.currentThread().isInterrupted())
				return;
			//Collection<IndepClause> newCl=new ArrayList<IndepClause>();
			Collection<Clause> newCsq=new ArrayList<Clause>();
			boolean sendAll=firstRun;
			
			//Initialisation
			if (firstRun) {
				firstRun=false;
				receivedCl.addAll(ag.getAllTopClauses());
			}
			for(Clause cl:sentCl)
				receivedCl.add(new Clause(ag.getEnv(), cl));	//this is necessary because of the vartable
			//receivedCl.addAll(sentCl);	
			waitingFor.remove(sender);
			if(Thread.currentThread().isInterrupted())
				return;
			
			if (waitingFor.isEmpty()){
				//Computing new consequence
				try {
					newCsq=ag.computeNewCons(receivedCl);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} // TODO: check that correct pfield is used
 catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(Thread.currentThread().isInterrupted())
					return;
				ag.updateListNewCons(newCsq);
				//Send relevant new consequences to other agents
				for (CanalComm target:cAg.getNeighbours()){
					Collection<Clause> clBatch=new ArrayList<Clause>();
					for (Clause cl:newCsq){
						if (ag.canResolve(target,cl))
							clBatch.add(cl);
					}
					if (!clBatch.isEmpty() || sendAll){
						cAg.send(new CFMessage(CFMessageTypes.PBM_SEND_CLAUSES,clBatch), target);
						waitingFor.add(target);
					}
					if(Thread.currentThread().isInterrupted())
						return;
				}
				//Ending condition
				if (waitingFor.isEmpty()){
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

		protected void receiveClAsLeaf(Collection<Clause> sentCl){
			if(Thread.currentThread().isInterrupted())
				return;
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
			
			//Computing new consequence
			try {
				newCsq=ag.computeNewCons(newCl);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(Thread.currentThread().isInterrupted())
				return;
			ag.updateListNewCons(newCsq);
			//Send all new consequences to root
			cAg.send(new CFMessage(CFMessageTypes.PBM_SEND_CLAUSES,newCsq), root);
		}

		
		protected boolean firstRun=true;
		protected PBICFAgent ag;
		protected CanalComm root,output;
		protected boolean isRoot;
		
		protected HashSet<CanalComm> waitingFor=new HashSet<CanalComm>();
		protected Collection<Clause> receivedCl=new ArrayList<Clause>();
		
		public static boolean verbose = true;

		@Override
		public void send(Message<?> m) {
			// TODO Auto-generated method stub
			
		}

		
}

