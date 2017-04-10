package problemDistribution;

import genLib.io.LoaderTool;
import genLib.io.Parser;

import java.io.BufferedReader;
import java.io.IOException;

import logicLanguage.IndepClause;
import solarInterface.SolProblem;

public class MetisInterpreter implements Parser {

	public MetisInterpreter(String solfilename, String partfilename) throws Exception{
		load(solfilename,partfilename);
	}
	
	
	public void load(String solfilename, String partfilename) throws Exception{
		source=new SolProblem(solfilename);
		output=new DCFProblem();
		output.pf=source.getPField();
		LoaderTool.load(partfilename, "", this);
	 }

	
	public void parse(BufferedReader input) throws IOException {
		int lineNumber=0;
		String line=LoaderTool.getNextLine(input, '%');
		while (line!=null){
			parseFileLine(line, lineNumber);
			line=LoaderTool.getNextLine(input, '%');
			lineNumber++;
		}
	}
	
	protected void parseFileLine(String line, int lineNumber){
		// get clause
		IndepClause clause=source.getClause(lineNumber);
		boolean topclause=source.isTopClause(lineNumber);
		//get partition
		int partition=Integer.parseInt(line.trim());
		// add agent if necessary
		if (output.localProblems.size()<=partition){
			for (int i=output.localProblems.size();i<=partition;i++){
				output.agents.add("ag"+i);
				output.localProblems.add(new SolProblem());
			}
		}
		// add clause to correct partition
		SolProblem pb=output.localProblems.get(partition);
		pb.addClause(clause,topclause);
	}
	
	public DCFProblem getOutput() {
		return output;
	}
	
	protected SolProblem source;
	protected DCFProblem output;
		
	
	
}
