package utilitaires;

import problemDistribution.DCFProblem;
import solarInterface.SolProblem;
import answerSetProgramming.ASPCaller;

public class buildGraph {

	public static void printHelp(){
		System.out.println("Usage :");
		System.out.println("    buildGraph filename.sol [filename2.gra]");
		System.out.println("'filename.sol'  is the file of the program whose graph should be generated.");
		System.out.println("'filename2.gra'  is the output file, by default,same name as filename with .gra extension.");
				
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Interpret command line
		if (args.length<=0){
			printHelp();
			return;
		}
		int i=0;
		boolean replace=false;
			// Options
		while (args[i].startsWith("-")) { i++; }
		String problemFilename=args[i].trim();
		if (problemFilename.endsWith(".sol"))
			problemFilename=problemFilename.substring(0,problemFilename.length()-4);
		String outputFilename=problemFilename;
		if (args.length>i+1)
			outputFilename=args[i+1].trim();
		if (outputFilename.endsWith(".gra"))
			outputFilename=outputFilename.substring(0,outputFilename.length()-4);
		
		// MAIN
		try {
			// load problem.sol and convert it to dcf problem
			SolProblem input=new SolProblem(problemFilename);
			System.out.println("File loaded, Now making graph...");
			input.saveConnectionGraph(outputFilename,replace);
		} catch (Exception e) {
			System.err.println("Error.");
			//e.printStackTrace();
		}

	}

}
