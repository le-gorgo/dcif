package utilitaires;

import org.nabelab.solar.Env;
import org.nabelab.solar.Options;
import org.nabelab.solar.pfield.PField;

import cnfPb.CnfReader;
import cnfPb.PFieldGenerator;
import solarInterface.IndepPField;
import solarInterface.SolProblem;

public class cnfsat2sol {

	public static void printHelp(){
		System.out.println("This tools convert a cnf sat problem into a solar consequence finding problem (pfield is set to get all consequences)");
		System.out.println("Usage :");
		System.out.println("    cnfsat2sol filename.cnf [filename2.sol]");
		System.out.println("Options");
		System.out.println("-replace  replace the produced .sol if it already exists.");	
	}
	
	public static void exec(final String pbFilename, final String outputFilename, final boolean replace){
		String problemFilename=pbFilename.trim();
		if (problemFilename.endsWith(".cnf"))
			problemFilename=problemFilename.substring(0,problemFilename.length()-4);
		String output;
		if (outputFilename==null || outputFilename.length()==0)
			output=problemFilename;
		else
			output=outputFilename.trim();
		if (output.endsWith(".sol"))
			output=output.substring(0,output.length()-4);
		// load CNF and save output 
		try {
			CnfReader source=new CnfReader(problemFilename);
			SolProblem outputPb=new SolProblem();
			outputPb.addAxioms(source.getProblem());
			PFieldGenerator pf= new PFieldGenerator(outputPb);
			PField p = pf.setGlobalPField();
			outputPb.setPField(p);
			outputPb.save(output,replace);
		} catch (Exception e) {
			System.err.println("Error.");
			//e.printStackTrace();
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int i=0;
		boolean replace=false;
		while (args[i].startsWith("-")) {
			if (args[i].trim().equals("-replace")){
				replace=true;
				i++;
				continue;
			}
			else{
				printHelp();
				return;
			}
		}
		
		String problemFilename=args[i].trim();
		String outputFilename=null;
		if (args.length>i+1)
			outputFilename=args[i+1].trim();
		exec(problemFilename, outputFilename, replace);
	}


}
