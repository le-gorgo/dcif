package distNewCarc.partition;

import java.util.Collection;

import org.nabelab.solar.Clause;
import org.nabelab.solar.Env;
import org.nabelab.solar.Options;
import org.nabelab.solar.parser.ParseException;

import agLib.agentCommunicationSystem.Agent;
import agLib.agentCommunicationSystem.CanalComm;
import logicLanguage.IndepClause;

public interface PBICFAgent extends Agent{
	public Collection<Clause> getAllTopClauses();
	public Collection<Clause> computeNewCons(Collection<Clause> newCl) throws ParseException, Exception;
	public boolean canResolve(CanalComm target, Clause cl);
	public boolean isPossibleOutput(Clause cl);
	public void updateListNewCons(Collection<Clause> newCl);
	public Collection<Clause> getListConseq();
	public Env getEnv();
	public Options getOptions();
	public void send(Collection<Clause> cl);
}
