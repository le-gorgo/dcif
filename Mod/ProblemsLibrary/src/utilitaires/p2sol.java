/**
 * 
 */
package utilitaires;

import org.nabelab.solar.Env;
import org.nabelab.solar.Options;
import org.nabelab.solar.parser.ParseException;

import solarInterface.SolProblem;
import tptp.TPTPProblem;

/**
 * @author Gauvain Bourgne
 *
 */
public class p2sol {

	
	public static void printHelp(){
		System.out.println("Usage :");
		System.out.println("    p2sol [-method=XXX] filename.p [filename2.sol]");
		System.out.println("Options");
		System.out.println("-replace  replace the produced .sol if it already exists.");	
		System.out.println("-method=XXX  gives the name of the conversion method to be used.");		
		System.out.println("             supported methods are : ");		
		System.out.println("                 ALL_FULLCARC : (default) considers all clauses as axioms " +
				"and use the whole vocabulary as production field");
		System.out.println("                 ALL_REFUT : considers all clauses as axioms " +
				"and tries to find contradiction");
		System.out.println("                 NCONJ_CARC : considers axioms and hypotheses as axioms " +
				"and use the vocabulary of conjecture to make the production field");
		System.out.println("                 TCONJ_FULLNEWC : considers axioms and hypotheses as axioms, " +
				"negated_conjecture as top-clause, and use the whole vocabulary as pf");
		System.out.println("                 TCONJ_REFUT : considers axioms and hypotheses as axioms, " +
				"negated_conjecture as top-clause, and tries to find contradiction in the Newcarc");
		System.out.println("                 THYP_FULLNEWC : considers axioms and negated_conjecture as axioms, " +
				"hypothesis as top-clauses, and use the whole vocabulary as pf");
		System.out.println("                 THYP_REFUT : considers axioms and negated_conjecture as axioms, " +
				"hypothesis as top-clauses, and tries to find contradiction in the Newcarc");
		System.out.println("                 TCH_FULLNEWC : considers only axioms as axioms, " +
				"hypothesis and negated_conjecture as top-clause, and use the whole vocabulary as pf");
		System.out.println("                 TCH_REFUT : considers only axioms as axioms, " +
				"hypothesis and negated_conjecture as top-clause and tries to find contradiction in the Newcarc");
		System.out.println("                 THYP_NCONJ_NEWC : considers axioms as such and " +
				"hypotheses as top-clauses, and use the vocabulary of conjecture to make the production field");
		System.out.println("                 ABDUCTION consider axioms as such and negated_conjecture as top-clauses " +
				"and use the vocabulary of negated hypothesis to make the production field");
	}
	
	public static void exec(final String method, final String pbFilename, final String outputFilename, final boolean replace) throws ParseException{
		String problemFilename=pbFilename.trim();
		if (problemFilename.endsWith(".p"))
			problemFilename=problemFilename.substring(0,problemFilename.length()-2);
		String output;
		if (outputFilename==null | outputFilename.length()==0)
			output=problemFilename;
		output=outputFilename.trim();
		if (output.endsWith(".sol"))
			output=output.substring(0,output.length()-4);
		// load problem.p and convert it to sol problem
		TPTPProblem sourcePb=new TPTPProblem(problemFilename);
		Env env = new Env();
		SolProblem outputPb=sourcePb.convertToSolProblem(env, new Options(env),method);
		// save output 
		try {
			outputPb.save(output,replace);
		} catch (Exception e) {
			System.err.println("Error while saving "+outputFilename+".sol, " +
					"file already exists and option -replace not used.");
			//e.printStackTrace();
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int i=0;
		String method="ALL_FULLCARC";
		boolean replace=false;
		while (args[i].startsWith("-")) {
			if (args[i].startsWith("-method=")){
				method=args[i].substring(args[i].indexOf("=")+1).trim();
				i++;
				continue;
			}
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
		try {
			exec(method,problemFilename, outputFilename, replace);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
