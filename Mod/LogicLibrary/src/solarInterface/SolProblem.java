/**
 * 
 */
package solarInterface;

import genLib.io.LoaderTool;
import genLib.io.Parser;
import genLib.io.Saver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nabelab.solar.CFP;
import org.nabelab.solar.Clause;
import org.nabelab.solar.ClauseTypes;
import org.nabelab.solar.Env;
import org.nabelab.solar.Options;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.pfield.PField;

import logicLanguage.CNF;
import logicLanguage.ClauseConnectionNetwork;
import logicLanguage.IndepClause;
import logicLanguage.PbFormula;

/**
 * @author Gauvain Bourgne
 *
 * This class describe a solar problem. It can be used to load or generate files .sol
 * It is the output of sol conversion of TPTPProblem, and also the input of distribution generators
 * (class DCFProblem)
 */
public class SolProblem extends CFP implements Parser, Saver{

	public SolProblem() {
		this(new Env());
	}
	
	public SolProblem(Env env) {
		this(env, new Options(env));
	}
	
	public SolProblem(Env env, Options opt) {
		super(env, opt);
		opt.setUsedClausesOp(true);
		this.env = env;
		this.opt = opt;
		this.name = "unnamed";
		this.problem = "";
		this.status = "";
		this.type = "";
	}

	public SolProblem(String name, String problem, String status, String type, PField pf) {
		this(new Env(), name, problem, status, type, pf);
	}
	
	public SolProblem(Env env, String name, String problem, String status, String type, PField pf) {
		this(env, new Options(env), name, problem, status, type, pf);
	}
	
	public SolProblem(Env env, Options opt, String name, String problem, String status, String type, PField pf) {
		super(env, opt);
		opt.setUsedClausesOp(true);
		this.env = env;
		this.opt = opt;
		this.name = name;
		this.setPField(pf);
		this.problem = problem;
		this.status = status;
		this.type = type;
	}

	public SolProblem(SolProblem pb, boolean includeClauses) {
		super(pb.getEnv(), pb.getOptions());
		this.env = pb.getEnv();
		this.opt = pb.getOptions();
		this.opt.setUsedClausesOp(true);
		this.name = pb.name;
		this.setPField(pb.getPField());
		this.problem = pb.problem;
		this.status = pb.status;
		this.type = pb.type;
		if (includeClauses){
			for(Clause cl:pb.getClauses())
				addClause(cl);
		}
	}
	
	public SolProblem(Collection<? extends Clause> axioms,  
	Collection<? extends Clause> top_clauses) {
		this(new Env(), axioms, top_clauses);
	}
		
	public SolProblem(Env env, Collection<? extends Clause> axioms,  
	Collection<? extends Clause> top_clauses) {
		this(env, new Options(env), axioms, top_clauses);
	}
		
	public SolProblem(Env env, Options opt, Collection<? extends Clause> axioms,  
	Collection<? extends Clause> top_clauses) {
		super(env, opt);
		opt.setUsedClausesOp(true);
		this.env = env;
		this.opt = opt;
		this.name = "";
		this.problem = "";
		this.status = "";
		this.type = "";
		for(Clause ax:axioms){
			ax.setType(ClauseTypes.AXIOM);
			addClause(ax);
		}
		for(Clause tc:top_clauses){
			tc.setType(ClauseTypes.AXIOM);
			addClause(tc);
		}
	}
	
	public SolProblem(Collection<? extends Clause> axioms,  
	Collection<? extends Clause> top_clauses, PField pf) {
		this(new Env(), axioms, top_clauses, pf);
	}
		
	public SolProblem(Env env, Collection<? extends Clause> axioms,  
	Collection<? extends Clause> top_clauses, PField pf) {
		this(env, new Options(env), axioms, top_clauses, pf);
	}
		
	public SolProblem(Env env, Options opt, Collection<? extends Clause> axioms,  
	Collection<? extends Clause> top_clauses, PField pf) {
		super(env, opt);
		opt.setUsedClausesOp(true);
		this.env = env;
		this.opt = opt;
		setPField(pf);
		this.name = "";
		this.problem = "";
		this.status = "";
		this.type = "";
		for(Clause ax:axioms)
			addAxiom(ax);
		for(Clause tc:top_clauses)
			addTopClause(tc);
	}
	
