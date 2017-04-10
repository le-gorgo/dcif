package stats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;

import org.nabelab.solar.Clause;
import org.nabelab.solar.ClauseTypes;
import org.nabelab.solar.Env;
import org.nabelab.solar.Options;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.pfield.PField;

import solarInterface.IndepPField;
import logicLanguage.IndepClause;
import logicLanguage.PbFormula;
import genLib.io.LoaderTool;
import genLib.io.Parser;
import genLib.io.Saver;

public class CsqHolder implements Parser, Saver {
	//problem;distrib;nbAg+;method
	public CsqHolder (){
		super();
		this.env = new Env();
		this.opt = new Options(this.env);
	}
	
	public CsqHolder (String filenameNoExt) throws Exception{
		this();		
		load(filenameNoExt);
	}
	public CsqHolder (Collection<Clause> clauses, boolean timeOut) throws Exception{
		this();
		this.clauses=clauses;
		this.timeOut = timeOut;
	}
	
	//load .sol
	public void load(String filename) throws Exception{
		LoaderTool.load(filename, ".csq", this);
	 }
	public void setStats(String s){
		this.stats = s;
	}
	//@Override
	public void parse(BufferedReader bIn) throws IOException{
		String line=LoaderTool.getNextLine(bIn, '%');
		while (line!=null){
			try {
				parseSolFileLine(line);
			} catch (ParseException e) {
				throw new IOException(e);
			}
			line=LoaderTool.getNextLine(bIn, '%');
		}
	}
	
    public void setPField(PField pfield) {
        this.pfield = pfield;
    
    }
	public void parseSolFileLine(String line) throws ParseException{
		if (line.startsWith("nb csq"))
			parseNbCsq(line);
		if (line.startsWith("timeout"))
			parseTimeOut(line);
		if (line.startsWith("cnf"))
			parseCnf(line);
		if (line.startsWith("pf"))
			setPField(PField.parse(env, opt, line));
		if (line.equalsIgnoreCase(""))
			return;
		if (line.equals("\n"))
			return;
		else
			stats += line + "\n";
	}
	/*
	 * all csq are axioms here
	 */
	private void parseTimeOut(String line) {
		// TODO Auto-generated method stub
		String s = line.substring(8);
		int t = Integer.parseInt(s);
		if (t == 1)
			this.timeOut = true;
		else
			this.timeOut = false;			
	}

	private void parseNbCsq(String line) {
		//do nothing		
	}

	private void parseCnf(String line) throws ParseException{
		PbFormula temp=PbFormula.parseFormulaBlock(line,"SOLAR");
		Clause clause=temp.toClause(env);
		int type = ClauseTypes.AXIOM;		
		clause.setType(type);
		addClause(clause);
	}
	public void addClause(Clause clause) {
	    clauses.add(clause);
	  }

	public void save(String filename, boolean replace) throws Exception{
		System.out.println(filename);
		LoaderTool.save(filename, ".csq", this, replace);
	}
	
	//@Override
	public void save(PrintStream p){
		printHeader(p);
		for(Clause cl:getClauses()){
			if (cl != null)
				p.println(IndepClause.toSolFileLine(cl, "axiom"));
		}
		printFooter(p);
	//	p.println();
		//p.println(IndepPField.toSolFileLine(getPField()));
	}
	
	//print .csv informations to be centralised after (can't guarantee that mono has been computed)
	private void printFooter(PrintStream p) {
		p.println(stats);
		
	}

	private void printHeader(PrintStream p) {
		String t;

		if (this.timeOut == true)
			t = "1" ;
		else 
			t = "0";
		p.println("timeout "+t);

		p.println("nb csq "+Integer.toString(this.getClauses().size()));
	}

	private Collection<Clause> clauses = new ArrayList<Clause>();
	private PField pfield = null;
	private Env env;
	private Options opt;
	private boolean timeOut;
	String stats;
	public Collection<Clause> getClauses(){
		return this.clauses;
	}
	public PField getPField(){
		return this.pfield;
	}
}
