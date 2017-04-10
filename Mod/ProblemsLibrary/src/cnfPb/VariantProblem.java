package cnfPb;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.nabelab.solar.Env;
import org.nabelab.solar.Options;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.pfield.PFCardConstraint;
import org.nabelab.solar.pfield.PField;

import genLib.io.LoaderTool;
import genLib.io.Parser;
import genLib.io.Saver;
import solarInterface.IndepPField;
import solarInterface.SolProblem;

public class VariantProblem implements Saver, Parser{

	public VariantProblem(Env env, Options opt, String solFilename,PField varPf, int depthLim, String name){
		varName=name;
		this.solFilename=solFilename;
		variantPField=varPf;
		depthLimit=depthLim;
		this.env = env;
		this.opt = opt;
	}
	
	public VariantProblem(Env env, Options opt, String variantFilenameNoExt) throws Exception{
		super();
		this.env = env;
		this.opt = opt;
		load(variantFilenameNoExt);
	}
	
	public void load(String filename) throws Exception{
		LoaderTool.load(filename, ".var", this);
		//Modified by BR the 7/07/15
		initialPb=new SolProblem(env, opt, solFilename);
		/**
		File accesFichier = new File(filename) ;
		Path p = Paths.get(accesFichier.getAbsolutePath());
		Path folder = p.getParent();
		initialPb=new SolProblem(env, opt, folder.toString()+"/"+solFilename);
		**/
	 }	
	
	public void parse(BufferedReader bIn) throws IOException {
		String line=LoaderTool.getNextLine(bIn, '%');
		//Env env = new Env(); 
		while (line!=null){
			try {
				parseFileLine(line);
			} catch (ParseException e) {
				throw new IOException(e);
			}
			line=LoaderTool.getNextLine(bIn, '%');
		}
	}

	public void parseFileLine(String line) throws ParseException{
		if (line.startsWith("solSource"))
			parseSolSource(line);
		else if (line.startsWith("pf"))
			variantPField=PField.parse(env, opt, line);
		else if (line.startsWith("gpf")){
			line = line.substring(line.indexOf("f")+1).trim();
			variantPField.addConstraint( PFCardConstraint.parse(env, opt, line));
		}
		else if (line.startsWith("depthLimit"))
			parseDepth(line);
	}

	public void parseSolSource(String line){
		solFilename=line.substring("solSource(".length(),line.lastIndexOf(")."));
	}
	
	public void parseDepth(String line){
		depthLimit=Integer.parseInt(line.substring("depthLimit(".length(),line.lastIndexOf(").")));
	}
	
	public void save(String filename, boolean replace) throws Exception{
		LoaderTool.save(filename, ".var", this, replace);
	}
	
	public void save(PrintStream p) {
		printHeader(p);
		p.println("solSource("+solFilename+").");
		p.println(IndepPField.toSolFileLine(variantPField));
		p.println("depthLimit("+depthLimit+").");
	}	
	
	public void printHeader(PrintStream p){
		p.println("%Sol source: "+solFilename);
		p.println("%Variant name: "+varName);
		p.println();		

	}
	
	public PField getVariantPField() {
		return variantPField;
	}
	
	private Env env = null;
	private Options opt = null;

	public SolProblem initialPb;
	public String solFilename;
	public String varName;
	public PField variantPField;

	public int depthLimit;
}