	public SolProblem(String name, String problem, String status, String type, 
	Collection<? extends Clause> axioms,  
	Collection<? extends Clause> top_clauses) {
		this(new Env(), name, problem, status, type, axioms, top_clauses);
	}
		
	public SolProblem(Env env, String name, String problem, String status, String type, 
	Collection<? extends Clause> axioms,  
	Collection<? extends Clause> top_clauses) {
		this(env, new Options(env), name, problem, status, type, axioms, top_clauses);
	}
		
	public SolProblem(Env env, Options opt, String name, String problem, String status, String type, 
	Collection<? extends Clause> axioms,  
	Collection<? extends Clause> top_clauses) {
		super(env, opt);
		opt.setUsedClausesOp(true);
		this.env = env;
		this.opt = opt;
		this.name = name;
		this.problem = problem;
		this.status = status;
		this.type = type;
		for(Clause ax:axioms){
			ax.setType(ClauseTypes.AXIOM);
			addClause(ax);
		}
		for(Clause tc:top_clauses){
			tc.setType(ClauseTypes.AXIOM);
			addClause(tc);
		}
	}
	
	public SolProblem(String name, String problem, String status, String type, 
						Collection<? extends Clause> axioms,  
						Collection<? extends Clause> top_clauses, 
						PField pf) {
		this(new Env(), name, problem, status, type, axioms, top_clauses, pf);
	}
	
	public SolProblem(Env env, String name, String problem, String status, String type, 
			Collection<? extends Clause> axioms,  
			Collection<? extends Clause> top_clauses, 
			PField pf) {
		this(env, new Options(env), name, problem, status, type, axioms, top_clauses, pf);
	}
	
	public SolProblem(Env env, Options opt, String name, String problem, String status, String type, 
			Collection<? extends Clause> axioms,  
			Collection<? extends Clause> top_clauses, 
			PField pf) {
		super(env, opt);
		opt.setUsedClausesOp(true);
		this.env = env;
		this.opt = opt;
		this.name = name;
		this.problem = problem;
		this.status = status;
		this.type = type;
		this.setPField(pf);
		for(Clause ax:axioms){
			ax.setType(ClauseTypes.AXIOM);
			addClause(ax);
		}
		for(Clause tc:top_clauses){
			tc.setType(ClauseTypes.TOP_CLAUSE);
			addClause(tc);
		}
	}
	
	public SolProblem(String name, String problem, String status, String type, 
			Collection<? extends Clause> clauses) {
		this(new Env(), name, problem, status, type, clauses);
	}
	
	public SolProblem(Env env,String name, String problem, String status, String type, 
			Collection<? extends Clause> clauses) {
		this(env, new Options(env), name, problem, status, type, clauses);
	}
	
	public SolProblem(Env env, Options opt, String name, String problem, String status, String type, 
			Collection<? extends Clause> clauses) {
		super(env, opt);
		opt.setUsedClausesOp(true);
		this.env = env;
		this.opt = opt;
		this.name = name;
		this.problem = problem;
		this.status = status;
		this.type = type;
		for(Clause cl:clauses)
			addClause(cl);
	}
	
	public SolProblem(String name, String problem, String status, String type, 
			Collection<? extends Clause> clauses,
			PField pf) {
		this(new Env(), name, problem, status, type, clauses, pf);
	}
	
	public SolProblem(Env env,String name, String problem, String status, String type, 
			Collection<? extends Clause> clauses,
			PField pf) {
		this(env, new Options(env), name, problem, status, type, clauses, pf);
	}
	
	public SolProblem(Env env, Options opt, String name, String problem, String status, String type, 
			Collection<? extends Clause> clauses,
			PField pf) {
		super(env, opt);
		opt.setUsedClausesOp(true);
		this.env = env;
		this.opt = opt;
		this.name = name;
		this.problem = problem;
		this.status = status;
		this.type = type;
		this.setPField(pf);
		for(Clause cl:clauses)
			addClause(cl);
	}
	
