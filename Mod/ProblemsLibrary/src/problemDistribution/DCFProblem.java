/**
 * 
 */
package problemDistribution;

import genLib.io.LoaderTool;
import genLib.io.Parser;
import genLib.io.Saver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.nabelab.solar.Clause;
import org.nabelab.solar.ClauseTypes;
import org.nabelab.solar.Env;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Options;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.pfield.PField;

import logicLanguage.CNF;
import logicLanguage.IndepClause;
import logicLanguage.IndepLiteral;
import solarInterface.IndepPField;
import solarInterface.SolProblem;
import genLib.tools.Arguments;
import genLib.tools.Pair;
import answerSetProgramming.ASPCaller;
import answerSetProgramming.AnswerSet;

/**
 * @author Gauvain Bourgne
 *
 * This class is used to distribute a sol problem
 */
public class DCFProblem implements Parser,Saver, DistributedConsequenceFindingProblem<SolProblem>{
	
	public static final int ASPCOD_DLV_NAIVE=0;
	public static final int ASPCOD_CLASP_CUT=1;
	
	
	public DCFProblem(){
		super();
	}
	
	public DCFProblem(String filenameNoExt) throws Exception{
		super();
		load(filenameNoExt);
	}
	public void generateFrom(SolProblem source, String distribProgASP, int distribType, int nbAg, double minProportion,
			double maxProportion) throws IOException, ParseException{
		//Given a SolProblem
		Pair<Integer,Integer> minMax=convertProportion(source, nbAg, minProportion, maxProportion);
		int minCl=minMax.getLeft();
		int maxCl=minMax.getRight();
		//Create temporary ASP file and generate full program
		int maxComm=1; 
		boolean sat;
		List<AnswerSet> models;
		do{
			System.out.println("Attempting to find solutions with maxComm="+maxComm);
			File f=makeDistribProgram(source.NumberedCopy(), distribProgASP, distribType,
					nbAg,nbAg,maxComm, maxCl, minCl);
			// call ASP solver
			int nbAS=0;
			if (distribType==ASPCaller.ASP_DLV) nbAS=1;
			models=ASPCaller.solve(f, aspSolverUsed(distribType), "hasClause", "", nbAS, true);
			// get ASP output
			sat=!models.isEmpty();
			maxComm++;
		} while (!sat);
		// initialise local problems
		initializeWithSource(source, nbAg);
		// translate output back to DCFP (using names)
		List<String> agentNames=new ArrayList<String>();
		for (Literal hasClause:models.get(0).getLiterals()){
			interpretHasClause(hasClause,source,agentNames);
		}
	}

	public Pair<Integer,Integer> convertProportion(SolProblem source, int nbAg, 
				double minProportion, double maxProportion){
		int nbClauses=source.getNumClauses();
		int avgClausesByAgent=nbClauses/nbAg;
		int minCl=(int) Math.round(minProportion*nbClauses);
		if (minCl>avgClausesByAgent) minCl=avgClausesByAgent;
		int maxCl=(int)Math.round(maxProportion*nbClauses);
		if (maxCl<avgClausesByAgent+1) maxCl=avgClausesByAgent+1;
		return new Pair<Integer, Integer>(minCl,maxCl);
	}
	
	public void initializeWithSource(SolProblem source, int nbAg){
		for (int i=0;i<nbAg;i++){
			SolProblem pb=new SolProblem(source, false);
			localProblems.add(pb);	
		}
		pf=source.getPField();
	}
	public void interpretHasClause(Literal hasClauseLit, SolProblem source, List<String>agentNames){
		if (!IndepLiteral.getPredicate(source.getEnv(), hasClauseLit).equals("hasClause"))
			return;
		String agName=IndepLiteral.getArguments(hasClauseLit).get(0).trim();
		int agent=agentNames.indexOf(agName);
		if (agent<0){
			agent=agentNames.size();
			agentNames.add(agName);
		}
		int ind=Integer.parseInt(IndepLiteral.getArguments(hasClauseLit).get(1).trim());
		localProblems.get(agent).addClause(source.getClause(ind));
	}
	
