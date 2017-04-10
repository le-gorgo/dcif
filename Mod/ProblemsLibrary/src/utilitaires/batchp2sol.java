package utilitaires;

import java.util.ArrayList;
import java.util.List;

import org.nabelab.solar.Env;
import org.nabelab.solar.Options;

import solarInterface.SolProblem;
import tptp.TPTPProblem;

public class batchp2sol {

	
	public static void printHelp(){
		System.out.println("Usage :");
		System.out.println("    batchp2sol [Options] filename.p");
		System.out.println("Options");
		System.out.println("-replace  replace the produced .sol if it already exists.");	
	}
	
	private static List<String> getCompatibleMethods(TPTPProblem sourcePb){
		List<String> methods=new ArrayList<String>();
		methods.add(TPTPProblem.MET_ALL_REFUT);
		boolean sat=sourcePb.sat();
		if (sat)
			methods.add(TPTPProblem.MET_ALL_FULLCARC);
		if (sourcePb.hasNegConj() && (sourcePb.hasAxioms()||sourcePb.hasHypothesis())){
			methods.add(TPTPProblem.MET_TCONJ_REFUT);
			methods.add(TPTPProblem.MET_NCONJ_CARC);
			if (sat)
				methods.add(TPTPProblem.MET_TCONJ_FULLNEWC);
		}
		if (sourcePb.hasHypothesis() && (sourcePb.hasAxioms() || sourcePb.hasNegConj())){
			methods.add(TPTPProblem.MET_THYP_REFUT);
			if (sat)
				methods.add(TPTPProblem.MET_THYP_FULLNEWC);
		}
		if (sourcePb.hasAxioms() && sourcePb.hasHypothesis() && sourcePb.hasNegConj()){
			methods.add(TPTPProblem.MET_TCH_REFUT);
			methods.add(TPTPProblem.MET_THYP_NCONJ_NEWC);
			methods.add(TPTPProblem.MET_ABDUCTION);
			if (sat)
				methods.add(TPTPProblem.MET_TCH_FULLNEWC);
		}
		return methods;
	}
	
	public static void exec(String problemFilename,boolean replace){
		String filename=problemFilename.trim();
		if (filename.endsWith(".p"))
			filename=filename.substring(0,filename.length()-2);
		
		TPTPProblem sourcePb=new TPTPProblem(filename);
		List<String> methods=getCompatibleMethods(sourcePb);
		
		try {
			for (String method:methods){
				System.out.print("Converting into "+filename+" with method "+method+"...");
				Env env = new Env();
				SolProblem outputPb=sourcePb.convertToSolProblem(env, new Options(env),method);
				System.out.print("writing "+filename+"."+method+".sol ...");
				outputPb.save(filename+"."+method,replace);
				System.out.println("Done.");
			}
		} catch (Exception e) {
			System.err.println("Error during saving... " +
					"file already exists and option -replace not used.");
			//e.printStackTrace();
		}		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int i=0;
		boolean replace=false;
		while (i<args.length && args[i].startsWith("-")) {
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
		if (args.length<=i){
			printHelp();
			return;			
		}
		String problemFilename=args[i].trim();
		
		exec(problemFilename,replace);
		//TODO : analysis of abduction : choose a unit hypothesis ?
	}

}