	public SolProblem(String filename) throws Exception{
		this(new Env(), filename);
	}
	
	public SolProblem(Env env, String filename) throws Exception{
		this(env, new Options(env), filename);
	}
	
	public SolProblem(Env env, Options opt, String filename) throws Exception{
		super(env, opt);
		opt.setUsedClausesOp(true);
		this.env = env;
		this.opt = opt;
		this.name = filename;
		this.problem = "";
		this.status = "";
		this.type = "";
		load(filename);
	}
	
	public SolProblem NumberedCopy(){
		int i;
		SolProblem res = new SolProblem(env, opt, name, problem, status, type, getPField());
		for (i=0;i<getNumClauses();i++){
			//if (isTopClause(i))
			res.addClause(IndepClause.rename(env, getClause(i), ""+i));
			//else
				//res.addAxiom(IndepClause.rename(env, getClause(i), ""+i));
		}
		// Note : the production field is not affected 
		return res;
	}
	
	/*
	public int getNbClauses(){
		return axioms.size()+top_clauses.size();
	}*/
	
	/*
	public IndepPField getPField(){
		return pf;
	}*/
	
	/*
	public void setPField(IndepPField pfield){
		pf=pfield;
	}*/

	public String getName(){
		return name;
	}
	
	/*
	public IndepClause getClause(int indice){
		if (indice<top_clauses.size())
			return top_clauses.get(indice);
		else if (indice<getNbClauses())
			return axioms.get(indice-top_clauses.size());
		return null;
	}*/
	
	public Clause getClause(int index) {
		return getClauses().get(index);
	}
	
	public boolean isTopClause(int indice){
		return (getClause(indice).getType() == ClauseTypes.TOP_CLAUSE);
	}
	
	/*
	//to CFP
	public void addClause(IndepClause clause, boolean topClause){
		if (topClause)
			addTopClause(clause);
		else
			addAxiom(clause);
	}*/
	
	public void addAxiom(Clause axiom){
	//	if (axioms.contains(axiom))
	//		return false;
		axiom.setType(ClauseTypes.AXIOM);
		addClause(axiom);
	//	return true;
	}
	
	public void addAxioms(CNF axiomList){
	//	boolean modif=false;
	//	for (IndepClause cl:axiomList){
	//		modif=addAxiom(cl)||modif;
	//	}
	//	return modif;
		for(Clause ax:axiomList)
			addAxiom(ax);
	}
	
	public void addTopClause(Clause topClause){
	//	if (top_clauses.contains(topClause))
	//		return false;
		topClause.setType(ClauseTypes.TOP_CLAUSE);
		addClause(topClause);
	//	return true;
	}
	public void addTopClauses(CNF clauseList){
	//	boolean modif=false;
	//	for (IndepClause cl:clauseList){
	//		modif=addTopClause(cl)||modif;
	//	}
	//	return modif;
		for(Clause tc:clauseList)
			addTopClause(tc);
	}
	
	
	/*public CNF getTopClauses(){
		CNF topclauses = new CNF();
		for(Clause cl:getClauses())
			if(cl.getType() == ClauseTypes.TOP_CLAUSE)
				topclauses.add(cl);
		return topclauses;
		//return CNF.copy();
	}*/
	
	public List<Clause> getAxioms(){
		List<Clause> axioms = new ArrayList<Clause>(getClauses());
		axioms.removeAll(getTopClauses());
		return axioms;
	}

	/*public CNF getAllClauses(){
		CNF clauses = new CNF(getClauses());
		return clauses;
	}*/

	//count nb predicates
	
	
	//load .sol
	public void load(String filename) throws Exception{
		LoaderTool.load(filename, ".sol", this);
	 }
	
	public void parse(BufferedReader bIn) throws IOException{
		String line=LoaderTool.getNextLine(bIn, '%');
		//TODO : le Header TPTP ?
		while (line!=null){
			try {
				parseSolFileLine(line);
			} catch (ParseException e) {
				throw new IOException(e);
			}
			line=LoaderTool.getNextLine(bIn, '%');
		}
	}
	