	public int aspSolverUsed(int distribType){
		switch(distribType){
		case ASPCOD_DLV_NAIVE: return ASPCaller.ASP_DLV;
		case ASPCOD_CLASP_CUT: return ASPCaller.ASP_CLASP;
		}
		return ASPCaller.ASP_CLASP;
	}
	
	public File makeDistribProgram(SolProblem source, String distribProgASP, int distribType,
			int nbMinAg, int maxAg, int maxComm, int maxCl, int minCl) throws IOException{
		//create file
		File f=File.createTempFile("distribASP_", ".tmp");
		PrintStream fileOut = new PrintStream(new FileOutputStream(f));
		switch (distribType){
		case ASPCOD_DLV_NAIVE:
			// get source asp form
			source.generateDlvAspFacts(fileOut);
			// set constants
			setConstants(fileOut, nbMinAg, maxComm, maxCl, minCl, source.getNumClauses());
			// combine with asp distributor program			
		case ASPCOD_CLASP_CUT:
			// get source asp form
			try {
				source.generateConnectionClaspFacts(fileOut);
			} catch (ParseException e1) {
				throw new IOException(e1);
			}
			// set constants
			setConstants(fileOut, maxAg, nbMinAg, maxComm, maxCl, minCl, source.getNumClauses());
			// combine with asp distributor program
			
		}
		try {
			LoaderTool.loadInOutput(distribProgASP, ".asp", fileOut);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//close file
		fileOut.close();		
		return f;
	}
	
	public void setConstants(PrintStream p, int nbAg, int maxComm, int maxCl, int minCl, int nbClauses){
		int maxEcart=maxCl-minCl;
		int maxTotComm=maxComm*(nbAg*(nbAg-1))/2;
		int maxInt=Math.max(Math.max(Math.max(nbClauses, maxCl), maxTotComm),nbAg);
		p.println("#maxint="+maxInt+".");
		p.println("#const nbAg="+nbAg+".");
		p.println("#const minCl="+minCl+".");
		p.println("#const maxCl="+maxCl+".");
		p.println("#const maxComm="+maxComm+".");
		p.println("#const maxEcart="+maxEcart+".");
		p.println("#const maxTotComm="+maxTotComm+".");
	}
		
	
	public void setConstants(PrintStream p, int minAg, int maxAg, int maxDiff, int maxCl, int minCl, int nbClauses){
		p.println("#const minAgent="+minAg+".");
		p.println("#const maxAgent="+maxAg+".");
		p.println("#const minCard="+minCl+".");
		p.println("#const maxCard="+maxCl+".");
		p.println("#const maxEcart="+maxDiff+".");
		//p.println("#const maxTotComm="+maxTotComm+".");
	}
	

	
	public void save(String filename, boolean replace) throws Exception{
		LoaderTool.save(filename, ".dcf", this, replace);
	}
	
	public void save(PrintStream p){
		//SolProblem sp=localProblems.get(0);
		// TODO sp.printHeader(p) ??
//		p.println("%"+sp.ge);
//		p.println("%Problem: "+problem);
//		p.println("%Status: "+status);
//		p.println("%Type: "+type);
//		p.println();
		String clause;
		for (int ag=0;ag<localProblems.size();ag++){
			SolProblem pb=localProblems.get(ag);
			p.println();
			p.println("agent(ag"+ag+").");
			for (int i=0;i<pb.getNumClauses();i++){
				if (pb.isTopClause(i))
					clause=IndepClause.toSolFileLine(pb.getClause(i), "top_clause");
				else
					clause=IndepClause.toSolFileLine(pb.getClause(i), "axiom");
				// new Format : position determines partition, though it an still be explicitly indicated for retrocompatibility
				//clause=clause.substring(0,clause.lastIndexOf(")."))+", ag"+ag+").";
				p.println(clause);
			}
		}
		//for now, the production field is supposed common.
		p.println();
		//p.println(IndepPField.toSolFileLine(pf));

	}
	
	//load .sol
	public void load(String filename) throws Exception{
		LoaderTool.load(filename, ".dcf", this);
		sharePf();
	 }
	
	public void parse(BufferedReader bIn) throws IOException{
		String line=LoaderTool.getNextLine(bIn, '%');
		//TODO : le Header TPTP ?
		env = new Env();
		opt = new Options(env);
		try{
			while (line!=null){
				if (line.startsWith("agent"))
					parseAgent(line);
				else if (line.startsWith("cnf"))
					parseCnf(line);
				else if (line.startsWith("pf"))
					pf = PField.parse(env, opt, line);
				line=LoaderTool.getNextLine(bIn, '%');
			}
		}catch (ParseException e){
			throw new IOException(e);
		}
	}

	private void parseAgent(String line){
		String ag=line.substring("agent(".length(),line.lastIndexOf(")."));
		if (!agents.contains(ag)){
			agents.add(ag);
			Env newenv = new Env(env.getSymTable(), env.getDebug());
			localProblems.add(new SolProblem(newenv, new Options(newenv)));
		}	
		lastParsedAgent=ag;
	}
	
	private void parseCnf(String line) throws ParseException{
		String agent, name, role, formula;
		//extract arguments
		String temp=line.substring(line.indexOf("(")+1, line.lastIndexOf(")."));
		Arguments arg=Arguments.parse("["+temp+"]");
		//attribute arguments to correct string
		name=arg.get(0).trim();
		role=arg.get(1).trim();
		formula=arg.get(2).trim();
		if (arg.size()>3)
			agent=arg.get(3).trim();
		else
			agent=lastParsedAgent;
		SolProblem pb = getProblem(agent);
		int type;
		if(role.equals("top_clause"))
			type = ClauseTypes.TOP_CLAUSE;
		else
			type = ClauseTypes.AXIOM;
		Clause clause = Clause.parse(pb.getEnv(), pb.getOptions(), name, type, formula);
		clause.setType(type);	//for some reason, Clause.parse doesn't set the right type for the clause
	//	if (pb==null) throw new IOException("Declare an agent before using it - agent "+agent+"in cnf "+line);
		pb.addClause(clause);
	}

	private void sharePf(){
		for (SolProblem pb:localProblems){
			pb.setPField(IndepPField.copyPField(pb.getEnv(), pb.getOptions(), pf));
		}
	}
	
	public int getIndex(String ag){
		return agents.indexOf(ag);
	}
	public List<String> getAgents(){
		return agents;
	}
	public SolProblem getProblem(String ag){
		if (getIndex(ag)>=0)
			return localProblems.get(getIndex(ag));
		return null;
	}
	
	public SolProblem getProblem(int index){
		if (index>0 && index<localProblems.size())
			return localProblems.get(index);
		return null;
	}
	
	public int getNbAgents() {
		return agents.size();
	}

	public List<SolProblem> getDistTheory(){
		List<SolProblem> res=new ArrayList<SolProblem>();
		res.addAll(localProblems);
		return res;
	}

	public PField getGbPField() {
		return pf;
	}
	
	public void setMaxLength(int maxLength) {
		pf.setMaxLength(maxLength);
		for(SolProblem pb:localProblems)
			pb.getPField().setMaxLength(maxLength);
	}
	public void setGbPField(PField pf) {
		this.pf = pf;
		sharePf();
	}

	public int getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth=maxDepth;
		for (SolProblem pb:localProblems)
			pb.setDepthLimit(maxDepth);
	}
	
	public Env getEnv(){
		return env;
	}

	public Options getOptions(){
		return opt;
	}
	
	private String lastParsedAgent="";
	public List<SolProblem> localProblems=new ArrayList<SolProblem>();
	public List<String> agents=new ArrayList<String>();
	public PField pf;
	public int maxDepth;
	private Env env;
	private Options opt;

	public static void main(String[] args){
		String filename=args[0];
		if (filename.endsWith(".dcf"))
			filename=filename.substring(0,filename.length()-4);
		DCFProblem dcfp;
		try {
			dcfp = new DCFProblem(filename);
			dcfp.save(filename, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
}
