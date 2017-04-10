package utilitaires;

import problemDistribution.MetisInterpreter;
import solarInterface.SolProblem;

public class graph2dcf {

	public static void printHelp(){
		System.out.println("Usage :");
		System.out.println("    graph2dcf filename.sol filename2.gra.part.N [output.dcf]");
		System.out.println("'filename.sol'  is the file of the program whose graph should be generated.");
		System.out.println("'output.dcf'  is the output file, by default, same name as filename2 with .part.N.dcf extension.");
				
		
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
		boolean replace=true;
			// Options
		while (args[i].startsWith("-")) { i++; }
		if (args.length<i+2){
			printHelp();
			return;
		}
		String problemFilename=args[i].trim();
		if (problemFilename.endsWith(".sol"))
			problemFilename=problemFilename.substring(0,problemFilename.length()-4);
		String graphFilename=args[i+1].trim();
			
		String outputFilename=graphFilename.substring(0, graphFilename.indexOf(".gra"))+
					graphFilename.substring(graphFilename.indexOf(".part"));
		if (args.length>i+2)
			outputFilename=args[i+2].trim();
		if (outputFilename.endsWith(".dcf"))
			outputFilename=outputFilename.substring(0,outputFilename.length()-4);
		
		// MAIN
		try {
			// load problem.sol and convert it to dcf problem
			MetisInterpreter metis=new MetisInterpreter(problemFilename,graphFilename);
			System.out.println("File loaded, Now saving...");
			metis.getOutput().save(outputFilename, replace);
			
		} catch (Exception e) {
			System.err.println("Error.");
			//e.printStackTrace();
		}

	}

}