	public void parseSolFileLine(String line) throws ParseException{
		if (line.startsWith("cnf"))
			parseCnf(line);
		if (line.startsWith("pf"))
			setPField(PField.parse(env, opt, line));
	}

	private void parseCnf(String line) throws ParseException{
		PbFormula temp=PbFormula.parseFormulaBlock(line,"SOLAR");
		Clause clause=temp.toClause(env);
		int type = ClauseTypes.AXIOM;
		if(temp.getRole().equals("top_clause"))
			type = ClauseTypes.TOP_CLAUSE;
		clause.setType(type);
		addClause(clause);
	}
	
	//distribution avec ASP
	public void generateDlvAspFacts(PrintStream p){
		PbFormula f;
		for(Clause cl:getClauses()){
			if(cl.getType() == ClauseTypes.TOP_CLAUSE)
				f=new PbFormula(cl,"top_clause");
			else
				f=new PbFormula(cl,"axiom");
			p.println(f.convertTo("asp"));
		}
	}
	
	public void generateConnectionClaspFacts(PrintStream p) throws ParseException{
		CNF theory = CNF.copy((CNF)getClauses());
		ClauseConnectionNetwork network = new ClauseConnectionNetwork(env, theory);
		p.println(network.convertToAsp());
	}
	
	public void saveConnectionGraph(String filename, boolean replace) throws Exception{
		CNF theory = CNF.copy(getClauses());
		ClauseConnectionNetwork network = new ClauseConnectionNetwork(env, theory);
		LoaderTool.save(filename, ".gra", network.convertToGraph(), replace);
	}
	
	
	public void save(String filename, boolean replace) throws Exception{
		LoaderTool.save(filename, ".sol", this, replace);
	}
	
	public void save(PrintStream p){
		printHeader(p);
		for(Clause cl:getClauses()){
			if(cl.getType() == ClauseTypes.TOP_CLAUSE)
				p.println(IndepClause.toSolFileLine(cl, "top_clause"));
			else
				p.println(IndepClause.toSolFileLine(cl, "axiom"));
		}
		p.println();
		p.println(IndepPField.toSolFileLine(getPField()));
	}
	
	public void printHeader(PrintStream p){
		p.println("%"+name);
		p.println("%Problem: "+problem);
		p.println("%Status: "+status);
		p.println("%Type: "+type);
		p.println();		
	}
	
	/*
	public CFP toCFP(Env env){
		//initialize options and problem 
		Options opt=new Options(env);
		//opt.setDepthLimit(depthLimit);
		opt.setUsedClausesOp(true);
		CFP problem= new CFP(env, opt);
		for (IndepClause cl : axioms)
			problem.addClause(cl.toClause(env));
		for (IndepClause cl : top_clauses)
			problem.addClause(cl.toClause(env,Clause.TOP_CLAUSE));
		if (pf!=null)
			try {
				problem.setPField(pf.toPField(env, problem.getOptions()));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		
		return problem;

	}*/
	
	
	public int getDepthLimit() {
		return getPField().getMaxTermDepth();
		//return this.depthLimit;
	}

	public void setDepthLimit(int depthLimit) {
		getPField().setMaxTermDepth(depthLimit);
		//this.depthLimit = depthLimit;
	}

	public Env getEnv() {
		return env;
	}
		
	/**
	 * Name of the problem (also filename - file 'name'.sol)
	 */
	protected String name;
	/**
	 * description of the problem
	 */
	protected String problem;	
	/**
	 * Status of the problem (SAT or UNSAT in our cases)
	 */
	protected String status;
	/**
	 * Types of the problems (CF,CARC,NEWCF,NEWCARC,REFUT,NEWREFUT)
	 */
	protected String type;	
	/**
	 * All the clauses that should be used as top-clauses
	 */
	//protected CNF top_clauses=new CNF();
	/**
	 * All the axioms clauses
	 */
	//protected CNF axioms=new CNF();
	/**
	 * The production field
	 */
	//protected IndepPField pf=new IndepPField();
	
	//protected int depthLimit=-1;
	/**The environment*/
	private Env env;
	/**The options*/
	private Options opt;
	public CNF listClauses() {
		CNF theory = CNF.copy(getClauses());
		return theory;
	}
}
